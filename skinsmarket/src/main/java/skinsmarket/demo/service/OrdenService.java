package skinsmarket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skinsmarket.demo.entity.*;
import skinsmarket.demo.exception.CuponInvalidoException;
import skinsmarket.demo.exception.StockInsuficienteException;
import skinsmarket.demo.repository.*;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenService implements IOrdenService {

    private final OrdenRepository ordenRepository;
    private final CarritoRepository carritoRepository;
    private final SkinRepository skinRepository;
    private final CuponRepository cuponRepository;
    private final UsuarioService usuarioService;

    public Orden finalizarCompra(String username, String codigoCupon) {
        Usuario usuario = usuarioService.obtenerPorUsername(username);

        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));

        if (carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        Orden orden = new Orden();
        orden.setUsuario(usuario);
        orden.setEstado(Orden.Estado.PENDIENTE);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemCarrito item : carrito.getItems()) {
            Skin skin = item.getSkin();
            if (skin.getStock() < item.getCantidad()) {
                throw new StockInsuficienteException();
            }
            skin.setStock(skin.getStock() - item.getCantidad());
            skinRepository.save(skin);

            DetalleOrden detalle = new DetalleOrden();
            detalle.setOrden(orden);
            detalle.setSkin(skin);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getPrecioUnitario());
            detalle.setSubtotal(item.getSubtotal());
            orden.getDetalles().add(detalle);

            subtotal = subtotal.add(item.getSubtotal());
        }

        BigDecimal descuento = BigDecimal.ZERO;
        if (codigoCupon != null && !codigoCupon.isBlank()) {
            Cupon cupon = cuponRepository.findByCodigo(codigoCupon)
                    .orElseThrow(() -> new CuponInvalidoException());
            if (!cupon.getActivo()) throw new CuponInvalidoException();
            if (cupon.getFechaVencimiento() != null &&
                    cupon.getFechaVencimiento().isBefore(java.time.LocalDate.now())) {
                throw new CuponInvalidoException();
            }
            descuento = subtotal.multiply(BigDecimal.valueOf(cupon.getPorcentaje() / 100));
        }

        orden.setSubtotal(subtotal);
        orden.setDescuentoTotal(descuento);
        orden.setTotal(subtotal.subtract(descuento));
        orden.setEstado(Orden.Estado.FINALIZADA);

        carrito.getItems().clear();
        carrito.setEstado(Carrito.Estado.VACIO);
        carritoRepository.save(carrito);

        return ordenRepository.save(orden);
    }

    public Orden cancelarOrden(Long id, String username) {
        Orden orden = obtenerPorId(id);
        if (!orden.getUsuario().getUsername().equals(username)) {
            throw new RuntimeException("No tenés permiso para cancelar esta orden");
        }
        if (orden.getEstado() == Orden.Estado.FINALIZADA) {
            throw new RuntimeException("No se puede cancelar una orden ya finalizada");
        }
        // Devolver stock
        for (DetalleOrden detalle : orden.getDetalles()) {
            Skin skin = detalle.getSkin();
            skin.setStock(skin.getStock() + detalle.getCantidad());
            skinRepository.save(skin);
        }
        orden.setEstado(Orden.Estado.CANCELADA);
        return ordenRepository.save(orden);
    }

    public List<Orden> misOrdenes(String username) {
        Usuario usuario = usuarioService.obtenerPorUsername(username);
        return ordenRepository.findByUsuario(usuario);
    }

    public Orden obtenerPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }

    public List<Orden> misVentas(String username) {
        Usuario vendedor = usuarioService.obtenerPorUsername(username);
        return ordenRepository.findByDetalles_Skin_Vendedor(vendedor);
    }
}