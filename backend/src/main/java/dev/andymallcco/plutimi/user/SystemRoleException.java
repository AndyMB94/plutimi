package dev.andymallcco.plutimi.user;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/**
 * Los roles de sistema (CUSTOMER, VET, ADMIN) no se pueden borrar (409): el código depende de
 * ellos, por ejemplo el registro asigna CUSTOMER.
 */
public class SystemRoleException extends ConflictException {

    public SystemRoleException(String code) {
        super("El rol '" + code + "' es un rol del sistema y no se puede borrar");
    }

}
