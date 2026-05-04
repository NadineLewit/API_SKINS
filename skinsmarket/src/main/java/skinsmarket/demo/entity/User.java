package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /**
     * Fecha y hora del último cambio de username.
     * Null si nunca se cambió (se registró con ese username y no lo modificó).
     * Se actualiza cada vez que el usuario cambia el username exitosamente.
     * Se usa para validar la restricción de cambio cada 15 días.
     */
    @Column(name = "username_changed_at")
    private LocalDateTime usernameChangedAt;

    /**
     * SteamID64 del usuario — número de 17 dígitos que identifica unívocamente
     * una cuenta en Steam (ej: "76561198012345678").
     *
     * Se usa para LEER el inventario público vía:
     *   GET https://steamcommunity.com/inventory/{steamId64}/730/2
     *
     * Es opcional: el usuario puede usar el marketplace sin Steam, pero si
     * quiere ver/sincronizar su inventario debe configurarlo.
     */
    @Column(name = "steam_id_64", length = 30)
    private String steamId64;

    /**
     * Trade URL de Steam — link tipo:
     *   https://steamcommunity.com/tradeoffer/new/?partner=12345&token=ABC
     *
     * NO se usa para leer inventario (eso es con steamId64). Se guarda para
     * futuras funcionalidades de envío de ofertas de trade reales.
     *
     * Es opcional: solo necesario si el usuario quiere recibir ofertas concretas.
     */
    @Column(name = "trade_url", length = 500)
    private String tradeUrl;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Order> orders;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
