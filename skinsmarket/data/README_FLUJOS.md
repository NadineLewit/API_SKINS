# Flujos de Compra / Venta / Intercambio / Cancelación

Documentación de los 4 flujos pedidos en el enunciado, mapeado a los archivos
del proyecto.

## Arquitectura general

```
┌─────────────┐      ┌────────────────┐      ┌─────────────────┐
│  Frontend   │ ───► │  Backend Java  │ ◄──► │   MySQL (BD)    │
│  (React)    │      │  Spring Boot   │      │                 │
└─────────────┘      └────────┬───────┘      └─────────────────┘
                              │
                              │ escribe/lee
                              ▼
                     ┌────────────────┐      ┌─────────────────┐
                     │ data/orders.   │ ◄──► │   Bot Node.js   │
                     │     json       │      │  (Steam trades) │
                     └────────────────┘      └─────────────────┘
```

El backend Java es la **única fuente de verdad**. El bot solo procesa lo que
el backend le indica vía `orders.json`. Mientras Steam esté bloqueado hasta el
10/06, el `MockTradeScheduler` simula al bot internamente.

## Estados de una operación

Cada `Order` tiene 3 dimensiones de estado:

| Campo | Valores | Quién lo maneja |
|---|---|---|
| `operationType` | PURCHASE / SALE / EXCHANGE / RETURN | Backend al crearla |
| `paymentStatus` | PENDING_PAYMENT / PAID / REJECTED / ... | Mercado Pago |
| `tradeStatus` | WAITING_PAYMENT / WAITING_USER_TRADE / BOT_SENT / COMPLETED / RETURN_PENDING / ... | Bot (o mock) |

## CASO 1 — COMPRA

### Flujo
1. USER agrega al carrito → POST /order/from-carrito → orden creada
   - operationType=`PURCHASE`, tradeStatus=`WAITING_PAYMENT`, paymentStatus=`PENDING_PAYMENT`
2. USER paga con MP → POST /payments/bricks/preferences/orders/{id} y /process-payment
3. MP confirma pago (vía webhook) → paymentStatus=`PAID`
4. **PaymentServiceImpl detecta PAID** y escribe entrada en `orders.json`
5. **MockTradeScheduler** detecta PURCHASE+PAID+WAITING_PAYMENT → avanza a:
   - `PREPARING_TRADE` (5s)
   - `BOT_SENT` (5s)
   - `COMPLETED` (5s)

### Archivos involucrados
- `OrderServiceImpl.createOrder()` — marca `operationType=PURCHASE`
- `PaymentServiceImpl.updateOrderWithPayment()` — al pasar a PAID, llama a `crearBotOrderParaCompra()`
- `MockTradeScheduler.procesar()` — caso PURCHASE+PAID

## CASO 2 — VENTA

### Flujo
1. USER selecciona items de su inventario → POST /operations/sale
   - Validaciones: items propios, tradeables, no usados en otra operación
   - operationType=`SALE`, tradeStatus=`WAITING_USER_TRADE`
   - Se escribe entrada en `orders.json` con direction=`USER_TO_BOT`
2. USER manda trade offer al bot desde Steam con los assetIds esperados
3. Bot valida que los assetIds coincidan → marca `USER_TRADE_RECEIVED`
   - En mock: 10s después de creada
   - En producción: bot real lee orders.json, valida la oferta entrante, acepta
4. Sistema acredita pago → `PREPARING_TRADE` → `COMPLETED`

### Archivos involucrados
- `OperationController.createSale()`
- `TradeOperationServiceImpl.createSale()` — toda la lógica de validación
- `BotTradeOrdersFileService.upsert()` — escribe en orders.json
- `MockTradeScheduler.procesar()` — caso SALE+WAITING_USER_TRADE

## CASO 3 — INTERCAMBIO

### Flujo
1. USER selecciona N skins propias + M skins del marketplace → POST /operations/exchange
2. Sistema calcula `valor_marketplace - valor_usuario`:
   - `valor_marketplace` = sum(Skin.finalPrice)
   - `valor_usuario` = sum(precioPromedioMarketplace para esa skin)
3. tradeStatus=`WAITING_USER_TRADE`, paymentStatus depende de la diferencia
4. USER manda sus skins → bot recibe → `USER_TRADE_RECEIVED`
5. Si diferencia > 0 → `WAITING_DIFFERENCE` → USER paga MP → `PREPARING_TRADE`
   Si diferencia ≤ 0 → directo a `PREPARING_TRADE` (saldo interno)
6. Bot envía skins del marketplace → `BOT_SENT` → `COMPLETED`

### Archivos involucrados
- `OperationController.createExchange()`
- `TradeOperationServiceImpl.createExchange()` + `precioPromedioMarketplace()`
- `MockTradeScheduler.procesar()` — caso EXCHANGE en cada estado

