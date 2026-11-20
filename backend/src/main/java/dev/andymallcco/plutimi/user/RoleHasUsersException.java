package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/** No se puede borrar un rol que todavía tienen usuarios (409): primero hay que cambiarles el rol. */
public class RoleHasUsersException extends ConflictException {

    public RoleHasUsersException(String code) {
        super("El rol '" + code + "' tiene usuarios asignados: cámbiales el rol antes de borrarlo");
    }

}
