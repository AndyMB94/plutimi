package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.exception.ConflictException;

/**
 * Ya existe una especie con el mismo código o con el mismo nombre (sin distinguir mayúsculas).
 * Se crea con los métodos de fábrica para que el mensaje explique cuál de los dos casos ocurrió.
 */
public class SpeciesAlreadyExistsException extends ConflictException {

    private SpeciesAlreadyExistsException(String message) {
        super(message);
    }

    public static SpeciesAlreadyExistsException withCode(String code) {
        return new SpeciesAlreadyExistsException("Ya existe una especie con el código '" + code + "'");
    }

    public static SpeciesAlreadyExistsException withName(String name) {
        return new SpeciesAlreadyExistsException("Ya existe una especie con el nombre '" + name + "'");
    }

}
