package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Stock insuficiente para completar la compra")
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException() {
        super("Stock insuficiente para completar la compra");
    }
}