package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Perfil del usuario autenticado ({@code GET /users/me}, en la 1.7): sus datos, su rol y los
 * códigos de sus permisos, para que el frontend decida qué mostrar.
 */
public record UserProfileResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "ana.torres@example.com")
        String email,

        @Schema(example = "Ana")
        String firstName,

        @Schema(example = "Torres")
        String lastName,

        @Schema(example = "987654321")
        String phone,

        RoleSummaryResponse role,

        @Schema(description = "Códigos de los permisos de su rol, ordenados", example = "[\"catalog:manage\", \"user:read\"]")
        List<String> permissions) {
}
