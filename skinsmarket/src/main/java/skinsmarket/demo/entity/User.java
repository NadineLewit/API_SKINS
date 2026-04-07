package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Entidad que representa a un usuario del marketplace de skins.
 *
 *
 * Relaciones:
 *   - Un usuario tiene muchas órdenes de compra (OneToMany → Order)
 *   - Un usuario tiene un carrito (OneToOne ← Carrito)
 *   - Un usuario puede publicar muchas skins como vendedor (OneToMany ← Skin)
 *
 * El campo email actúa como username en Spring Security (identificador único de login).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    // Identificador único generado automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre de usuario único: requerido por el TPO para registro y visualización.
    // La autenticación (login) se hace por EMAIL, no por username,
    // para simplificar el flujo del frontend (según indicación del docente).
    @Column(nullable = false, unique = true)
    private String username;

    // Email del usuario: actúa como identificador para el login y el JWT
    // Debe ser único en toda la tabla
    @Column(nullable = false, unique = true)
    private String email;

    // Contraseña almacenada como hash BCrypt (nunca en texto plano)
    @Column(nullable = false)
    private String password;

    // Nombre de pila del usuario
    @Column(nullable = false)
    private String firstName;

    // Apellido del usuario
    @Column(nullable = false)
    private String lastName;

    // Lista de órdenes de compra realizadas por el usuario
    // mappedBy indica que Order es el dueño de la relación (tiene la FK user_id)
    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Order> orders;

    // Rol del usuario en el sistema: USER (comprador) o ADMIN
    // @Builder.Default asegura que al construir con el builder el rol por defecto sea USER
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    // -------------------------------------------------------------------------
    // Implementación de UserDetails (requerida por Spring Security)
    // -------------------------------------------------------------------------

    /**
     * Devuelve los roles/permisos del usuario como GrantedAuthority.
     * Spring Security los usa para evaluar reglas de autorización como
     * hasAnyAuthority('ADMIN') o hasAnyAuthority('USER').
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Devuelve el identificador de login del usuario.
     * En este sistema usamos el email como username.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * La cuenta nunca expira en este sistema.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * La cuenta nunca se bloquea automáticamente en este sistema.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Las credenciales (contraseña) nunca expiran en este sistema.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * El usuario siempre está habilitado (sin baja lógica de usuarios).
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}