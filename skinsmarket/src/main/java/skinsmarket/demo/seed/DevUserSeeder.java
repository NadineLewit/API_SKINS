package skinsmarket.demo.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.Role;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.CarritoRepository;
import skinsmarket.demo.repository.UserRepository;

@Component
public class DevUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CarritoRepository carritoRepository;
    private final PasswordEncoder passwordEncoder;

    public DevUserSeeder(
            UserRepository userRepository,
            CarritoRepository carritoRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.carritoRepository = carritoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureUser("admin", "Admin", "Dev", "admin@mail.com", "admin123", Role.ADMIN);
        ensureUser("user", "User", "Buyer", "user@mail.com", "user123", Role.USER);
        ensureUser("user2", "User", "Seller", "user2@mail.com", "user2123", Role.USER);
    }

    private void ensureUser(
            String username,
            String firstName,
            String lastName,
            String email,
            String password,
            Role role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String finalUsername = availableUsername(username);
            User created = User.builder()
                    .username(finalUsername)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .emailVerified(true)
                    .role(role)
                    .saldo(0.0)
                    .build();
            System.out.println("[DevUserSeeder] Usuario dev creado: " + email + " / " + password);
            return userRepository.save(created);
        });

        boolean changed = false;
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            changed = true;
        }
        if (role == Role.ADMIN && user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            changed = true;
        }
        if (changed) {
            user = userRepository.save(user);
        }

        final User savedUser = user;
        carritoRepository.findByUser(savedUser).orElseGet(() -> {
            Carrito carrito = new Carrito();
            carrito.setUser(savedUser);
            carrito.setEstado(Carrito.Estado.VACIO);
            return carritoRepository.save(carrito);
        });
    }

    private String availableUsername(String base) {
        String candidate = base;
        int suffix = 2;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + "_dev" + suffix;
            suffix += 1;
        }
        return candidate;
    }
}
