package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Filtros opcionales del listado de usuarios ({@code ?roleId=2&q=ana}). Los que falten quedan en
 * null y no filtran.
 */
public record UserFilter(

        @Parameter(description = "Usuarios con este rol")
        @Positive(message = "El id del rol debe ser positivo")
        Long roleId,

        @Parameter(description = "Texto contenido en el email, el nombre o el apellido (sin distinguir mayúsculas)")
        @Size(max = 100, message = "El texto de búsqueda no puede superar los 100 caracteres")
        String q) {

    public static UserFilter none() {
        return new UserFilter(null, null);
    }

    /** Texto de búsqueda sin espacios sobrantes, o null si no hay nada que buscar. */
    public String searchText() {
        return q == null || q.isBlank() ? null : q.strip();
    }

}
