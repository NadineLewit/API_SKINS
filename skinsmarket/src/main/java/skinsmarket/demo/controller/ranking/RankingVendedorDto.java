package skinsmarket.demo.controller.ranking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de un vendedor en el ranking de top sellers.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingVendedorDto {

    /** Posición en el ranking (1 = primero). */
    private Integer posicion;

    /** ID del vendedor. */
    private Long vendedorId;

    /** Email del vendedor (también es su identificador visible). */
    private String email;

    /** Cantidad total de ventas (eventos SALE). */
    private Long totalVentas;

    /** Suma de ingresos del vendedor (precio * cantidad). */
    private Double totalIngresos;
}
