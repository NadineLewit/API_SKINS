package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una Categoría de skins.
 *
 * Estructura idéntica al Category del TPO aprobado.
 * Las categorías permiten organizar las skins por tipo de ítem dentro del juego.
 *
 * Ejemplos de categorías: Rifle, Pistola, Cuchillo, Guante, Subfusil.
 *
 * Relaciones:
 *   - Una categoría puede estar asociada a muchas skins (OneToMany ← Skin)
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "categories")
public class Category {

    /**
     * Constructor de conveniencia para crear categorías solo con nombre.
     * Usado en el CategoryServiceImpl al crear desde el CategoryRequest.
     */
    public Category(String name) {
        this.name = name;
    }

    // Identificador único generado automáticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre de la categoría, debe ser único para evitar duplicados
    // La validación de duplicados se realiza en el CategoryServiceImpl
    @Column(nullable = false, unique = true)
    private String name;
}
