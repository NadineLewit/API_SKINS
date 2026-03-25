package skinsmarket.demo.service;

import skinsmarket.demo.entity.Carrito;

public interface ICarritoService {
    Carrito obtenerOCrearCarrito(String username);
    Carrito agregarSkin(String username, Long skinId, Integer cantidad);
    Carrito modificarCantidad(String username, Long itemId, Integer cantidad);
    Carrito eliminarItem(String username, Long itemId);
    Carrito vaciar(String username);
}
