package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class RoleNotFoundException extends ResourceNotFoundException {

    public RoleNotFoundException(Long id) {
        super("No existe un rol con id " + id);
    }

}
