package skinsmarket.demo.service;

import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.ItemCarrito;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.CarritoRepository;
import skinsmarket.demo.repository.ItemCarritoRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de Carrito de compras.
 *
 * Usa @Autowired en atributos y @Transactional, igual que los servicios del TPO.
 *
 * Lógica principal: el carrito persiste entre sesiones. Cada usuario tiene
 * exactamente un carrito que se crea la primera vez que accede.
 */
@Service
public class CarritoServiceImpl implements CarritoService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Obtiene el carrito del usuario o lo crea vacío si no tiene uno.
     *
     * Patrón orElseGet: si no hay carrito en BD, se crea y persiste uno nuevo.
     * Es el punto de entrada de todos los demás métodos.
     */
    @Override
    public Carrito obtenerOCrearCarrito(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));

        // Buscar el carrito existente, o crear uno nuevo si es la primera vez
        return carritoRepository.findByUser(user).orElseGet(() -> {
            Carrito nuevo = new Carrito();
            nuevo.setUser(user);
            nuevo.setEstado(Carrito.Estado.VACIO);
            return carritoRepository.save(nuevo);
        });
    }

    /**
     * Agrega una skin al carrito del usuario.
     *
     * Cada publicación es una skin única: no se manejan cantidades mayores a 1.
     *
     * Valida que la skin esté activa y tenga stock suficiente antes de agregar.
     */
    @Override
    @Transactional
    public Carrito agregarSkin(String email, Long skinId, Integer cantidad) {
        if (cantidad != null && cantidad != 1) {
            throw new RuntimeException("Cada skin publicada es única. La cantidad debe ser 1");
        }

        // Buscar y validar la skin
        Skin skin = skinRepository.findById(skinId)
                .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + skinId));

        if (!skin.getActive()) {
            throw new RuntimeException("La skin no está disponible");
        }
        if (Boolean.FALSE.equals(skin.getVendible())) {
            throw new RuntimeException("La skin no está habilitada para compra directa");
        }
        if (skin.getStock() == null || skin.getStock() < 1) {
            throw new RuntimeException("La skin ya fue reservada o vendida");
        }

        Carrito carrito = obtenerOCrearCarrito(email);

        boolean yaEstaEnCarrito = carrito.getItems().stream()
                .filter(i -> i.getSkin().getId().equals(skinId))
                .findFirst()
                .isPresent();
        if (yaEstaEnCarrito) {
            throw new RuntimeException("Esa skin ya está en tu carrito");
        }

        ItemCarrito item = new ItemCarrito();
        item.setCarrito(carrito);
        item.setSkin(skin);
        item.setCantidad(1);
        item.setPrecioUnitario(skin.getFinalPrice());
        carrito.getItems().add(item);

        // Actualizar estado del carrito a ACTIVO
        carrito.setEstado(Carrito.Estado.ACTIVO);
        return carritoRepository.save(carrito);
    }

    /**
     * Modifica la cantidad de un ítem específico en el carrito.
     *
     * La cantidad debe ser > 0. Para eliminar un ítem usar eliminarItem().
     * Lanza excepción si el ítem no pertenece al carrito del usuario.
     */
    @Override
    @Transactional
    public Carrito modificarCantidad(String email, Long itemId, Integer cantidad) {
        if (cantidad == null || cantidad != 1) {
            throw new RuntimeException("Cada skin publicada es única. La cantidad debe ser 1");
        }

        Carrito carrito = obtenerOCrearCarrito(email);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado en el carrito"));

        Skin skin = item.getSkin();
        if (Boolean.FALSE.equals(skin.getVendible())) {
            throw new RuntimeException("La skin no está habilitada para compra directa");
        }
        if (skin.getStock() == null || skin.getStock() < 1) {
            throw new RuntimeException("La skin ya fue reservada o vendida");
        }

        item.setCantidad(cantidad);
        return carritoRepository.save(carrito);
    }

    /**
     * Elimina un ítem específico del carrito.
     *
     * Usa removeIf para quitar el ítem de la lista (orphanRemoval en Carrito se encarga
     * de eliminarlo de la BD). Si el carrito queda vacío, cambia el estado a VACIO.
     */
    @Override
    @Transactional
    public Carrito eliminarItem(String email, Long itemId) {
        Carrito carrito = obtenerOCrearCarrito(email);

        // Eliminar el ítem de la lista (orphanRemoval lo borra de BD en el save)
        carrito.getItems().removeIf(i -> i.getId().equals(itemId));

        // Actualizar el estado si el carrito quedó vacío
        if (carrito.getItems().isEmpty()) {
            carrito.setEstado(Carrito.Estado.VACIO);
        }

        return carritoRepository.save(carrito);
    }

    /**
     * Vacía completamente el carrito del usuario.
     *
     * clear() vacía la lista; orphanRemoval en Carrito elimina todos los ítems de BD.
     * El estado vuelve a VACIO.
     */
    @Override
    @Transactional
    public Carrito vaciar(String email) {
        Carrito carrito = obtenerOCrearCarrito(email);
        carrito.getItems().clear();
        carrito.setEstado(Carrito.Estado.VACIO);
        return carritoRepository.save(carrito);
    }
}
