package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.user.dto.RoleSummaryResponse;
import dev.andymallcco.plutimi.user.dto.UserProfileResponse;
import dev.andymallcco.plutimi.user.dto.UserResponse;
import org.springframework.stereotype.Component;

/** Toca el rol (LAZY): llamarlo dentro de la transacción del service. */
@Component
class UserMapper {

    UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                toRoleSummary(user.getRole()),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                toRoleSummary(user.getRole()),
                user.getRole().getPermissions().stream()
                        .map(Permission::getCode)
                        .sorted()
                        .toList());
    }

    private RoleSummaryResponse toRoleSummary(Role role) {
        return new RoleSummaryResponse(role.getId(), role.getCode(), role.getName());
    }

}
