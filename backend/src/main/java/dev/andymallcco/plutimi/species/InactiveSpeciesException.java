package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.exception.BusinessException;

/** La operación necesita una especie activa: añadirle razas o asignarla a una mascota. */
public class InactiveSpeciesException extends BusinessException {

    public InactiveSpeciesException(String speciesName) {
        super("La especie '" + speciesName + "' está desactivada");
    }

}
