package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(Long id) {
        super("No existe un usuario con id " + id);
    }

}
