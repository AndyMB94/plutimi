package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.web.PageResponse;
import dev.andymallcco.plutimi.user.dto.ChangeUserRoleRequest;
import dev.andymallcco.plutimi.user.dto.ChangeUserStatusRequest;
import dev.andymallcco.plutimi.user.dto.UserFilter;
import dev.andymallcco.plutimi.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuarios", description = "Administración de usuarios: consulta, rol y estado")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Listar usuarios", description = "Paginado y ordenado por email. Filtros opcionales por rol y texto.")
    @GetMapping
    public PageResponse<UserResponse> list(
            @ParameterObject @Valid UserFilter filter,
            @ParameterObject @SortDefault(sort = "email") Pageable pageable) {
        return userService.list(filter, pageable);
    }

    @Operation(summary = "Obtener un usuario")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable @Positive Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "Cambiar el rol de un usuario",
            description = "No se puede quitar el rol ADMIN al último administrador activo.")
    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable @Positive Long id,
                                   @Valid @RequestBody ChangeUserRoleRequest request) {
        return userService.changeRole(id, request);
    }

    @Operation(summary = "Activar o desactivar un usuario",
            description = "Idempotente. No se puede desactivar al último administrador activo.")
    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable @Positive Long id,
                                     @Valid @RequestBody ChangeUserStatusRequest request) {
        return userService.changeStatus(id, request);
    }

}
