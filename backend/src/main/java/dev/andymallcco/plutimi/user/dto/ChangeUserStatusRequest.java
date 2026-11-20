package dev.andymallcco.plutimi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(

        // Boolean y no boolean: con el primitivo, un JSON sin el campo llegaría como false
        // y desactivaría al usuario sin querer
        @Schema(description = "true activa la cuenta y false la desactiva", example = "false")
        @NotNull(message = "El estado es obligatorio")
        Boolean enabled) {
}
