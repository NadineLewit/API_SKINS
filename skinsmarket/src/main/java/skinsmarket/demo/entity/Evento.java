package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad Evento — tabla ÚNICA para tracking de eventos del sistema.
 *
 * Esta tabla captura toda la actividad relevante del marketplace en una sola
 * estructura, distinguida por el ENUM `tipo`. Se usa para:
 *   - Historial de precios (PRICE_CHANGE) → series temporales por skin
 *   - Ventas (SALE) → ranking de vendedores y skins más vendidas
 *   - Vistas (SKIN_VIEW) → ranking de skins más populares
 *   - Publicaciones (LISTING_CREATED) → métricas de actividad
 *
 * Diseño "tabla única con ENUM" elegido sobre "tabla por evento":
 *   ✅ Una sola tabla → queries de tendencia simples
 *   ✅ Agregar un tipo nuevo no requiere migración de schema
 *   ✅ Joins más simples
 *   ⚠️ Algunos campos quedan null para ciertos tipos (price solo aplica a
 *       PRICE_CHANGE/SALE) — está bien, lo asumimos como trade-off
 *
 * Los eventos se registran desde el EventoService, llamado por los demás
 * services (OrderService al crear orden, SkinService al editar precio, etc.)
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "eventos", indexes = {
    @Index(name = "idx_evento_tipo_fecha", columnList = "tipo, fecha"),
    @Index(name = "idx_evento_skin", columnList = "skin_id, tipo"),
    @Index(name = "idx_evento_catalogo", columnList = "catalogo_id, tipo"),
    @Index(name = "idx_evento_user", columnList = "user_id, tipo")
})
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de evento — discrimina el resto de campos. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEvento tipo;

    /** Cuándo ocurrió (UTC server time). Indexado para queries temporales. */
    @Column(nullable = false)
    private LocalDateTime fecha;

    /**
     * Skin asociada (publicación de venta).
     * Aplica para: PRICE_CHANGE, SALE, SKIN_VIEW, LISTING_CREATED, LISTING_INACTIVATED
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skin_id")
    @JsonIgnore
    private Skin skin;

    /**
     * Catálogo asociado (item maestro del juego).
     * Se usa para queries de "historial de un modelo de skin" agregando todas
     * las publicaciones de la misma AK Redline aunque sean de distintos vendedores.
     * Se completa cuando la Skin tiene catálogo asociado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogo_id")
    @JsonIgnore
    private SkinCatalogo catalogo;

    /**
     * Usuario asociado al evento.
     * Aplica para: SALE (comprador), LISTING_CREATED (vendedor), SKIN_VIEW (visitante)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * Vendedor asociado (cuando aplica).
     * Aplica para: SALE → para sumar al ranking de vendedores
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    @JsonIgnore
    private User vendedor;

    /**
     * Precio asociado al evento.
     * - PRICE_CHANGE: nuevo precio
     * - SALE: precio unitario al que se vendió
     * - resto: null
     */
    private Double precio;

    /**
     * Precio anterior (solo para PRICE_CHANGE).
     * Permite calcular variaciones y delta sin re-consultar la skin.
     */
    @Column(name = "precio_anterior")
    private Double precioAnterior;

    /**
     * Cantidad asociada al evento (solo para SALE).
     */
    private Integer cantidad;

    public enum TipoEvento {
        /** Cambio de precio en una skin publicada. */
        PRICE_CHANGE,
        /** Venta efectiva de una skin (parte de una orden). */
        SALE,
        /** Vista del detalle de una skin (página de producto). */
        SKIN_VIEW,
        /** Nueva publicación creada. */
        LISTING_CREATED,
        /** Publicación inactivada (baja lógica). */
        LISTING_INACTIVATED
    }
}
