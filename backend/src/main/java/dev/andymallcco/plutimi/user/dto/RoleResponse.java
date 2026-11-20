package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record RoleResponse(

        @Schema(example = "4")
        Long id,

        @Schema(example = "STOCK_MANAGER")
        String code,

        @Schema(example = "Encargado de almacén")
        String name,

        @Schema(example = "Gestiona el catálogo y el stock")
        String description,

        @Schema(description = "Rol base (CUSTOMER, VET, ADMIN): no se puede borrar", example = "false")
        boolean system,

        @Schema(description = "Ordenados por código")
        List<PermissionResponse> permissions,

        Instant createdAt,

        Instant updatedAt) {
}
