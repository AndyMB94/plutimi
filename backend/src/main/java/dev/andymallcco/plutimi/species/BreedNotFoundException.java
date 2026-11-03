package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class BreedNotFoundException extends ResourceNotFoundException {

    public BreedNotFoundException(Long id) {
        super("No existe una raza con id " + id);
    }

}
