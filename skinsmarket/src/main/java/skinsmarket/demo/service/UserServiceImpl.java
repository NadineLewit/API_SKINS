package skinsmarket.demo.service;

import skinsmarket.demo.controller.admin.AdminUserResponse;
import skinsmarket.demo.controller.auth.ForgotPasswordRequest;
import skinsmarket.demo.controller.user.UserRequest;
import skinsmarket.demo.controller.user.UserResponse;
import skinsmarket.demo.entity.Role;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.utils.InfoValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    @Autowired
    private InventarioService inventarioService;

    @Override
    @Transactional
    public void cambiarRolUser(Long userId, String nuevoRolStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Role nuevoRol;
        try {
            nuevoRol = Role.valueOf(nuevoRolStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rol inválido. Los roles válidos son: USER, ADMIN");
        }
        user.setRole(nuevoRol);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse actualizarUser(String email, UserRequest request) throws EmailException {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El body de actualización es obligatorio");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean emailCambio = false;
        if (request.getEmail() != null) {
            String nuevoEmail = InfoValidator.normalizeEmail(request.getEmail());
            if (!InfoValidator.isValidEmail(nuevoEmail)) {
                throw new EmailException();
            }
            userRepository.findByEmail(nuevoEmail).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    try { throw new EmailException(); }
                    catch (EmailException e) { throw new RuntimeException(e); }
                }
            });
            request.setEmail(nuevoEmail);
            emailCambio = !nuevoEmail.equals(user.getEmail());
        }

        // ── Cambio de username con restricción de 15 días ──────────────────
        if (request.getUsername() != null
                && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getRealUsername())) {

            if (user.getUsernameChangedAt() != null) {
                long diasDesdeUltimoCambio = ChronoUnit.DAYS.between(
                        user.getUsernameChangedAt(), LocalDateTime.now());

                if (diasDesdeUltimoCambio < 15) {
                    long diasRestantes = 15 - diasDesdeUltimoCambio;
                    throw new RuntimeException(
                        "Solo podés cambiar el username cada 15 días. " +
                        "Podés volver a cambiarlo en " + diasRestantes + " día/s."
                    );
                }
            }

            userRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new RuntimeException("El username ya está en uso");
                }
            });

            user.setUsername(request.getUsername());
            user.setUsernameChangedAt(LocalDateTime.now());
        }

        // ── Resto de campos ────────────────────────────────────────────────
        if (request.getEmail()     != null) {
            user.setEmail(request.getEmail());
            if (emailCambio) {
                user.setEmailVerified(false);
            }
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getPassword()  != null && !request.getPassword().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Para cambiar la contraseña solicitá el link por mail.");
        }

        // ── Steam ──────────────────────────────────────────────────────────
        boolean steamIdCambio = false;
        if (request.getSteamId64() != null) {
            String nuevo = request.getSteamId64().trim();
            if (!nuevo.isBlank() && !nuevo.matches("\\d{17}")) {
                throw new RuntimeException(
                        "SteamID64 inválido. Debe ser un número de 17 dígitos.");
            }
            String anterior = user.getSteamId64();
            if (!Objects.equals(anterior, nuevo)) {
                user.setSteamId64(nuevo.isBlank() ? null : nuevo);
                steamIdCambio = !nuevo.isBlank();
            }
        }
        if (request.getTradeUrl() != null) {
            String nuevo = request.getTradeUrl().trim();
            user.setTradeUrl(nuevo.isBlank() ? null : nuevo);
        }
        if (request.getAliasCobro() != null) {
            String nuevo = request.getAliasCobro().trim();
            user.setAliasCobro(nuevo.isBlank() ? null : nuevo);
        }

        userRepository.save(user);

        if (emailCambio) {
            ForgotPasswordRequest verificationRequest = new ForgotPasswordRequest();
            verificationRequest.setEmail(user.getEmail());
            authenticationService.resendVerification(verificationRequest);
        }

        // ── Sync automático del inventario si cambió el SteamID ────────────
        // IMPORTANTE: usamos sincronizarAislado() porque está marcado como
        // @Transactional(REQUIRES_NEW). Esto significa que corre en su propia
        // transacción independiente — si Steam falla (rate limit, inventario
        // privado, etc.), el rollback queda contenido ahí adentro y NO
        // contamina la transacción de actualizarUser(), que ya commiteó el
        // perfil sin problemas.
        //
        // Si llamáramos a sincronizar() (sin REQUIRES_NEW), una excepción
        // adentro marcaría TODA la transacción como rollback-only, y aunque
        // la atrapemos con try/catch, al hacer commit explotaría con
        // UnexpectedRollbackException.
        if (steamIdCambio) {
            try {
                inventarioService.sincronizarAislado(user.getEmail());
            } catch (Exception e) {
                System.err.println(
                        "Sync automático del inventario falló para " + user.getEmail() +
                        ": " + e.getMessage());
            }
        }

        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return mapToResponse(user);
    }

    @Override
    public List<AdminUserResponse> getAllUsers() {
        List<AdminUserResponse> usersResponse = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            AdminUserResponse resp = new AdminUserResponse();
            resp.setId(user.getId());
            resp.setUsername(user.getRealUsername());
            resp.setEmail(user.getEmail());
            resp.setFirstName(user.getFirstName());
            resp.setLastName(user.getLastName());
            resp.setSaldo(user.getSaldo());
            resp.setRole(user.getRole());
            resp.setAliasCobro(user.getAliasCobro());
            usersResponse.add(resp);
        }
        return usersResponse;
    }

    private UserResponse mapToResponse(User user) {
        UserResponse r = new UserResponse();
        r.setUsername(user.getRealUsername());
        r.setEmail(user.getEmail());
        r.setEmailVerified(user.getEmailVerified());
        r.setFirstName(user.getFirstName());
        r.setLastName(user.getLastName());
        r.setSaldo(user.getSaldo());
        r.setRole(user.getRole());
        r.setSteamId64(user.getSteamId64());
        r.setTradeUrl(user.getTradeUrl());
        r.setAliasCobro(user.getAliasCobro());
        return r;
    }
}
