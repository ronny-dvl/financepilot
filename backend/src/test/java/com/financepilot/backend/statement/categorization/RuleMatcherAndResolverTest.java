package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.financepilot.backend.statement.categorization.CatalogFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class RuleMatcherAndResolverTest {
    @Test
    void shouldSupportMoreThanFiveHundredIndexedRules() {
        String[] rules = java.util.stream.IntStream.range(0, 600)
                .mapToObj(index -> rule("compras.rule-" + index, "\"LOJA" + index + "\"",
                        "WORD", "SPECIFIC", 100)).toArray(String[]::new);
        CompiledCatalog catalog = load(catalog("COMPRAS", rules));
        assertEquals(600, catalog.rules().size());
        var matches = new RuleMatcher(catalog, new DescriptionNormalizer()).match("PIX LOJA599");
        assertEquals(1, matches.size());
        assertEquals("compras.rule-599", new CategoryResolver().resolve(matches).ruleId());
    }

    @Test
    void shouldMatchAliasesWithStableRuleId() {
        CompiledCatalog catalog = load(catalog("COMPRAS",
                rule("compras.acme", "\"ACME\",\"ACME ONLINE\"", "PHRASE", "SPECIFIC", 100)));
        for (String description : List.of("PIX ACME", "Compra Acme-Online")) {
            CategorizationDecision decision = decide(catalog, description);
            assertEquals(TransactionCategory.COMPRAS, decision.result().category());
            assertEquals("compras.acme", decision.ruleId());
            assertEquals(CategorizationSource.KEYWORD_RULE, decision.result().source());
            assertFalse(decision.result().requiresReview());
        }
        assertTrue(decide(catalog, "ACMEX").result().requiresReview());
    }

    @Test
    void shouldRequireAllInclusionsAndVetoAnyExclusion() {
        String conditional = """
                {"id":"compras.loja","aliases":["LOJA"],"priority":100,"matchType":"WORD","specificity":"GENERIC",
                 "include":[{"matchType":"WORD","value":"débito"},{"matchType":"WORD","value":"CARTAO"}],
                 "exclude":[{"matchType":"WORD","value":"ESTORNO"},{"matchType":"WORD","value":"DEVOLUCAO"}]}
                """;
        CompiledCatalog catalog = load(catalog("COMPRAS", conditional));
        assertEquals(TransactionCategory.COMPRAS, decide(catalog, "DEBITO CARTAO LOJA").result().category());
        for (String description : List.of("DEBITO LOJA", "CARTAO LOJA", "LOJA",
                "DEBITO CARTAO LOJA ESTORNO", "DEBITO CARTAO LOJA DEVOLUÇÃO")) {
            assertTrue(decide(catalog, description).result().requiresReview());
        }
    }

    @Test
    void shouldUseTokenBoundariesInsteadOfSubstringMatches() {
        CompiledCatalog catalog = load(catalog("TRANSPORTE",
                rule("transporte.uber", "\"UBER\"", "WORD", "SPECIFIC", 100)));
        assertEquals(TransactionCategory.TRANSPORTE, decide(catalog, "PIX UBER TRIP").result().category());
        for (String description : List.of("UBERABA", "UBERLANDIA SHOPPING", "SUPERUBER")) {
            assertEquals(TransactionCategory.OUTROS, decide(catalog, description).result().category());
        }
    }

    @Test
    void shouldApplyPrefixesOnlyWhenExplicitlyConfigured() {
        CompiledCatalog word = load(catalog("SAUDE", rule("saude.farm", "\"FARM\"", "WORD", "GENERIC", 100)));
        CompiledCatalog prefix = load(catalog("SAUDE", rule("saude.farm", "\"FARM\"", "PREFIX", "GENERIC", 100)));
        assertEquals(TransactionCategory.OUTROS, decide(word, "FARMACIA").result().category());
        assertEquals(TransactionCategory.SAUDE, decide(prefix, "PIX FARMACIA").result().category());
        assertEquals(TransactionCategory.OUTROS, decide(prefix, "SUPERFARMA").result().category());
    }

    @Test
    void shouldRequireFullDescriptionForExactMode() {
        CompiledCatalog catalog = load(catalog("COMPRAS",
                rule("compras.exact", "\"LOJA EXEMPLO\"", "EXACT", "SPECIFIC", 100)));
        assertEquals(TransactionCategory.COMPRAS, decide(catalog, " loja-exemplo ").result().category());
        assertEquals(TransactionCategory.OUTROS, decide(catalog, "PIX LOJA EXEMPLO").result().category());
    }

    @Test
    void shouldPreferSpecificMerchantOverHigherPriorityGenericRule() {
        CompiledCatalog catalog = load(
                catalog("COMPRAS", rule("compras.acme", "\"ACME\"", "WORD", "SPECIFIC", 10)),
                catalog("ALIMENTACAO", rule("alimentacao.loja", "\"LOJA\"", "WORD", "GENERIC", 999)));
        assertEquals("compras.acme", decide(catalog, "LOJA ACME").ruleId());
    }

    @Test
    void shouldApplyPriorityWithinTheSameSpecificity() {
        CompiledCatalog catalog = load(
                catalog("COMPRAS", rule("compras.alfa", "\"ALFA\"", "WORD", "SPECIFIC", 200)),
                catalog("SAUDE", rule("saude.longa", "\"ESTABELECIMENTO\"", "WORD", "SPECIFIC", 100)));
        assertEquals("compras.alfa", decide(catalog, "ALFA ESTABELECIMENTO").ruleId());
    }

    @Test
    void shouldPreferLongerPhraseWhenScopeAndPriorityAreEqual() {
        CompiledCatalog catalog = load(
                catalog("COMPRAS", rule("compras.alfa", "\"LOJA ALFA\"", "PHRASE", "SPECIFIC", 100)),
                catalog("SAUDE", rule("saude.alfa", "\"ALFA\"", "WORD", "SPECIFIC", 100)));
        assertEquals("compras.alfa", decide(catalog, "PIX LOJA ALFA").ruleId());
    }

    @Test
    void shouldPreferExactOverPrefixAtEqualPriorityAndScope() {
        CompiledCatalog catalog = load(
                catalog("COMPRAS", rule("compras.exact", "\"FARMA\"", "EXACT", "SPECIFIC", 100)),
                catalog("SAUDE", rule("saude.prefix", "\"FARM\"", "PREFIX", "SPECIFIC", 100)));
        assertEquals("compras.exact", decide(catalog, "FARMA").ruleId());
    }

    @Test
    void shouldRequireReviewForUnresolvedCategoryConflictRegardlessOfFileOrder() {
        String health = catalog("SAUDE", rule("saude.alfa", "\"ALFA\"", "WORD", "SPECIFIC", 100));
        String shopping = catalog("COMPRAS", rule("compras.beta", "\"BETA\"", "WORD", "SPECIFIC", 100));
        CategorizationDecision decision = decide(load(health, shopping), "PIX ALFA BETA");
        assertEquals(TransactionCategory.OUTROS, decision.result().category());
        assertEquals(CategorizationSource.UNKNOWN, decision.result().source());
        assertTrue(decision.result().requiresReview());
        assertNull(decision.ruleId());
        assertEquals(List.of("compras.beta", "saude.alfa"), decision.conflictingRuleIds());
        assertEquals(decision, decide(load(shopping, health), "PIX ALFA BETA"));
    }

    @Test
    void shouldUseStableIdForEqualRankMatchesInTheSameCategory() {
        String a = rule("saude.a", "\"ALFA\"", "WORD", "SPECIFIC", 100);
        String z = rule("saude.z", "\"BETA\"", "WORD", "SPECIFIC", 100);
        CategorizationDecision decision = decide(load(catalog("SAUDE", z, a)), "ALFA BETA");
        assertEquals("saude.a", decision.ruleId());
        assertFalse(decision.result().requiresReview());
        assertEquals(decision, decide(load(catalog("SAUDE", a, z)), "ALFA BETA"));
    }

    @Test
    void shouldKeepUnknownFallbackWithoutExposingDescriptionsInDiagnostics() {
        CompiledCatalog catalog = load(catalog("SAUDE", rule("saude.a", "\"FARMA\"", "WORD", "GENERIC", 100)));
        for (String description : new String[] { null, " ", "---", "NOME FINANCEIRO DESCONHECIDO" }) {
            CategorizationDecision decision = decide(catalog, description);
            assertEquals(TransactionCategory.OUTROS, decision.result().category());
            assertEquals(CategorizationSource.UNKNOWN, decision.result().source());
            assertTrue(decision.result().requiresReview());
            assertNull(decision.ruleId());
            assertTrue(decision.conflictingRuleIds().isEmpty());
            assertFalse(decision.toString().contains("NOME FINANCEIRO"));
        }
    }
}
