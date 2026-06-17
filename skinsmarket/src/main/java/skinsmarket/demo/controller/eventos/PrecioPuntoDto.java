package skinsmarket.demo.controller.eventos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de un punto del historial de precios.
 *
 * Usado en dos formas:
 *   - Historial individual de una Skin: cada punto es un evento puntual
 *     (PRICE_CHANGE o SALE). En este caso `precioPromedio` = precio del evento
 *     y los min/max/muestras quedan en null.
 *   - Historial agregado por catálogo: cada punto es un día con todos los
 *     precios observados ese día agregados (avg, min, max y cuántas muestras).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrecioPuntoDto {

    /** Fecha del punto (timestamp para puntos individuales, fecha del día para agregados). */
    private LocalDateTime fecha;

    /** Precio promedio del día (en agregados) o precio del evento (en individuales). */
    private Double precioPromedio;

    /** Precio mínimo del día (solo en agregados). */
    private Double precioMin;

    /** Precio máximo del día (solo en agregados). */
    private Double precioMax;

    /** Cantidad de muestras agregadas en este día (solo en agregados). */
    private Integer muestras;

    /** Tipo del evento (solo en historial individual). */
    private String tipoEvento;
}
