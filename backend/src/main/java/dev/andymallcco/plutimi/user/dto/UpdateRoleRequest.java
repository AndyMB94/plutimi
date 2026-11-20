package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El código no aparece: no cambia nunca. */
public record UpdateRoleRequest(

        @Schema(example = "Encargado de almacén")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
        String name,

        @Schema(example = "Gestiona el catálogo y el stock")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String description) {
}
