package dev.andymallcco.plutimi.shared.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Genera slugs para URLs amigables: minúsculas, sin tildes y con guiones.
 * Ejemplo: {@code "Alimento para Perros & Gatos"} → {@code "alimento-para-perros-gatos"}.
 */
public final class Slugs {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private Slugs() {
    }

    public static String of(String text) {
        Objects.requireNonNull(text, "text must not be null");
        // NFD separa cada letra de su tilde ("ñ" → "n" + "~"); luego se eliminan las marcas
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("");
        String dashed = NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("-");
        String slug = EDGE_DASHES.matcher(dashed).replaceAll("");
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("text has no letters or digits to build a slug: " + text);
        }
        return slug;
    }

}
