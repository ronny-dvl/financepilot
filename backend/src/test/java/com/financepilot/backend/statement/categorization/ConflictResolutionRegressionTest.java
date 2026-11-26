package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.service.TransactionCategorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static com.financepilot.backend.statement.categorization.CatalogFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ConflictResolutionRegressionTest {
    private static final TransactionCategorizationService SERVICE =
            new TransactionCategorizationService(new ObjectMapper());

    @ParameterizedTest
    @ValueSource(strings = {
            "COMPRA ASUS DROGASIL", "COMPRA DROGASIL ASUS", "COMPRA ASUS PETZ", "COMPRA PETZ ASUS",
            "COMPRA 99 FOOD NIKE", "COMPRA NIKE 99 FOOD", "COMPRA 99FOOD NIKE", "COMPRA NIKE 99FOOD",
            "COMPRA AMAZONMARKETPLACE AWS", "COMPRA AWS AMAZONMARKETPLACE",
            "COMPRA AMAZON MARKETPLACE AMAZONWEBSERVICES", "COMPRA AMAZONWEBSERVICES AMAZON MARKETPLACE",
            "COMPRA AMZN MKTP AWS", "COMPRA AWS AMZN MKTP", "COMPRA AMAZONCOMBR AMAZONWEBSERVICES",
            "COMPRA AMAZON MARKETPLACE AMAZON PRIME", "COMPRA PRIMEVIDEO AMZNMARKETPLACE",
            "COMPRA UBER EATS ASUS", "COMPRA ASUS UBEREATS", "COMPRA 99 APP 99 FOOD",
            "COMPRA 99APP 99FOOD", "COMPRA 99FOOD 99APP", "COMPRA UBER TRIP UBER EATS",
            "COMPRA UBER EATS UBER", "COMPRA UBER UBER EATS"
    })
    void shouldRequireReviewForIndependentSpecificEvidence(String description) {
        var decision = SERVICE.categorizeWithDetails(description);
        assertEquals(TransactionCategory.OUTROS, decision.result().category());
        assertEquals(CategorizationSource.UNKNOWN, decision.result().source());
        assertTrue(decision.result().requiresReview());
        assertNull(decision.ruleId());
        assertTrue(decision.conflictingRuleIds().size() >= 2);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            UBER EATS | ALIMENTACAO
            UBEREATS | ALIMENTACAO
            UBER TRIP | TRANSPORTE
            UBERTRIP | TRANSPORTE
            99 FOOD | ALIMENTACAO
            99FOOD | ALIMENTACAO
            COMPRA 99 APP | TRANSPORTE
            COMPRA 99APP | TRANSPORTE
            AMAZON WEB SERVICES | TECNOLOGIA
            AMAZONWEBSERVICES | TECNOLOGIA
            COMPRA AWS | TECNOLOGIA
            AWS | TECNOLOGIA
            AMZN MKTP | COMPRAS
            AMAZONMARKETPLACE | COMPRAS
            PIX RENNER | VESTUARIO
            PIX ENVIADO HIROTA FOOD | SUPERMERCADO
            COMPRAZ RENNER | VESTUARIO
            PIX QRSX RENNER | VESTUARIO
            AMAZON PRIME | ASSINATURAS
            AMAZONPRIME | ASSINATURAS
            UBER ONE | ASSINATURAS
            UBERONE | ASSINATURAS
            RSCSS RENNER | VESTUARIO
            COMPRA RENNER | VESTUARIO
            PIX QRS RENNER | VESTUARIO
            PIX QRS HIROTA FOOD | SUPERMERCADO
            RSCSS HIROTA | SUPERMERCADO
            SUPER DUOLINGO | ASSINATURAS
            DUOLINGO PLUS | ASSINATURAS
            COMPRA CASA RIACHUELO | CASA_UTILIDADES
            NETFLIX GAMES | GAMES
            COPEL TELECOM | INTERNET_TELEFONIA
            PLANO DE SAUDE PET | PETS
            """)
    // Merchant defaults need no purchase marker; contextual refinements still resolve related services.
    void shouldRecognizeAliasesAndDeclaredRefinements(String description, TransactionCategory expected) {
        var result = SERVICE.categorize(description);
        assertEquals(expected, result.category());
        assertEquals(CategorizationSource.KEYWORD_RULE, result.source());
        assertFalse(result.requiresReview());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIX RECEBIDO CARLOS RENNER",
            "PIX ENVIADO COMPRA RENNER", "PIX RECEBIDO RSCSS RENNER", "UBERABA", "UBERLANDIA SHOPPING"})
    void shouldPreservePersonalNameGuardsAndTokenBoundaries(String description) {
        var result = SERVICE.categorize(description);
        assertEquals(TransactionCategory.OUTROS, result.category());
        assertEquals(CategorizationSource.UNKNOWN, result.source());
        assertTrue(result.requiresReview());
    }

    @Test
    void shouldKeepAllIndependentRuleIdentifiersDespitePriorityAndLengthDifferences() {
        for (int priority : List.of(0, 10, 100, 10000)) {
            String first = catalog("COMPRAS", rule("compras.a", "\"A\"", "WORD", "SPECIFIC", priority));
            String second = catalog("SAUDE", rule("saude.long", "\"MUITO MAIS LONGO\"", "PHRASE", "SPECIFIC", 500));
            var forward = decide(load(first, second), "A MUITO MAIS LONGO");
            var reverse = decide(load(second, first), "MUITO MAIS LONGO A");
            assertEquals(forward, reverse);
            assertEquals(TransactionCategory.OUTROS, forward.result().category());
            assertEquals(List.of("compras.a", "saude.long"), forward.conflictingRuleIds());
        }
    }

    @Test
    void shouldPreserveExcludedIndependentEvidenceInDiagnostics() {
        String excluded = catalog("COMPRAS", """
                {"id":"compras.a","aliases":["ALFA"],"priority":1,"matchType":"WORD","specificity":"SPECIFIC",
                 "exclude":[{"matchType":"WORD","value":"BETA"}]}
                """);
        String other = catalog("SAUDE", rule("saude.b", "\"BETA\"", "WORD", "SPECIFIC", 999));
        var decision = decide(load(excluded, other), "ALFA BETA");
        assertTrue(decision.result().requiresReview());
        assertEquals(Set.of("compras.a", "saude.b"), Set.copyOf(decision.conflictingRuleIds()));
    }

    @Test
    void shouldNotLetAGenericLiteralEraseSpecificEvidence() {
        var decision = decide(load(
                catalog("COMPRAS", rule("compras.generic", "\"FARMA\"", "EXACT", "GENERIC", 999)),
                catalog("SAUDE", rule("saude.specific", "\"FARM\"", "PREFIX", "SPECIFIC", 1))), "FARMA");
        assertEquals(TransactionCategory.SAUDE, decision.result().category());
    }

    @Test
    void shouldRequireReviewForCompetingGenericPurposes() {
        var decision = decide(load(
                catalog("COMPRAS", rule("compras.generic", "\"LOJA\"", "WORD", "GENERIC", 999)),
                catalog("SAUDE", rule("saude.generic", "\"CLINICA\"", "WORD", "GENERIC", 1))), "LOJA CLINICA");
        assertEquals(TransactionCategory.OUTROS, decision.result().category());
        assertTrue(decision.result().requiresReview());
    }

    @Test
    void shouldNotUseUndeclaredPhraseLengthAsSemanticRefinement() {
        var decision = decide(load(
                catalog("COMPRAS", rule("compras.full", "\"LOJA ALFA\"", "PHRASE", "SPECIFIC", 999)),
                catalog("SAUDE", rule("saude.short", "\"ALFA\"", "WORD", "SPECIFIC", 1))), "LOJA ALFA");
        assertTrue(decision.result().requiresReview());
    }
}
