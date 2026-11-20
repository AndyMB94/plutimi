package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Nunca incluye el hash de la contraseña. */
public record UserResponse(

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

        @Schema(description = "false si un administrador desactivó la cuenta", example = "true")
        boolean enabled,

        Instant createdAt,

        Instant updatedAt) {
}
