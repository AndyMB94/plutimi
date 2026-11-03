package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.exception.BusinessException;

/** La operación necesita una raza activa, por ejemplo asignarla a una mascota. */
public class InactiveBreedException extends BusinessException {

    public InactiveBreedException(String breedName) {
        super("La raza '" + breedName + "' está desactivada");
    }

}
