package dev.andymallcco.plutimi.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Permiso con formato {@code <recurso>:<acción>} (ej. {@code catalog:manage}).
 * <p>
 * Solo se crean con migraciones, porque un permiso sin código que lo compruebe no sirve de nada.
 * Por eso la entidad es de solo lectura: no tiene constructor público ni setters, y
 * {@link Immutable} hace que Hibernate nunca genere un UPDATE para ella.
 */
@Entity
@Table(name = "permissions")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission {

    // Sin @GeneratedValue: Java nunca inserta permisos, solo los lee
    @Id
    private Long id;

    private String code;

    private String description;

    private Instant createdAt;

    /**
     * Igualdad por id: los permisos siempre vienen de la base de datos, así que el id nunca es
     * {@code null}. Se usa el getter y no el campo porque {@code other} puede ser un proxy de
     * Hibernate, cuyos campos están vacíos hasta que se inicializa.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Permission other && id != null && id.equals(other.getId());
    }

    /** Constante, coherente con {@link #equals}: igual para una entidad que para su proxy. */
    @Override
    public int hashCode() {
        return Permission.class.hashCode();
    }

}
