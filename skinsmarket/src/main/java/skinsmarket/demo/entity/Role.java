package skinsmarket.demo.entity;

/**
 * Enum que representa los roles posibles de un usuario en el sistema.
 *
 * Idéntico al Role del TPO aprobado.
 * Usado en la entidad User y en las reglas de autorización del SecurityConfig.
 *
 * USER  → usuario estándar: puede comprar, gestionar su carrito y ver su historial.
 * ADMIN → administrador: puede gestionar skins, categorías, cupones y usuarios.
 */
public enum Role {
    USER,
    ADMIN
}
