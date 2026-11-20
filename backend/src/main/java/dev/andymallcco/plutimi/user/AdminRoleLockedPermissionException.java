package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ConflictException;
import java.util.Set;

/**
 * El rol ADMIN no puede perder {@code role:manage} ni {@code user:manage} (409): si los perdiera,
 * nadie podría volver a gestionar roles ni usuarios.
 */
public class AdminRoleLockedPermissionException extends ConflictException {

    public AdminRoleLockedPermissionException(Set<String> missingCodes) {
        super("El rol ADMIN debe conservar los permisos " + String.join(", ", missingCodes));
    }

}
