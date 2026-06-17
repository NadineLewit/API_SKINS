package skinsmarket.demo.controller.ranking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de una skin en el ranking (más vendidas, más vistas, etc.).
 *
 * El ranking se hace siempre por catálogo (modelo del juego), no por
 * publicación individual: el ranking de "AK-47 Redline" suma todas las
 * publicaciones de esa skin de cualquier vendedor.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingSkinDto {

    /** Posición en el ranking (1 = primero). */
    private Integer posicion;

    /** ID del item en el catálogo. */
    private Long catalogoId;

    /** Nombre del item (ej: "AK-47 | Redline"). */
    private String nombre;

    /** Métrica del ranking (ventas o vistas, según endpoint). */
    private Long total;

    /** Cantidad de unidades (solo aplica a ranking de ventas). */
    private Long unidades;
}
