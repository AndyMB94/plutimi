package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Los permisos se asignan después, con {@code PUT /roles/{id}/permissions}. */
public record CreateRoleRequest(

        @Schema(description = "Código único e inmutable; se guarda en mayúsculas", example = "STOCK_MANAGER")
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código no puede superar los 30 caracteres")
        @Pattern(regexp = RoleValidation.CODE_FORMAT,
                message = "El código debe empezar por una letra y contener solo letras, números y guiones bajos")
        String code,

        @Schema(example = "Encargado de almacén")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
        String name,

        @Schema(example = "Gestiona el catálogo y el stock")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description) {
}
