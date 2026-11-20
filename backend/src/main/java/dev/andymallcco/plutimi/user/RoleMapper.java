package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.user.dto.RoleResponse;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RoleMapper {

    private final PermissionMapper permissionMapper;

    /** Toca la colección de permisos: llamarlo dentro de la transacción del service. */
    RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                // El Set no tiene orden: se ordena para que la respuesta sea estable
                role.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getCode))
                        .map(permissionMapper::toResponse)
                        .toList(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }

}
