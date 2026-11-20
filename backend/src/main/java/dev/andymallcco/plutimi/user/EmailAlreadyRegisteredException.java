package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("Ya existe una cuenta con el email '" + email + "'");
    }

}
