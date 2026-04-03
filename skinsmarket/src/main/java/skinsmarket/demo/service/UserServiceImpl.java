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

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de Usuarios.
 *
 * Gestiona actualización de perfil, cambio de roles y listado para el admin.
 *
 * Usa inyección por constructor con @RequiredArgsConstructor (Lombok),
 * que es la práctica recomendada en el TPO aprobado para este servicio.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Inyección por constructor (Lombok genera el constructor con estos campos final)
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cambia el rol de un usuario específico.
     *
     * Valida que el nuevo rol sea un valor válido del enum Role.
     * Lanza RuntimeException si el usuario no existe o el rol es inválido
     * (Spring convierte RuntimeException a HTTP 500 por defecto).
     *
     * @param userId      ID del usuario a modificar
     * @param nuevoRolStr string con el nuevo rol (ej: "ADMIN", "USER")
     */
    @Override
    @Transactional
    public void cambiarRolUser(Long userId, String nuevoRolStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Convertir el string al enum Role; si no es válido lanza excepción descriptiva
        Role nuevoRol;
        try {
            nuevoRol = Role.valueOf(nuevoRolStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rol inválido. Los roles válidos son: USER, ADMIN");
        }

        user.setRole(nuevoRol);
        userRepository.save(user);
    }

    /**
     * Actualiza los datos del perfil del usuario autenticado.
     *
     * Permite actualizaciones parciales: solo se modifican los campos no nulos.
     * Si se provee una nueva contraseña, se hashea con BCrypt antes de guardar.
     * Si el email cambia, se valida su formato con InfoValidator.
     *
     * @param email   email actual del usuario (identificador del token JWT)
     * @param request objeto con los campos a actualizar (todos opcionales)
     * @throws EmailException si el nuevo email tiene formato inválido
     */
    @Override
    @Transactional
    public UserResponse actualizarUser(String email, UserRequest request) throws EmailException {
        // Validar formato del nuevo email si se proporcionó
        if (request.getEmail() != null) {
            if (!InfoValidator.isValidEmail(request.getEmail())) {
                throw new EmailException();
            }
            // Verificar que el nuevo email no esté en uso por OTRO usuario.
            // Sin esta validación, JPA lanzaría DataIntegrityViolationException (500 genérico)
            // porque email tiene unique = true en la BD.
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getEmail().equals(email)) {
                    // El email le pertenece a un usuario distinto al que está actualizando
                    try { throw new EmailException(); }
                    catch (EmailException e) { throw new RuntimeException(e); }
                }
            });
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Actualización parcial: solo modificar campos no nulos (patrón del TPO aprobado)
        if (request.getEmail()     != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());

        // Hash de la nueva contraseña si fue enviada y no está vacía
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        // Construir y devolver el DTO de respuesta (sin datos sensibles)
        UserResponse userResponse = new UserResponse();
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        return userResponse;
    }

    /**
     * Devuelve los datos del perfil de un usuario por su email.
     * Usado en GET /api/v1/users/me para que el usuario vea su propio perfil.
     */
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

    /**
     * Devuelve la lista completa de usuarios para el panel de administración.
     *
     * Mapea cada User a AdminUserResponse (incluye id, email, nombre, apellido y rol).
     * Usa el mismo patrón for + setters manuales que el TPO aprobado.
     */
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