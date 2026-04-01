package skinsmarket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.ItemCarrito;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.Usuario;
import skinsmarket.demo.exception.StockInsuficienteException;
import skinsmarket.demo.repository.CarritoRepository;
import skinsmarket.demo.repository.ItemCarritoRepository;
import skinsmarket.demo.repository.SkinRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CarritoService implements ICarritoService {

    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final SkinRepository skinRepository;
    private final UsuarioService usuarioService;

    public Carrito obtenerOCrearCarrito(String username) {
        Usuario usuario = usuarioService.obtenerPorUsername(username);
        return carritoRepository.findByUsuario(usuario)
                .orElseGet(() -> {
                    Carrito nuevo = new Carrito();
                    nuevo.setUsuario(usuario);
                    nuevo.setEstado(Carrito.Estado.VACIO);
                    return carritoRepository.save(nuevo);
                });
    }

    public Carrito agregarSkin(String username, Long skinId, Integer cantidad) {
        if (cantidad <= 0) throw new RuntimeException("La cantidad debe ser mayor a 0");

        Skin skin = skinRepository.findById(skinId)
                .orElseThrow(() -> new RuntimeException("Skin no encontrada"));

        if (!skin.getActiva()) throw new RuntimeException("La skin no está disponible");
        if (skin.getStock() < cantidad) throw new StockInsuficienteException();

        // Un usuario no puede comprar su propia skin
        if (skin.getVendedor() != null && skin.getVendedor().getUsername().equals(username)) {
            throw new RuntimeException("No podés agregar al carrito una skin que vos mismo publicaste");
        }

        Carrito carrito = obtenerOCrearCarrito(username);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getSkin().getId().equals(skinId))
                .findFirst()
                .orElseGet(() -> {
                    ItemCarrito nuevo = new ItemCarrito();
                    nuevo.setCarrito(carrito);
                    nuevo.setSkin(skin);
                    nuevo.setCantidad(0);
                    nuevo.setPrecioUnitario(skin.getPrecio());
                    carrito.getItems().add(nuevo);
                    return nuevo;
                });

        item.setCantidad(item.getCantidad() + cantidad);
        item.setSubtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())));

        carrito.setEstado(Carrito.Estado.ACTIVO);
        return carritoRepository.save(carrito);
    }

    public Carrito modificarCantidad(String username, Long itemId, Integer cantidad) {
        if (cantidad <= 0) throw new RuntimeException("La cantidad debe ser mayor a 0");

        Carrito carrito = obtenerOCrearCarrito(username);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item no encontrado en el carrito"));

        item.setCantidad(cantidad);
        item.setSubtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad)));

        return carritoRepository.save(carrito);
    }

    public Carrito eliminarItem(String username, Long itemId) {
        Carrito carrito = obtenerOCrearCarrito(username);
        carrito.getItems().removeIf(i -> i.getId().equals(itemId));
        if (carrito.getItems().isEmpty()) {
            carrito.setEstado(Carrito.Estado.VACIO);
        }
        return carritoRepository.save(carrito);
    }

    public Carrito vaciar(String username) {
        Carrito carrito = obtenerOCrearCarrito(username);
        carrito.getItems().clear();
        carrito.setEstado(Carrito.Estado.VACIO);
        return carritoRepository.save(carrito);
    }
}