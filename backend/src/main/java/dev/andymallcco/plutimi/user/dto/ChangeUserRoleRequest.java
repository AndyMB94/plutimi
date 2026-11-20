package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChangeUserRoleRequest(

        @Schema(description = "Rol nuevo (ver GET /api/v1/roles)", example = "2")
        @NotNull(message = "El rol es obligatorio")
        @Positive(message = "El id del rol debe ser positivo")
        Long roleId) {
}
