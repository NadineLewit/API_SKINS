package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa el Carrito de compras de un usuario.
 *
 *
 * Cada usuario tiene exactamente un carrito (relación OneToOne).
 * El carrito persiste entre sesiones: si el usuario cierra sesión y vuelve,
 * sus ítems siguen en el carrito.
 *
 * Relaciones:
 *   - Un carrito pertenece a un usuario (OneToOne → User)
 *   - Un carrito tiene muchos ítems (OneToMany → ItemCarrito)
 */
@Entity
@Data
@Table(name = "carritos")
public class Carrito {

    // Identificador único generado automáticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dueño del carrito: cada usuario tiene exactamente uno
    // unique = true garantiza la relación uno a uno a nivel de BD
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // Estado actual del carrito
    // VACIO: recién creado, sin ítems
    // ACTIVO: tiene al menos un ítem agregado
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.VACIO;

    // Lista de ítems del carrito (cada ítem = skin + cantidad + precio unitario)
    // CascadeType.ALL: los ítems se persisten/eliminan junto con el carrito
    // orphanRemoval = true: si un ítem se quita de la lista, se elimina de la BD
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrito> items = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Métodos auxiliares de gestión de ítems
    // -------------------------------------------------------------------------

    /**
     * Agrega un ítem al carrito y establece la referencia bidireccional.
     * Cambia el estado del carrito a ACTIVO automáticamente.
     *
     * @param item ítem a agregar al carrito
     */
    public void agregarItem(ItemCarrito item) {
        items.add(item);
        item.setCarrito(this); // Mantener la FK en el ítem apuntando a este carrito
        this.estado = Estado.ACTIVO;
    }

    /**
     * Quita un ítem del carrito y limpia la referencia bidireccional.
     * Si el carrito queda vacío, cambia el estado a VACIO.
     *
     * @param item ítem a quitar del carrito
     */
    public void quitarItem(ItemCarrito item) {
        items.remove(item);
        item.setCarrito(null);
        if (items.isEmpty()) {
            this.estado = Estado.VACIO;
        }
    }

    // -------------------------------------------------------------------------
    // Estado del carrito
    // -------------------------------------------------------------------------

    /**
     * Estados posibles del carrito:
     *   VACIO  → el carrito no tiene ítems
     *   ACTIVO → el carrito tiene al menos un ítem
     */
    public enum Estado {
        VACIO,
        ACTIVO
    }
}
