package skinsmarket.demo.service;

import skinsmarket.demo.controller.admin.AdminUserResponse;
import skinsmarket.demo.controller.user.UserRequest;
import skinsmarket.demo.controller.user.UserResponse;
import skinsmarket.demo.entity.Role;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.utils.InfoValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        // Validar formato del nuevo email si se proporcionó
        if (request.getEmail() != null) {
            if (!InfoValidator.isValidEmail(request.getEmail())) {
                throw new EmailException();
            }
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getEmail().equals(email)) {
                    try { throw new EmailException(); }
                    catch (EmailException e) { throw new RuntimeException(e); }
                }
            });
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ── Cambio de username con restricción de 15 días ──────────────────
        if (request.getUsername() != null
                && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {

            // Validar que no hayan pasado menos de 15 días desde el último cambio
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

            // Validar que el nuevo username no esté en uso
            userRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new RuntimeException("El username ya está en uso");
                }
            });

            user.setUsername(request.getUsername());
            user.setUsernameChangedAt(LocalDateTime.now());
        }

        // ── Resto de campos ────────────────────────────────────────────────
        if (request.getEmail()     != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getPassword()  != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        UserResponse userResponse = new UserResponse();
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        return userResponse;
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        UserResponse userResponse = new UserResponse();
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        return userResponse;
    }

    @Override
    public List<AdminUserResponse> getAllUsers() {
        List<AdminUserResponse> usersResponse = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            AdminUserResponse resp = new AdminUserResponse();
            resp.setId(user.getId());
            resp.setEmail(user.getEmail());
            resp.setFirstName(user.getFirstName());
            resp.setLastName(user.getLastName());
            resp.setRole(user.getRole());
            usersResponse.add(resp);
        }
        return usersResponse;
    }
}
