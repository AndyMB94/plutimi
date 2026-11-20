package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class BrandNotFoundException extends ResourceNotFoundException {

    public BrandNotFoundException(Long id) {
        super("No existe una marca con id " + id);
    }

}
