# Skins Marketplace — Backend API

TPO Aplicaciones Interactivas · UADE 1C 2026

Marketplace de skins de videojuegos desarrollado con Spring Boot. Permite a los usuarios publicar, comprar y gestionar skins de CS2 y otros juegos a través de una API REST con autenticación JWT.

---

## Stack tecnológico

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.3 |
| Spring Security | JWT stateless |
| Hibernate / JPA | 7 |
| MySQL | 8+ |
| Lombok | última |
| Maven | gestor de dependencias |

---

## Configuración y ejecución

### Requisitos
- Java 21
- MySQL 8 corriendo en localhost:3306
- Maven

### Pasos

1. Clonar el repositorio
2. Crear la base de datos (o dejar que Spring la cree automáticamente):
```sql
CREATE DATABASE skinsmarketplace;
```
3. Configurar `application.properties` si es necesario:
```properties
server.port=4003
spring.datasource.url=jdbc:mysql://localhost:3306/skinsmarketplace?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=TU_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```
4. Ejecutar:
```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:4003`

---

## Autenticación

La API usa **JWT Bearer Token**. El token se obtiene al registrarse o hacer login y debe enviarse en cada request protegido:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Registro, verificación y login

**Registrarse:**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "juangarcia",
  "firstName": "Juan",
  "lastName": "García",
  "email": "juan@mail.com",
  "password": "pass123",
  "passwordRepeat": "pass123"
}
```

El registro no devuelve JWT. Devuelve un mensaje y envía un link de verificación al mail:
`http://localhost:5173/verify-email?token=...`

**Verificar email:**
```http
POST /api/v1/auth/verify-email
Content-Type: application/json

{
  "token": "TOKEN_DEL_MAIL"
}
```

**Login:**
```http
POST /api/v1/auth/authenticate
Content-Type: application/json

{
  "email": "juan@mail.com",
  "password": "pass123"
}
```

El login devuelve `{ "access_token": "eyJ..." }` solo si el email ya fue verificado.

**Reenviar verificación:**
```http
POST /api/v1/auth/resend-verification
Content-Type: application/json

{
  "email": "juan@mail.com"
}
```

**Recuperar contraseña:**
```http
POST /api/v1/auth/forgot-password
Content-Type: application/json

{
  "email": "juan@mail.com"
}
```

Si el email está registrado, el backend genera un token de 30 minutos y arma un link:
`http://localhost:5173/reset-password?token=...`

En desarrollo el link se imprime en consola desde `EmailService`. En producción se puede conectar ese servicio a SMTP sin cambiar los endpoints.

**Cambiar contraseña estando logueado:**
```http
POST /api/v1/users/me/password-reset-email
Authorization: Bearer TOKEN
```

El backend manda el mismo link de recuperación al mail registrado del usuario autenticado.

**Confirmar nueva contraseña:**
```http
POST /api/v1/auth/reset-password
Content-Type: application/json

{
  "token": "TOKEN_DEL_MAIL",
  "password": "nueva123",
  "passwordRepeat": "nueva123"
}
```

