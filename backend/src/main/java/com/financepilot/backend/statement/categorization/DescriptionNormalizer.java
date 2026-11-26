package com.financepilot.backend.statement.categorization;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DescriptionNormalizer {
    private static final Pattern MARKS = Pattern.compile("\\p{M}");
    private static final Pattern SEPARATORS = Pattern.compile("[^A-Z0-9]+");

    public String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String unaccented = MARKS.matcher(decomposed).replaceAll("").toUpperCase(Locale.ROOT);
        return SEPARATORS.matcher(unaccented).replaceAll(" ").trim();
    }
}
