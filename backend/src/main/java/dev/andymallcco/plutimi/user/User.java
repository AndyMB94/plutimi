package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.persistence.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Usuario de la aplicación. El email se guarda siempre en minúsculas y los usuarios no se borran:
 * se desactivan.
 * <p>
 * La regla "no puede quedar sin ningún administrador activo" depende de los demás usuarios, así
 * que la comprueba {@code UserService} antes de llamar a {@link #changeRole} o {@link #disable}.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String passwordHash;

    private String firstName;

    private String lastName;

    private String phone;

    // Role es del mismo módulo: asociación JPA permitida
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    private boolean enabled;

    /**
     * @param passwordHash hash ya calculado (BCrypt): la entidad nunca ve la contraseña en claro
     */
    public User(String email, String passwordHash, String firstName, String lastName, String phone, Role role) {
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.phone = phone == null || phone.isBlank() ? null : phone.strip();
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.enabled = true;
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "role must not be null");
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    /** Administrador que todavía puede entrar: cuenta para la regla del último admin. */
    public boolean isActiveAdmin() {
        return enabled && role.isAdmin();
    }

    /** Email tal como se guardará (sin espacios y en minúsculas). El service lo usa para buscar y comprobar la unicidad. */
    static String normalizeEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String requireText(String text, String field) {
        Objects.requireNonNull(text, field + " must not be null");
        return text.strip();
    }

}
