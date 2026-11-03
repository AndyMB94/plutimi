package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.persistence.AuditableEntity;
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
 * Especie (dato de referencia). El código es la referencia estable que usan el código y los
 * seeders, así que no cambia nunca; el nombre visible sí. Las especies no se borran: se desactivan.
 */
@Entity
@Table(name = "species")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Species extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    private boolean active;

    public Species(String code, String name) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = normalizeName(name);
        this.active = true;
    }

    public void update(String name) {
        this.name = normalizeName(name);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * Nombre tal como se guardará: sin espacios al inicio o al final y con los espacios internos
     * reducidos a uno. Lo usan también las razas y el service, para comprobar la unicidad antes
     * de modificar la entidad.
     */
    static String normalizeName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return name.strip().replaceAll("\\s+", " ");
    }

}
