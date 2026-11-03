package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/** Ya existe una raza con el mismo nombre (sin distinguir mayúsculas) en la misma especie. */
public class BreedAlreadyExistsException extends ConflictException {

    public BreedAlreadyExistsException(String breedName, String speciesName) {
        super("Ya existe la raza '" + breedName + "' en la especie '" + speciesName + "'");
    }

}
