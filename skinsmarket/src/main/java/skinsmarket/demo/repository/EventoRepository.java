package skinsmarket.demo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skinsmarket.demo.entity.Evento;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.SkinCatalogo;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    /** Historial completo de un evento sobre una skin específica. */
    List<Evento> findBySkinAndTipoOrderByFechaAsc(Skin skin, Evento.TipoEvento tipo);

    /** Historial de un tipo de evento sobre un item del catálogo (todas sus publicaciones). */
    List<Evento> findByCatalogoAndTipoOrderByFechaAsc(SkinCatalogo catalogo, Evento.TipoEvento tipo);

    /** Eventos de un tipo en un rango de fechas. */
    List<Evento> findByTipoAndFechaBetweenOrderByFechaAsc(
            Evento.TipoEvento tipo, LocalDateTime desde, LocalDateTime hasta);

    // -------------------------------------------------------------------------
    // Queries agregadas para los rankings
    // -------------------------------------------------------------------------

    /**
     * Top vendedores: cuenta cuántas SALE tiene cada vendedor.
     * Devuelve filas tipo [vendedorId, vendedorEmail, totalVentas, totalIngresos].
     */
    @Query("""
           SELECT e.vendedor.id,
                  e.vendedor.email,
                  COUNT(e),
                  COALESCE(SUM(e.precio * e.cantidad), 0)
           FROM Evento e
           WHERE e.tipo = skinsmarket.demo.entity.Evento.TipoEvento.SALE
             AND e.vendedor IS NOT NULL
           GROUP BY e.vendedor.id, e.vendedor.email
           ORDER BY COUNT(e) DESC
           """)
    List<Object[]> findTopVendedores(Pageable pageable);

    /**
     * Top skins más vendidas (por modelo del catálogo, no por publicación).
     * Devuelve [catalogoId, catalogoName, totalVentas, totalUnidades].
     */
    @Query("""
           SELECT e.catalogo.id,
                  e.catalogo.name,
                  COUNT(e),
                  COALESCE(SUM(e.cantidad), 0)
           FROM Evento e
           WHERE e.tipo = skinsmarket.demo.entity.Evento.TipoEvento.SALE
             AND e.catalogo IS NOT NULL
           GROUP BY e.catalogo.id, e.catalogo.name
           ORDER BY COUNT(e) DESC
           """)
    List<Object[]> findTopSkinsVendidas(Pageable pageable);

    /**
     * Top skins más vistas.
     * Devuelve [catalogoId, catalogoName, totalVistas].
     */
    @Query("""
           SELECT e.catalogo.id,
                  e.catalogo.name,
                  COUNT(e)
           FROM Evento e
           WHERE e.tipo = skinsmarket.demo.entity.Evento.TipoEvento.SKIN_VIEW
             AND e.catalogo IS NOT NULL
           GROUP BY e.catalogo.id, e.catalogo.name
           ORDER BY COUNT(e) DESC
           """)
    List<Object[]> findTopSkinsVistas(Pageable pageable);

    /**
     * Tendencia agregada de precios para un item del catálogo.
     * Devuelve por fecha (día) el precio promedio observado en eventos PRICE_CHANGE y SALE.
     * Útil para chartear "evolución del precio del AK Redline".
     */
    @Query(value = """
           SELECT DATE(e.fecha) AS dia,
                  AVG(e.precio) AS precio_promedio,
                  MIN(e.precio) AS precio_min,
                  MAX(e.precio) AS precio_max,
                  COUNT(*)      AS muestras
           FROM eventos e
           WHERE e.catalogo_id = :catalogoId
             AND e.tipo IN ('PRICE_CHANGE', 'SALE')
             AND e.precio IS NOT NULL
             AND e.fecha >= :desde
           GROUP BY DATE(e.fecha)
           ORDER BY dia ASC
           """, nativeQuery = true)
    List<Object[]> findTendenciaPrecioCatalogo(
            @Param("catalogoId") Long catalogoId,
            @Param("desde") LocalDateTime desde);
}
