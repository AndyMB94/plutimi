package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.exception.BusinessException;

/** La raza existe, pero es de otra especie (por ejemplo, un gato de raza Labrador). */
public class BreedSpeciesMismatchException extends BusinessException {

    public BreedSpeciesMismatchException(String breedName, String speciesName) {
        super("La raza '" + breedName + "' no pertenece a la especie '" + speciesName + "'");
    }

}
