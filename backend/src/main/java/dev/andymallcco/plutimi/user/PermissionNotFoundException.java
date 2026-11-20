package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class PermissionNotFoundException extends ResourceNotFoundException {

    public PermissionNotFoundException(Long id) {
        super("No existe un permiso con id " + id);
    }

}
