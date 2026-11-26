
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.dto.CategorizationResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionCategorizationServiceTest {

    private final TransactionCategorizationService service =
            new TransactionCategorizationService(new ObjectMapper());

    @Test
    void shouldPreserveAllFourteenMigratedRuleResults() {
        Map<String, TransactionCategory> cases = Map.ofEntries(
                Map.entry("MERCADO LIVRE", TransactionCategory.COMPRAS),
                Map.entry("UBER", TransactionCategory.TRANSPORTE),
                Map.entry("99APP", TransactionCategory.TRANSPORTE),
                Map.entry("NETFLIX", TransactionCategory.ASSINATURAS),
                Map.entry("SPOTIFY", TransactionCategory.ASSINATURAS),
                Map.entry("IFOOD", TransactionCategory.ALIMENTACAO),
                Map.entry("KEETA DELIVERY", TransactionCategory.ALIMENTACAO),
                Map.entry("99 FOOD", TransactionCategory.ALIMENTACAO),
                Map.entry("SUPERMERCADO", TransactionCategory.SUPERMERCADO),
                Map.entry("MERCADINHO", TransactionCategory.SUPERMERCADO),
                Map.entry("ATACADAO", TransactionCategory.SUPERMERCADO),
                Map.entry("FARMACIA", TransactionCategory.SAUDE),
                Map.entry("DROGARIA", TransactionCategory.SAUDE),
                Map.entry("FARMA", TransactionCategory.SAUDE)
        );
        cases.forEach((description, category) -> {
            CategorizationResult result = service.categorize("PIX ENVIADO - " + description);
            assertEquals(category, result.category());
            assertEquals(CategorizationSource.KEYWORD_RULE, result.source());
            assertFalse(result.requiresReview());
            assertNotNull(service.categorizeWithDetails(description).ruleId());
        });
    }

    @Test
    void shouldKeepSupermarketRuleIdentifiersStableAfterMigration() {
        Map.of("SUPERMERCADO", "alimentacao.supermercado",
                "MERCADINHO", "alimentacao.mercadinho",
                "ATACADÃO", "alimentacao.atacadao")
                .forEach((description, ruleId) -> {
                    var decision = service.categorizeWithDetails(description);
                    assertEquals(TransactionCategory.SUPERMERCADO, decision.result().category());
                    assertEquals(ruleId, decision.ruleId());
                });
    }

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
    void shouldCategorizeSupermarketSeparatelyFromFood() {

        CategorizationResult result =
                service.categorize("PIX QRS SUPERMERCADO");

        assertEquals(TransactionCategory.SUPERMERCADO, result.category());
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
    void shouldCategorizeMercadinhoAsSupermarket() {
        CategorizationResult result = service.categorize(
                "Pix enviado - Mercadinho Ilha Bela"
        );

        assertEquals(TransactionCategory.SUPERMERCADO, result.category());
        assertFalse(result.requiresReview());
    }

    @Test
    void shouldPrioritizeMercadoLivreAsShopping() {
        CategorizationResult result = service.categorize(
                "Pix enviado - Mercado Livre"
        );

        assertEquals(TransactionCategory.COMPRAS, result.category());
    }

    @Test
    void shouldNotMatchUberInsideUberaba() {
        CategorizationResult result = service.categorize(
                "Pix enviado - Uberaba Turismo"
        );

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
