package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.persistence.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

/**
 * Rol que agrupa permisos. Cada usuario tiene exactamente un rol.
 * <p>
 * Reglas propias: el {@code code} no cambia nunca (es la clave estable que usa el código), los
 * roles de sistema no se pueden borrar y el rol {@code ADMIN} nunca pierde los permisos que
 * permiten gestionar roles y usuarios. Lo que depende de otros datos (código único, permisos que
 * existen, rol sin usuarios) lo comprueba {@code RoleService}.
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends AuditableEntity {

    public static final String ADMIN_CODE = "ADMIN";

    /** Sin ellos nadie podría volver a gestionar roles ni usuarios. */
    private static final Set<String> ADMIN_LOCKED_PERMISSIONS = Set.of("role:manage", "user:manage");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    private String description;

    // Lombok genera isSystem() para un boolean
    private boolean system;

    // Permission es del mismo módulo: asociación JPA sobre la tabla de unión role_permissions.
    // LAZY por defecto en @ManyToMany: los permisos solo se cargan si se piden.
    // @BatchSize evita el N+1 en el listado paginado: al tocar los permisos del primer rol,
    // Hibernate carga los de hasta 50 roles de la página con un solo "WHERE role_id IN (...)",
    // parecido a prefetch_related de Django.
    // Un @EntityGraph no sirve ahí: un JOIN con una colección multiplica las filas y Hibernate
    // tendría que paginar en memoria.
    @ManyToMany
    @BatchSize(size = 50)
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    /** Rol creado desde la API: nunca es de sistema (esos solo se crean con migraciones). */
    public Role(String code, String name, String description) {
        this.code = normalizeCode(code);
        update(name, description);
        this.system = false;
    }

    public void update(String name, String description) {
        Objects.requireNonNull(name, "name must not be null");
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
    }

    /**
     * Reemplaza los permisos. Se modifica el mismo Set para que Hibernate solo borre e inserte
     * las diferencias en role_permissions.
     *
     * @throws AdminRoleLockedPermissionException si es el rol ADMIN y el nuevo conjunto no
     *                                            incluye {@code role:manage} y {@code user:manage}
     */
    public void replacePermissions(Set<Permission> newPermissions) {
        Objects.requireNonNull(newPermissions, "permissions must not be null");
        if (isAdmin()) {
            Set<String> newCodes = newPermissions.stream().map(Permission::getCode).collect(Collectors.toSet());
            Set<String> missing = new TreeSet<>(ADMIN_LOCKED_PERMISSIONS);
            missing.removeAll(newCodes);
            if (!missing.isEmpty()) {
                throw new AdminRoleLockedPermissionException(missing);
            }
        }
        this.permissions.retainAll(newPermissions);
        this.permissions.addAll(newPermissions);
    }

    /**
     * Comprueba la regla propia del borrado. Que no tenga usuarios lo comprueba el service.
     *
     * @throws SystemRoleException si es un rol de sistema
     */
    public void assertDeletable() {
        if (system) {
            throw new SystemRoleException(code);
        }
    }

    public boolean isAdmin() {
        return ADMIN_CODE.equals(code);
    }

    /** Vista de solo lectura: los permisos solo cambian con {@link #replacePermissions(Set)}. */
    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    /** Código tal como se guardará (sin espacios y en mayúsculas). El service lo usa para comprobar la unicidad. */
    static String normalizeCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        return code.strip().toUpperCase(Locale.ROOT);
    }

}
