package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.service.CuponService;
import skinsmarket.demo.service.ICuponService;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/cupones")
@RequiredArgsConstructor
public class CuponController {

    private final ICuponService cuponService;

    @PostMapping("/validar")
    public ResponseEntity<Cupon> validar(@RequestParam String codigo) {
        return ResponseEntity.ok(cuponService.validar(codigo));
    }

    @PostMapping
    public ResponseEntity<Cupon> crear(@RequestBody Cupon cupon) {
        return ResponseEntity.ok(cuponService.crear(cupon));
    }

    @GetMapping
    public ResponseEntity<List<Cupon>> listar() {
        return ResponseEntity.ok(cuponService.listar());
    }
}