package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.dto.LoginRequest;
import skinsmarket.demo.dto.LoginResponse;
import skinsmarket.demo.dto.RegistroRequest;
import skinsmarket.demo.dto.UsuarioResponse;
import skinsmarket.demo.service.IUsuarioService;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final IUsuarioService usuarioService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody RegistroRequest request) {
        return ResponseEntity.ok(UsuarioResponse.fromEntity(usuarioService.registrar(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> miPerfil(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(UsuarioResponse.fromEntity(usuarioService.obtenerPorUsername(userDetails.getUsername())));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> actualizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(UsuarioResponse.fromEntity(usuarioService.actualizar(userDetails.getUsername(), request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UserDetails userDetails) {
        usuarioService.eliminar(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
