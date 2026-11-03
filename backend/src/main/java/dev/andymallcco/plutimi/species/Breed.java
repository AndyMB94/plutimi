package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.persistence.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Raza de una especie (dato de referencia). La especie no cambia nunca: las mascotas guardan
 * el par (breed_id, species_id) con una FK compuesta, y moverla de especie las dejaría incoherentes.
 */
@Entity
@Table(name = "breeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Breed extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Asociación JPA permitida: Species es del mismo módulo
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", updatable = false)
    private Species species;

    private String name;

    private boolean active;

    public Breed(Species species, String name) {
        this.species = Objects.requireNonNull(species, "species must not be null");
        this.name = Species.normalizeName(name);
        this.active = true;
    }

    public void update(String name) {
        this.name = Species.normalizeName(name);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

}
