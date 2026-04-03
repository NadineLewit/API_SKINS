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
 * Reemplaza WishlistServiceImpl del TPO aprobado con lógica más completa.
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
     * Si la skin ya está en el carrito, suma la cantidad solicitada al ítem existente.
     * Si no está, crea un nuevo ítem con la skin y la cantidad indicada.
     *
     * Valida que la skin esté activa y tenga stock suficiente antes de agregar.
     */
    @Override
    @Transactional
    public Carrito agregarSkin(String email, Long skinId, Integer cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
        }

        // Buscar y validar la skin
        Skin skin = skinRepository.findById(skinId)
                .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + skinId));

        if (!skin.getActive()) {
            throw new RuntimeException("La skin no está disponible");
        }
        if (skin.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente para la cantidad solicitada");
        }

        Carrito carrito = obtenerOCrearCarrito(email);

        // Buscar si la skin ya está en el carrito para sumar cantidad (evitar duplicados)
        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getSkin().getId().equals(skinId))
                .findFirst()
                .orElseGet(() -> {
                    // La skin no estaba en el carrito: crear nuevo ítem
                    ItemCarrito nuevo = new ItemCarrito();
                    nuevo.setCarrito(carrito);
                    nuevo.setSkin(skin);
                    nuevo.setCantidad(0);
                    nuevo.setPrecioUnitario(skin.getFinalPrice()); // precio con descuento aplicado
                    carrito.getItems().add(nuevo);
                    return nuevo;
                });

        // Validar la cantidad ACUMULADA (existente + nueva) contra el stock disponible.
        // BUG sin esto: ítem tiene 3, usuario agrega 5, stock=6 → 3+5=8 > 6 pero pasaba.
        int cantidadTotal = item.getCantidad() + cantidad;
        if (cantidadTotal > skin.getStock()) {
            throw new RuntimeException(
                    "Stock insuficiente. En tu carrito ya tenés " + item.getCantidad()
                            + ", querés agregar " + cantidad
                            + " pero el stock disponible es " + skin.getStock()
            );
        }

        // Sumar la cantidad solicitada al ítem (nuevo o existente)
        item.setCantidad(cantidadTotal);

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
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
        }

        Carrito carrito = obtenerOCrearCarrito(email);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado en el carrito"));

        // Validar que la nueva cantidad no supere el stock disponible de la skin.
        // Sin esta validación, el usuario podría poner cantidad=999 aunque haya solo 2 en stock,
        // y el error recién aparecería al intentar crear la orden (confundiendo al usuario).
        Skin skin = item.getSkin();
        if (cantidad > skin.getStock()) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: " + skin.getStock() + ", solicitado: " + cantidad
            );
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