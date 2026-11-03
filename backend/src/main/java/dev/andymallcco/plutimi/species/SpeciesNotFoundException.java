package dev.andymallcco.plutimi.species;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class SpeciesNotFoundException extends ResourceNotFoundException {

    public SpeciesNotFoundException(Long id) {
        super("No existe una especie con id " + id);
    }

}
