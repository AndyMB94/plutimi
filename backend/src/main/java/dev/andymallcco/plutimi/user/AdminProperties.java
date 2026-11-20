package dev.andymallcco.plutimi.user;

import jakarta.validation.constraints.Email;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Datos del primer administrador ({@code plutimi.admin.*}). Email y contraseña llegan de las
 * variables de entorno {@code ADMIN_EMAIL} y {@code ADMIN_PASSWORD}; pueden estar vacíos si ya
 * existe un administrador.
 *
 * @param required si es {@code true} (prod) y no hay ningún admin ni datos para crearlo, la
 *                 aplicación no arranca
 */
@Validated
@ConfigurationProperties(prefix = "plutimi.admin")
public record AdminProperties(
        @Email String email,
        String password,
        @DefaultValue("Administrador") String firstName,
        @DefaultValue("Plutimi") String lastName,
        @DefaultValue("false") boolean required) {

    boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    /** Evita que la contraseña aparezca en logs o en el endpoint de configuración. */
    @Override
    public String toString() {
        return "AdminProperties[email=" + email + ", required=" + required + "]";
    }

}
