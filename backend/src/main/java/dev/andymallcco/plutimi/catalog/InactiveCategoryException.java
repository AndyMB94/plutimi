package dev.andymallcco.plutimi.catalog;

import dev.andymallcco.plutimi.shared.exception.BusinessException;

/** Un producto activo necesita una categoría activa (422). */
public class InactiveCategoryException extends BusinessException {

    public InactiveCategoryException(String categoryName) {
        super("La categoría '" + categoryName + "' está desactivada");
    }

}
