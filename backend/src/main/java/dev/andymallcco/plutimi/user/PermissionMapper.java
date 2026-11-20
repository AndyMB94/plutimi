package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.user.dto.PermissionResponse;
import org.springframework.stereotype.Component;

@Component
class PermissionMapper {

    PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getDescription());
    }

}
