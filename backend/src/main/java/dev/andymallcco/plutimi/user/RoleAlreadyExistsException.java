package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

public class RoleAlreadyExistsException extends ConflictException {

    public RoleAlreadyExistsException(String code) {
        super("Ya existe un rol con el código '" + code + "'");
    }

}
