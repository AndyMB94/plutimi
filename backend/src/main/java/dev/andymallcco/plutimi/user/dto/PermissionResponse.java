package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PermissionResponse(

        @Schema(example = "1")
        Long id,

        @Schema(description = "Formato <recurso>:<acción>", example = "catalog:manage")
        String code,

        @Schema(example = "Gestionar categorías, marcas y productos, y ajustar el stock")
        String description) {
}
