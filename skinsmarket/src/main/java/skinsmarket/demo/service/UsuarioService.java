package skinsmarket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import skinsmarket.demo.dto.LoginRequest;
import skinsmarket.demo.dto.LoginResponse;
import skinsmarket.demo.dto.RegistroRequest;
import skinsmarket.demo.entity.Usuario;
import skinsmarket.demo.exception.UsuarioNoEncontradoException;
import skinsmarket.demo.repository.UsuarioRepository;
import skinsmarket.demo.security.JwtUtil;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public Usuario registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Ya existe un usuario con ese username");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword())); // encriptamos la password
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setRol(Usuario.Rol.USER);

        return usuarioRepository.save(usuario);
    }

    public LoginResponse login(LoginRequest request) {
        // Spring Security verifica usuario y contraseña
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Si llegamos acá, las credenciales son correctas
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsuarioNoEncontradoException());

        String token = jwtUtil.generarToken(usuario.getUsername());

        return new LoginResponse(token, usuario.getUsername(), usuario.getRol().name());
    }

    public Usuario obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsuarioNoEncontradoException());
    }
    public Usuario actualizar(String username, RegistroRequest request) {
        Usuario usuario = obtenerPorUsername(username);
        if (request.getNombre() != null) usuario.setNombre(request.getNombre());
        if (request.getApellido() != null) usuario.setApellido(request.getApellido());
        if (request.getEmail() != null) usuario.setEmail(request.getEmail());
        return usuarioRepository.save(usuario);
    }

    public void eliminar(String username) {
        Usuario usuario = obtenerPorUsername(username);
        usuarioRepository.delete(usuario);
    }
}
