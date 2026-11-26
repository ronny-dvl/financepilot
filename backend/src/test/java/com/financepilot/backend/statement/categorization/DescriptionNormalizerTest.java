package com.financepilot.backend.statement.categorization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DescriptionNormalizerTest {
    private final DescriptionNormalizer normalizer = new DescriptionNormalizer();

    @Test
    void shouldNormalizeAccentsCasePunctuationAndWhitespace() {
        assertEquals("PIX FARMACIA SAO JOAO 123", normalizer.normalize("  Pix - Farmácia\tSão João*123  "));
    }

    @Test
    void shouldKeepTokenBoundariesAndDigits() {
        assertEquals("99 APP", normalizer.normalize("99.APP"));
        assertEquals("99APP", normalizer.normalize("99app"));
        assertEquals("MERCADO LIVRE", normalizer.normalize("Mercado--Livre"));
        assertEquals("UBERABA", normalizer.normalize("Uberaba"));
    }

    @Test
    void shouldHandleMissingAndNonAlphanumericText() {
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize(" \t\n"));
        assertEquals("", normalizer.normalize("***---"));
    }

    @Test
    void shouldBeIdempotent() {
        String normalized = normalizer.normalize("Débito - Farmácia!");
        assertEquals(normalized, normalizer.normalize(normalized));
    }
}