**Para hacer un usuario ADMIN** (desde MySQL Workbench):
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'juan@mail.com';
```

---

## Endpoints

### 🔐 Auth — sin token

| Método | URL | Descripción |
|---|---|---|
| POST | `/api/v1/auth/register` | Registrar usuario |
| POST | `/api/v1/auth/verify-email` | Verificar email con token |
| POST | `/api/v1/auth/resend-verification` | Reenviar link de verificación |
| POST | `/api/v1/auth/authenticate` | Login |
| POST | `/api/v1/auth/forgot-password` | Solicitar link de recuperación |
| POST | `/api/v1/auth/reset-password` | Cambiar contraseña con token |

---

### 👤 Usuarios — token USER o ADMIN

| Método | URL | Descripción |
|---|---|---|
| GET | `/api/v1/users/me` | Ver mi perfil |
| PUT | `/api/v1/users/me` | Actualizar mi perfil |
| POST | `/api/v1/users/me/password-reset-email` | Enviar link para cambiar contraseña |

> El username solo puede cambiarse cada 15 días.
> El cambio de contraseña se hace por mail, no desde `PUT /api/v1/users/me`.

---

### 📂 Categorías

| Método | URL | Token | Descripción |
|---|---|---|---|
| GET | `/categories` | Sin token | Listar categorías |
| GET | `/categories/{id}` | Sin token | Obtener categoría por ID |
| POST | `/categories/create` | ADMIN | Crear categoría |
| PUT | `/categories/{id}` | ADMIN | Editar categoría |
| DELETE | `/categories/{id}` | ADMIN | Eliminar categoría |

---

### 🎮 Skins

| Método | URL | Token | Descripción |
|---|---|---|---|
| GET | `/skins/get/all` | Sin token | Publicaciones disponibles |
| GET | `/skins/get/{id}` | Sin token | Obtener skin por ID |
| GET | `/skins/get/category?id=1` | Sin token | Filtrar por ID de categoría |
| GET | `/skins/get/category?name=Rifle` | Sin token | Filtrar por nombre de categoría |
| GET | `/skins/get/search?name=AK` | Sin token | Buscar por nombre |
| GET | `/skins/get/price?min=10&max=100` | Sin token | Filtrar por precio |
| GET | `/skins/admin/all` | ADMIN | Listar todas (incluye inactivas) |
| POST | `/skins/admin/create` | ADMIN | Crear publicación |
| PUT | `/skins/admin/edit/{id}` | ADMIN | Editar publicación |
| PUT | `/skins/admin/inactivar/{id}` | ADMIN | Baja lógica de publicación |
| POST | `/inventario/{itemId}/publicar` | USER | Publicar skin desde inventario |
| GET | `/skins/mis-skins` | USER | Mis skins publicadas |
| PUT | `/skins/{id}` | USER | Editar mi skin |
| PUT | `/skins/{id}/inactivar` | USER | Inactivar mi skin |

> Cada publicación representa una skin única. El campo `stock` queda oculto en las respuestas y se usa solo internamente para marcar si la publicación sigue disponible. Para intercambio, el front debe mirar `intercambiable`.

---

### 🛒 Carrito — token USER

| Método | URL | Descripción |
|---|---|---|
| GET | `/carrito` | Ver mi carrito |
| PATCH | `/carrito/skins/{skinId}?cantidad=1` | Agregar skin al carrito |
| PUT | `/carrito/items/{itemId}?cantidad=1` | Mantener cantidad única |
| DELETE | `/carrito/items/{itemId}` | Eliminar item |
| DELETE | `/carrito` | Vaciar carrito |

> La cantidad siempre debe ser `1`: no existe compra por cantidad porque cada skin publicada es única.

---

### 🔁 Intercambios — token USER

| Método | URL | Descripción |
|---|---|---|
| POST | `/operations/exchange/quote` | Cotizar intercambio sin reservar |
| POST | `/operations/exchange` | Crear orden de intercambio |
| GET | `/operations/{id}/status` | Ver estado |

La cotización y la orden reciben:
```json
{
  "inventarioItemIds": [10],
  "skinIds": [25]
}
```

El backend calcula valores por precio promedio de publicaciones comparables: misma skin del catálogo, mismo desgaste (`exterior`) y mismo StatTrak. Si `diferencia > 0`, el usuario paga esa diferencia por Mercado Pago. Si `diferencia < 0`, se acredita saldo interno al usuario cuando el bot recibe sus skins.

---

### 🎟️ Cupones

| Método | URL | Token | Descripción |
|---|---|---|---|
| POST | `/cupones` | ADMIN | Crear cupón |
| GET | `/cupones` | ADMIN | Listar cupones |
| GET | `/cupones/{id}` | ADMIN | Buscar cupón por ID |
| GET | `/cupones/validar?codigo=PROMO2027` | USER o ADMIN | Validar cupón |
| DELETE | `/cupones/{id}` | ADMIN | Eliminar cupón |

---

### 📦 Órdenes — token USER

| Método | URL | Descripción |
|---|---|---|
| POST | `/order` | Crear orden con itemList |
| POST | `/order/from-carrito` | Crear orden desde el carrito |
| POST | `/order/from-carrito?codigoCupon=PROMO2027` | Crear orden desde carrito con cupón |
| GET | `/order/me` | Mi historial de órdenes |
| GET | `/order/{id}` | Obtener orden por ID |
| DELETE | `/order/{id}` | Eliminar orden |

**Body para crear orden con itemList:**
```json
{
  "itemList": [
    { "skinId": 1, "quantity": 1 }
  ],
  "codigoCupon": null
}
```

---

### 💳 Pagos Mercado Pago Bricks — token USER

Variables necesarias para levantar el backend:

```bash
export MERCADOPAGO_ACCESS_TOKEN="TEST-..."
export MERCADOPAGO_PUBLIC_KEY="TEST-..."
export MERCADOPAGO_BACKEND_URL="https://tu-url-publica"
```

En desarrollo local, `MERCADOPAGO_BACKEND_URL` debe ser una URL pública tipo ngrok para que Mercado Pago pueda llamar al webhook.

| Método | URL | Descripción |
|---|---|---|
| POST | `/payments/bricks/preferences/from-carrito` | Crea una orden desde el carrito y devuelve `preferenceId` para Payment Brick |
| POST | `/payments/bricks/preferences/orders/{orderId}` | Devuelve `preferenceId` para una orden existente |
| POST | `/payments/bricks/orders/{orderId}/process-payment` | Recibe el `formData` del Payment Brick y crea el pago |
| POST | `/payments/webhook` | Webhook público de Mercado Pago |

El frontend futuro debe enviar en `/process-payment` el `formData` recibido en `onSubmit` del Payment Brick. El backend no confía en el monto enviado por frontend: usa el total de la orden guardada.

Ejemplo de body para tarjeta:

```json
{
  "token": "CARD_TOKEN_GENERADO_POR_BRICK",
  "payment_method_id": "visa",
  "issuer_id": "310",
  "installments": 1,
  "payer": {
    "email": "comprador@test.com",
    "identification": {
      "type": "DNI",
      "number": "12345678"
    }
  }
}
```

Agregar siempre un header de idempotencia desde el frontend:

```http
X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

