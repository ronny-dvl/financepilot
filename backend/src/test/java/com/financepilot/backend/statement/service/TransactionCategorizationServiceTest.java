
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.dto.CategorizationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionCategorizationServiceTest {

    private final TransactionCategorizationService service =
            new TransactionCategorizationService();

    @Test
    void shouldCategorizeUberAsTransport() {

        CategorizationResult result =
                service.categorize("UBER TRIP");

        assertEquals(TransactionCategory.TRANSPORTE, result.category());
        assertEquals(CategorizationSource.KEYWORD_RULE, result.source());
        assertFalse(result.requiresReview());
    }

    @Test
    void shouldCategorizeNetflixAsSubscription() {

        CategorizationResult result =
                service.categorize("NETFLIX");

        assertEquals(TransactionCategory.ASSINATURAS, result.category());
        assertFalse(result.requiresReview());
    }

    @Test
    void shouldCategorizeSupermarketAsFood() {

        CategorizationResult result =
                service.categorize("PIX QRS SUPERMERCADO");

        assertEquals(TransactionCategory.ALIMENTACAO, result.category());
    }

    @Test
    void shouldNormalizeAccents() {

        CategorizationResult result =
                service.categorize("Farmácia São João");

        assertEquals(TransactionCategory.SAUDE, result.category());
    }

    @Test
    void shouldReturnUnknownForUnrecognizedMerchant() {

        CategorizationResult result =
                service.categorize("HYPERBENEFICIOS");

        assertEquals(TransactionCategory.OUTROS, result.category());
        assertEquals(CategorizationSource.UNKNOWN, result.source());
        assertTrue(result.requiresReview());
    }

    @Test
    void shouldHandleNullDescription() {

        CategorizationResult result =
                service.categorize(null);

        assertEquals(TransactionCategory.OUTROS, result.category());
        assertTrue(result.requiresReview());
    }

    @Test
    void shouldHandleBlankDescription() {

        CategorizationResult result =
                service.categorize("   ");

        assertEquals(TransactionCategory.OUTROS, result.category());
        assertTrue(result.requiresReview());
    }

    @Test
    void shouldNotCategorizePartialKeywordMatch() {

        CategorizationResult result =
                service.categorize("UBERLANDIA SHOPPING");

        assertEquals(TransactionCategory.OUTROS, result.category());
        assertEquals(CategorizationSource.UNKNOWN, result.source());
        assertTrue(result.requiresReview());
    }
}
