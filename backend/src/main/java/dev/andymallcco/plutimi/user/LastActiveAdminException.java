package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/**
 * No se puede quitar el rol ADMIN ni desactivar al último administrador activo (409): nadie
 * podría volver a gestionar roles ni usuarios.
 */
public class LastActiveAdminException extends ConflictException {

    public LastActiveAdminException() {
        super("Es el último administrador activo: asigna el rol ADMIN a otro usuario antes");
    }

}
