package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.dto.CuponResponse;
import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.service.ICuponService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/cupones")
@RequiredArgsConstructor
public class CuponController {

    private final ICuponService cuponService;

    @PostMapping("/validar")
    public ResponseEntity<CuponResponse> validar(@RequestParam String codigo) {
        return ResponseEntity.ok(CuponResponse.fromEntity(cuponService.validar(codigo)));
    }

    @PostMapping
    public ResponseEntity<CuponResponse> crear(@RequestBody Cupon cupon) {
        return ResponseEntity.ok(CuponResponse.fromEntity(cuponService.crear(cupon)));
    }

    @GetMapping
    public ResponseEntity<List<CuponResponse>> listar() {
        List<CuponResponse> cupones = cuponService.listar().stream()
                .map(CuponResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cupones);
    }
}
