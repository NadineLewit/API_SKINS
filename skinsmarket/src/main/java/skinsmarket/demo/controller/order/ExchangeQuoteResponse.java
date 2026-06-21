package skinsmarket.demo.controller.order;

import lombok.Data;

@Data
public class ExchangeQuoteResponse {
    private Double valorMarketplace;
    private Double valorUsuario;
    private Double diferencia;
    private Double montoAPagar;
    private Double saldoARecibir;
    private Double saldoDisponible;
    private Double saldoRestante;
    private Double saldoFaltante;
    private String resultado;
}
