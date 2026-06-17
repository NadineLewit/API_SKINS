package skinsmarket.demo.service;

import skinsmarket.demo.controller.admin.AdminUserResponse;
import skinsmarket.demo.controller.user.UserRequest;
import skinsmarket.demo.controller.user.UserResponse;
import skinsmarket.demo.exception.EmailException;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Interfaz del servicio de Usuarios.
 *
 * Define el contrato que implementa UserServiceImpl.
 */
@Service
public interface UserService {

    /** Cambia el rol de un usuario (USER ↔ ADMIN). */
    void cambiarRolUser(Long userId, String nuevoRolStr);

    /**
     * Actualiza los datos del perfil del usuario autenticado.
     * @throws EmailException si el nuevo email ya está en uso o tiene formato inválido
     */
    UserResponse actualizarUser(String email, UserRequest request) throws EmailException;

    /** Devuelve los datos del perfil de un usuario por su email. */
    UserResponse getUserByEmail(String email);

    /** Devuelve la lista completa de usuarios (para el panel de admin). */
    List<AdminUserResponse> getAllUsers();
}
