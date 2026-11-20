package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.persistence.AuditableEntity;
import dev.andymallcco.plutimi.shared.text.Slugs;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Marca de productos. El slug se deriva siempre del nombre, así que nunca se desincronizan.
 * Las marcas no se borran: se desactivan.
 */
@Entity
@Table(name = "brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String slug;

    private boolean active;

    public Brand(String name) {
        rename(name);
        this.active = true;
    }

    public void rename(String name) {
        this.name = CatalogNames.normalize(name);
        this.slug = Slugs.of(this.name);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

}
