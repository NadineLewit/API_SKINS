package skinsmarket.demo.service;

import skinsmarket.demo.controller.eventos.PrecioPuntoDto;
import skinsmarket.demo.controller.ranking.RankingSkinDto;
import skinsmarket.demo.controller.ranking.RankingVendedorDto;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;

import java.util.List;

/**
 * Servicio para registro y consulta de eventos del sistema.
 *
 * Lo usan los demás services para registrar lo que pasa:
 *   - OrderService llama a registrarVenta() cuando se completa una orden
 *   - SkinService llama a registrarCambioPrecio() cuando cambia el precio
 *   - SkinController llama a registrarVista() en GET /skins/get/{id}
 *
 * Los métodos de registro nunca tiran excepción para no romper el flujo
 * principal: si falla el log de un evento, no debe romper la venta.
 */
public interface EventoService {

    // ─── Métodos de registro (los llaman otros services) ────────────────────

    /** Registra una venta (parte de una orden). */
    void registrarVenta(Order order, Skin skin, int cantidad, double precioUnitario);

    /** Registra un cambio de precio sobre una skin. */
    void registrarCambioPrecio(Skin skin, Double precioAnterior, Double precioNuevo);

    /** Registra una vista de skin (lo llama el controller en cada GET por id). */
    void registrarVista(Skin skin, User user);

    /** Registra publicación nueva. */
    void registrarPublicacion(Skin skin, User vendedor);

    /** Registra inactivación. */
    void registrarInactivacion(Skin skin);

    // ─── Queries para los endpoints públicos ────────────────────────────────

    /** Historial de precios de una skin (publicación). */
    List<PrecioPuntoDto> historialDeSkin(Long skinId);

    /** Historial agregado del catálogo (item del juego). */
    List<PrecioPuntoDto> historialDeCatalogo(Long catalogoId, int dias);

    /** Top N vendedores por cantidad de ventas. */
    List<RankingVendedorDto> topVendedores(int limit);

    /** Top N skins más vendidas (por catálogo). */
    List<RankingSkinDto> topSkinsVendidas(int limit);

    /** Top N skins más vistas (por catálogo). */
    List<RankingSkinDto> topSkinsVistas(int limit);
}
