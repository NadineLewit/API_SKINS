package skinsmarket.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import skinsmarket.demo.controller.eventos.PrecioPuntoDto;
import skinsmarket.demo.controller.ranking.RankingSkinDto;
import skinsmarket.demo.controller.ranking.RankingVendedorDto;
import skinsmarket.demo.entity.Evento;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.EventoRepository;
import skinsmarket.demo.repository.SkinCatalogoRepository;
import skinsmarket.demo.repository.SkinRepository;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de EventoService.
 *
 * Diseño defensivo: los métodos de REGISTRO atrapan TODA excepción y la loguean
 * sin propagarla. Razón: si falla el log de un evento, NO debe romper el flujo
 * principal (orden creada, precio cambiado, etc.). Los eventos son "best effort".
 *
 * Las queries son simples y aprovechan los índices definidos en la entidad Evento.
 */
@Service
public class EventoServiceImpl implements EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private SkinCatalogoRepository skinCatalogoRepository;

    // =========================================================================
    // Métodos de registro (best-effort, NUNCA tiran excepción)
    // =========================================================================

    @Override
    @Transactional
    public void registrarVenta(Order order, Skin skin, int cantidad, double precioUnitario) {
        try {
            Evento e = Evento.builder()
                    .tipo(Evento.TipoEvento.SALE)
                    .fecha(LocalDateTime.now())
                    .skin(skin)
                    .catalogo(skin != null ? skin.getCatalogo() : null)
                    .user(order != null ? order.getUser() : null)
                    .vendedor(skin != null ? skin.getVendedor() : null)
                    .precio(precioUnitario)
                    .cantidad(cantidad)
                    .build();
            eventoRepository.save(e);
        } catch (Exception ex) {
            System.err.println("[EventoService] Error registrando venta: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarCambioPrecio(Skin skin, Double precioAnterior, Double precioNuevo) {
        try {
            // Solo registramos si hay cambio real
            if (precioAnterior != null && precioAnterior.equals(precioNuevo)) return;

            Evento e = Evento.builder()
                    .tipo(Evento.TipoEvento.PRICE_CHANGE)
                    .fecha(LocalDateTime.now())
                    .skin(skin)
                    .catalogo(skin.getCatalogo())
                    .vendedor(skin.getVendedor())
                    .precio(precioNuevo)
                    .precioAnterior(precioAnterior)
                    .build();
            eventoRepository.save(e);
        } catch (Exception ex) {
            System.err.println("[EventoService] Error registrando cambio de precio: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarVista(Skin skin, User user) {
        try {
            Evento e = Evento.builder()
                    .tipo(Evento.TipoEvento.SKIN_VIEW)
                    .fecha(LocalDateTime.now())
                    .skin(skin)
                    .catalogo(skin.getCatalogo())
                    .user(user) // puede ser null si la vista es anónima
                    .build();
            eventoRepository.save(e);
        } catch (Exception ex) {
            System.err.println("[EventoService] Error registrando vista: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarPublicacion(Skin skin, User vendedor) {
        try {
            Evento e = Evento.builder()
                    .tipo(Evento.TipoEvento.LISTING_CREATED)
                    .fecha(LocalDateTime.now())
                    .skin(skin)
                    .catalogo(skin.getCatalogo())
                    .vendedor(vendedor)
                    .precio(skin.getPrice())
                    .build();
            eventoRepository.save(e);
        } catch (Exception ex) {
            System.err.println("[EventoService] Error registrando publicación: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarInactivacion(Skin skin) {
        try {
            Evento e = Evento.builder()
                    .tipo(Evento.TipoEvento.LISTING_INACTIVATED)
                    .fecha(LocalDateTime.now())
                    .skin(skin)
                    .catalogo(skin.getCatalogo())
                    .vendedor(skin.getVendedor())
                    .build();
            eventoRepository.save(e);
        } catch (Exception ex) {
            System.err.println("[EventoService] Error registrando inactivación: " + ex.getMessage());
        }
    }

    // =========================================================================
    // Queries para los endpoints
    // =========================================================================

    @Override
    public List<PrecioPuntoDto> historialDeSkin(Long skinId) {
        Skin skin = skinRepository.findById(skinId)
                .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + skinId));

        List<PrecioPuntoDto> puntos = new ArrayList<>();

        // Combinamos PRICE_CHANGE + SALE para tener un historial completo
        List<Evento> cambios = eventoRepository.findBySkinAndTipoOrderByFechaAsc(
                skin, Evento.TipoEvento.PRICE_CHANGE);
        List<Evento> ventas = eventoRepository.findBySkinAndTipoOrderByFechaAsc(
                skin, Evento.TipoEvento.SALE);

        for (Evento e : cambios) {
            puntos.add(new PrecioPuntoDto(e.getFecha(), e.getPrecio(),
                    null, null, null, "PRICE_CHANGE"));
        }
        for (Evento e : ventas) {
            puntos.add(new PrecioPuntoDto(e.getFecha(), e.getPrecio(),
                    null, null, null, "SALE"));
        }

        // Ordenar cronológicamente
        puntos.sort((a, b) -> a.getFecha().compareTo(b.getFecha()));
        return puntos;
    }

    @Override
    public List<PrecioPuntoDto> historialDeCatalogo(Long catalogoId, int dias) {
        // Validamos que el catálogo exista
        skinCatalogoRepository.findById(catalogoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Catálogo no encontrado: " + catalogoId));

        LocalDateTime desde = LocalDateTime.now().minusDays(dias > 0 ? dias : 30);
        List<Object[]> rows = eventoRepository.findTendenciaPrecioCatalogo(catalogoId, desde);

        List<PrecioPuntoDto> resultado = new ArrayList<>();
        for (Object[] row : rows) {
            // row = [dia (java.sql.Date), avg, min, max, count]
            LocalDateTime fecha = ((Date) row[0]).toLocalDate().atStartOfDay();
            Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : null;
            Double min = row[2] != null ? ((Number) row[2]).doubleValue() : null;
            Double max = row[3] != null ? ((Number) row[3]).doubleValue() : null;
            Integer muestras = row[4] != null ? ((Number) row[4]).intValue() : 0;
            resultado.add(new PrecioPuntoDto(fecha, avg, min, max, muestras, null));
        }
        return resultado;
    }

    @Override
    public List<RankingVendedorDto> topVendedores(int limit) {
        List<Object[]> rows = eventoRepository.findTopVendedores(
                PageRequest.of(0, Math.max(1, limit)));
        List<RankingVendedorDto> resultado = new ArrayList<>();
        int pos = 1;
        for (Object[] row : rows) {
            resultado.add(new RankingVendedorDto(
                    pos++,
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    row[3] != null ? ((Number) row[3]).doubleValue() : 0.0
            ));
        }
        return resultado;
    }

    @Override
    public List<RankingSkinDto> topSkinsVendidas(int limit) {
        List<Object[]> rows = eventoRepository.findTopSkinsVendidas(
                PageRequest.of(0, Math.max(1, limit)));
        List<RankingSkinDto> resultado = new ArrayList<>();
        int pos = 1;
        for (Object[] row : rows) {
            resultado.add(new RankingSkinDto(
                    pos++,
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    row[3] != null ? ((Number) row[3]).longValue() : 0L
            ));
        }
        return resultado;
    }

    @Override
    public List<RankingSkinDto> topSkinsVistas(int limit) {
        List<Object[]> rows = eventoRepository.findTopSkinsVistas(
                PageRequest.of(0, Math.max(1, limit)));
        List<RankingSkinDto> resultado = new ArrayList<>();
        int pos = 1;
        for (Object[] row : rows) {
            resultado.add(new RankingSkinDto(
                    pos++,
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    null
            ));
        }
        return resultado;
    }
}
