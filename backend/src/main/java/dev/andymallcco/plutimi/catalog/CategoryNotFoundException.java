package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(Long id) {
        super("No existe una categoría con id " + id);
    }

}
