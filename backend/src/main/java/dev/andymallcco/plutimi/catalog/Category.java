package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.persistence.AuditableEntity;
import dev.andymallcco.plutimi.shared.text.Slugs;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Categoría del catálogo. El slug se deriva siempre del nombre, así que nunca se desincronizan.
 * Las categorías no se borran: se desactivan.
 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String slug;

    private String description;

    private boolean active;

    public Category(String name, String description) {
        rename(name);
        this.description = normalizeDescription(description);
        this.active = true;
    }

    public void update(String name, String description) {
        rename(name);
        this.description = normalizeDescription(description);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * Nombre tal como se guardará: sin espacios al inicio o al final y con los espacios internos
     * reducidos a uno. El service lo usa para comprobar la unicidad antes de modificar la entidad.
     */
    static String normalizeName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return name.strip().replaceAll("\\s+", " ");
    }

    private void rename(String name) {
        this.name = normalizeName(name);
        this.slug = Slugs.of(this.name);
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.strip();
    }

}