---

### 🛡️ Panel Admin — token ADMIN

| Método | URL | Descripción |
|---|---|---|
| GET | `/api/v1/admin/ordenes` | Listar todas las órdenes |
| GET | `/api/v1/admin/usuarios` | Listar todos los usuarios |
| PUT | `/api/v1/admin/usuarios/{id}/rol` | Cambiar rol de usuario |

**Body para cambiar rol:**
```json
{
  "nuevoRol": "ADMIN"
}
```
> Valores válidos: `USER` o `ADMIN`. El campo es `nuevoRol`, no `role`.

---

## Arquitectura

```
src/main/java/skinsmarket/demo/
│
├── controller/          # Endpoints REST (@RestController)
│   ├── auth/            # Registro y login
│   ├── admin/           # Panel de administración
│   ├── carrito/         # Gestión del carrito
│   ├── category/        # Categorías de skins
│   ├── config/          # SecurityConfig, JwtFilter
│   ├── cupon/           # Cupones de descuento
│   ├── order/           # Órdenes de compra
│   ├── skin/            # Skins del marketplace
│   └── user/            # Perfil de usuario
│
├── entity/              # Entidades JPA (@Entity)
│   ├── User, Role
│   ├── Skin, Category
│   ├── Carrito, ItemCarrito
│   ├── Order, OrderDetail
│   └── Cupon
│
├── repository/          # Acceso a BD (JpaRepository)
│
├── service/             # Lógica de negocio
│   ├── interfaces (SkinService, OrderService, etc.)
│   └── implementaciones (SkinServiceImpl, etc.)
│
├── exception/           # Excepciones personalizadas
│
└── utils/               # InfoValidator (email, password, etc.)
```

---

## Decisiones de diseño

- **Login por email** — el username se guarda pero no autentica. La autenticación es siempre por email.
- **Imágenes como BLOB** — las imágenes se almacenan en la BD como `LONGBLOB` y se devuelven en base64 (`imageBase64`). El frontend las renderiza con `data:image/jpeg;base64,...`.
- **Imagen obligatoria** — no se puede crear ni editar una skin sin imagen. Devuelve 400 si falta.
- **Baja lógica en skins** — las skins no se borran físicamente, se pone `active=false` para no romper el historial de órdenes.
- **@Transactional en writes** — todos los métodos que modifican la BD están anotados con `@Transactional`.
- **No comprar skin propia** — el servicio valida que el comprador no sea el vendedor de la skin.
- **Username cada 15 días** — se valida con `ChronoUnit.DAYS.between()` y se guarda `usernameChangedAt` en el usuario.
- **JWT stateless** — no hay sesión en el servidor. Cada request se autentica con el token en el header.

---

## Colección Insomnia

El archivo `skins_marketplace_insomnia.json` contiene todos los endpoints listos para importar en Insomnia.

**Cómo usarlo:**
1. Abrir Insomnia → File → Import → From File
2. Seleccionar `skins_marketplace_insomnia.json`
3. En el **Base Environment**, reemplazar:
   - `token_user` → token de un usuario con rol USER
   - `token_admin` → token de un usuario con rol ADMIN

---

## Equipo

Proyecto desarrollado para la materia **Aplicaciones Interactivas** — UADE 1C 2026.
