package dev.andymallcco.plutimi.shared.text;

import java.util.Locale;
import java.util.Objects;

/**
 * Patrones para búsquedas "contiene" con LIKE, compartidos por las Specifications de los módulos.
 * Uso: {@code cb.like(cb.lower(root.get("name")), LikePatterns.containing(text), LikePatterns.ESCAPE)}.
 */
public final class LikePatterns {

    /** Carácter de escape que hay que pasar a {@code cb.like(..., ESCAPE)}. */
    public static final char ESCAPE = '\\';

    private LikePatterns() {
    }

    /**
     * {@code %texto%} en minúsculas (para comparar con {@code lower(columna)}) y con los comodines
     * escapados, para que un "%" o "_" escrito por el usuario se busque literalmente.
     */
    public static String containing(String text) {
        Objects.requireNonNull(text, "text must not be null");
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

}
