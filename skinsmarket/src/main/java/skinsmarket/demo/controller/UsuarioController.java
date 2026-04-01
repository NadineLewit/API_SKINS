package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.dto.LoginRequest;
import skinsmarket.demo.dto.LoginResponse;
import skinsmarket.demo.dto.RegistroRequest;
import skinsmarket.demo.entity.Usuario;
import skinsmarket.demo.service.IUsuarioService;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final IUsuarioService usuarioService;

    // POST /usuarios/registro
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(usuarioService.registrar(request));
    }

    // POST /usuarios/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    // GET /usuarios/me
    @GetMapping("/me")
    public ResponseEntity<Usuario> miPerfil(@AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.obtenerPorUsername(userDetails.getUsername());
        return ResponseEntity.ok(usuario);
    }

    // PUT /usuarios/me
    @PutMapping("/me")
    public ResponseEntity<Usuario> actualizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(userDetails.getUsername(), request));
    }

    // DELETE /usuarios/me
    @DeleteMapping("/me")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UserDetails userDetails) {
        usuarioService.eliminar(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}