package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.ResourceNotFoundException;

public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(Long id) {
        super("No existe un producto con id " + id);
    }

}
