package skinsmarket.demo.service;

import skinsmarket.demo.entity.Carrito;

/**
 * Interfaz del servicio de Carrito de compras.
 *
 */
public interface CarritoService {

    /** Obtiene el carrito del usuario o lo crea si no existe todavía. */
    Carrito obtenerOCrearCarrito(String email);

    /**
     * Agrega una skin al carrito. Si ya está, suma la cantidad.
     * @param email    email del usuario autenticado
     * @param skinId   ID de la skin a agregar
     * @param cantidad cantidad a agregar (mínimo 1)
     */
    Carrito agregarSkin(String email, Long skinId, Integer cantidad);

    /**
     * Modifica la cantidad de un ítem existente en el carrito.
     * @param email    email del usuario autenticado
     * @param itemId   ID del ítem en el carrito
     * @param cantidad nueva cantidad (debe ser > 0)
     */
    Carrito modificarCantidad(String email, Long itemId, Integer cantidad);

    /**
     * Elimina un ítem específico del carrito.
     * @param email  email del usuario autenticado
     * @param itemId ID del ítem a eliminar
     */
    Carrito eliminarItem(String email, Long itemId);

    /** Vacía completamente el carrito del usuario. */
    Carrito vaciar(String email);
}
