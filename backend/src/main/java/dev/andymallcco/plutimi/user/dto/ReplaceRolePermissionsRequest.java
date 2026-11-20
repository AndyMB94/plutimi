package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;

/**
 * Conjunto completo de permisos del rol: los que no aparecen se quitan. Puede estar vacío, salvo
 * para ADMIN, que siempre conserva {@code role:manage} y {@code user:manage}.
 */
public record ReplaceRolePermissionsRequest(

        @Schema(description = "IDs de los permisos (ver GET /api/v1/permissions)", example = "[1, 2]")
        @NotNull(message = "La lista de permisos es obligatoria (puede estar vacía)")
        Set<@NotNull(message = "Los IDs de permiso no pueden ser nulos")
            @Positive(message = "Los IDs de permiso deben ser positivos") Long> permissionIds) {
}
