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
import skinsmarket.demo.entity.Usuario;
import skinsmarket.demo.service.UsuarioService;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // POST /usuarios/registro — crear cuenta nueva
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrar(@RequestBody RegistroRequest request) {
        return ResponseEntity.ok(usuarioService.registrar(request));
    }

    // POST /usuarios/login — iniciar sesión, devuelve el token JWT
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    // GET /usuarios/me — ver mi perfil (requiere token)
    @GetMapping("/me")
    public ResponseEntity<Usuario> miPerfil(@AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioService.obtenerPorUsername(userDetails.getUsername());
        return ResponseEntity.ok(usuario);
    }
}