## CASO 4 — CANCELACIÓN / DEVOLUCIÓN

### Regla 1: USER cancela ANTES de entregar skins
- WAITING_PAYMENT / WAITING_USER_TRADE / WAITING_DIFFERENCE → `CANCELLED`
- Se libera stock de las skins del marketplace
- Se borra la entrada del orders.json
- NO se manda trade de devolución

### Regla 2: USER cancela DESPUÉS de entregar skins
- USER_TRADE_RECEIVED / PREPARING_TRADE → `RETURN_PENDING`
- Se crea una segunda Order con operationType=`RETURN` vinculada a la original
- La nueva Order tiene los mismos assetIds que el USER había entregado
- Se escribe entrada en orders.json con direction=`BOT_TO_USER`
- Mock: `RETURN_PENDING` → `RETURN_SENT` → `RETURNED`

### Regla 3: USER pagó diferencia y luego cancela
- La diferencia queda registrada como `priceDifference`
- La lógica actual marca como CANCELLED y deja la devolución como TODO manual
- TODO para producción: integrar refund de MP usando paymentId

### Regla 4: Bot YA envió la skin final
- BOT_SENT / COMPLETED / RETURNED → no se puede cancelar
- Devuelve 400 con mensaje claro

### Archivos involucrados
- `OperationController.cancel()`
- `TradeOperationServiceImpl.cancelOperation()` — todos los casos arriba

## Cuándo apagar el mock (post 10/06)

Cambiar en `application.properties`:
```
mock.enabled=false
```

Y reiniciar el server. El `MockTradeScheduler` deja de ejecutarse.
El bot Node.js real toma el control:
1. Lee `data/orders.json` cada N segundos
2. Por cada entrada PENDING o WAITING_USER_TRADE, hace lo correspondiente
3. Actualiza `orders.json` con el nuevo estado
4. El backend Java tiene que tener un scheduler (o el bot llama un endpoint)
   que lee orders.json y sincroniza los cambios a la BD

**NOTA**: el endpoint `POST /operations/{id}/user-trade-received` ya existe
para que el bot real pueda llamarlo cuando confirme una oferta entrante.

## Validaciones de seguridad

| Validación | Dónde | Por qué |
|---|---|---|
| El item del inventario pertenece al USER | `TradeOperationServiceImpl.createSale/createExchange` | Que no use skins ajenas |
| El item es tradeable en Steam | Idem | Que no falle el trade real |
| El item no está en otra operación activa | `findActiveOrdersWithAssetId` | Que no use la misma skin 2 veces |
| El USER no compra su propia skin del marketplace | `createExchange` | Lógica básica de marketplace |
| La orden pertenece al USER al cancelar | `cancelOperation` | Que no cancele órdenes ajenas |
| El payment del webhook tiene externalReference válida | `processWebhook` | Que no se manipule el estado de otra orden |

## Demo para defensa universitaria

1. Login con `amigo@mail.com` (tiene SteamID configurado y items reales)
2. GET /inventario → mostrar items reales sincronizados desde Steam
3. **CASO COMPRA**:
   - Login con `user@mail.com`
   - PATCH /carrito/skins/{id}?cantidad=1 → POST /order/from-carrito
   - POST /payments/bricks/preferences/orders/{id} → simular pago aprobado
   - Esperar 15-20s → GET /operations/{id}/status → ver `COMPLETED`
4. **CASO VENTA**:
   - Login con `amigo@mail.com`
   - POST /operations/sale con sus inventarioItemIds
   - GET /operations/{id}/status → `WAITING_USER_TRADE`
   - Esperar 10s → `USER_TRADE_RECEIVED` → `PREPARING_TRADE` → `COMPLETED`
5. **CASO CANCELACIÓN CON DEVOLUCIÓN**:
   - POST /operations/sale → esperar 15s para que pase a USER_TRADE_RECEIVED
   - POST /operations/{id}/cancel → ver que crea una RETURN
   - GET /operations/me → mostrar dos órdenes: la original (RETURN_PENDING) y la devolución (RETURN_PENDING → RETURN_SENT → RETURNED)
6. Abrir `data/orders.json` y mostrar las entradas escritas/actualizadas

## Lo que NO se hizo (y por qué)

- **Refund automático en MP cuando se cancela**: requiere el método `refund()` de
  MP SDK. Lo dejé como TODO para producción porque para el TPO no es necesario.
- **Webhook de Steam para validar trade real**: el bot Node.js que ya tenés
  hace esa parte. El backend Java solo necesita el endpoint
  `/operations/{id}/user-trade-received` que ya está expuesto.
- **Refresh asíncrono del orders.json hacia la BD**: cuando el bot real esté
  activo, sería necesario un scheduler que relea orders.json cada N segundos
  y sincronice estados que el bot haya modificado. Lo dejé para after-10/06
  porque ahora el flujo Java→archivo es unidireccional.
