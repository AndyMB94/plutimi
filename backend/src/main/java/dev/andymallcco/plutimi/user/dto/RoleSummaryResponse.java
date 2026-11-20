package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Rol sin sus permisos, para incluirlo dentro de un usuario. */
public record RoleSummaryResponse(

        @Schema(example = "3")
        Long id,

        @Schema(example = "ADMIN")
        String code,

        @Schema(example = "Administrador")
        String name) {
}
