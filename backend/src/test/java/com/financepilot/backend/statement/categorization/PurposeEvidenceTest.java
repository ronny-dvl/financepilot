package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.TransactionCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static com.financepilot.backend.statement.categorization.CatalogFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurposeEvidenceTest {
    private static final String STORE = """
            {"version":2,"category":"COMPRAS","rules":[{
              "id":"compras.store","aliases":["LOJA TESTE"],"priority":250,
              "matchType":"PHRASE","specificity":"SPECIFIC",
              "concludeAny":[[{"matchType":"WORD","value":"PRODUTO"}]]
            }]}
            """;

    @Test
    void shouldKeepIdentityEvidenceWithoutConcludingPurposeOrHidingConflicts() {
        var compiled = load(STORE, catalog("SAUDE", rule("saude.other", "\"CLINICA TESTE\"", "PHRASE", "SPECIFIC", 1)));
        assertTrue(decide(compiled, "COMPRA LOJA TESTE").result().requiresReview());
        assertEquals(TransactionCategory.COMPRAS, decide(compiled, "COMPRA LOJA TESTE PRODUTO").result().category());
        var conflict = decide(compiled, "COMPRA LOJA TESTE CLINICA TESTE");
        assertEquals(TransactionCategory.OUTROS, conflict.result().category());
        assertEquals(List.of("compras.store", "saude.other"), conflict.conflictingRuleIds());
        assertThrows(UnsupportedOperationException.class, () -> compiled.rules().getFirst().concludeAny().clear());
        assertThrows(UnsupportedOperationException.class, () -> compiled.rules().getFirst().concludeAny().getFirst().clear());
    }

    @Test
    void shouldDistinguishReviewOnlyFromLegacyRules() {
        var quarantine = load(STORE.replace("[[{\"matchType\":\"WORD\",\"value\":\"PRODUTO\"}]]", "[]"));
        assertTrue(decide(quarantine, "COMPRA LOJA TESTE PRODUTO").result().requiresReview());
        var legacy = load(catalog("COMPRAS", rule("legacy.store", "\"LOJA TESTE\"", "PHRASE", "SPECIFIC", 1)));
        assertFalse(decide(legacy, "LOJA TESTE").result().requiresReview());
    }

    @Test
    void shouldRequireEveryConditionInOnePurposeAlternativeAndKeepIdentityGuards() {
        String groups = "[[{\"matchType\":\"WORD\",\"value\":\"PRODUTO\"},"
                + "{\"matchType\":\"WORD\",\"value\":\"DIGITAL\"}],"
                + "[{\"matchType\":\"WORD\",\"value\":\"SERVICO\"}]]";
        String json = STORE.replace("[[{\"matchType\":\"WORD\",\"value\":\"PRODUTO\"}]]", groups)
                .replace("\"concludeAny\":", "\"includeAny\":[[{\"matchType\":\"STARTS_WITH\","
                        + "\"value\":\"COMPRA\"}]],\"concludeAny\":");
        var compiled = load(json);
        assertTrue(decide(compiled, "COMPRA LOJA TESTE PRODUTO").result().requiresReview());
        assertFalse(decide(compiled, "COMPRA LOJA TESTE PRODUTO DIGITAL").result().requiresReview());
        assertFalse(decide(compiled, "COMPRA LOJA TESTE SERVICO").result().requiresReview());
        assertTrue(new RuleMatcher(compiled, new DescriptionNormalizer())
                .match("PIX ENVIADO LOJA TESTE PRODUTO DIGITAL").isEmpty());
    }

    @Test
    void shouldRejectRedundantOrContradictoryPurposeConditions() {
        String condition = "{\"matchType\":\"WORD\",\"value\":\"PRODUTO\"}";
        String original = "[[" + condition + "]]";
        assertThrows(IllegalArgumentException.class, () -> load(STORE.replace(original,
                "[[" + condition + "," + condition + "]]")));
        assertThrows(IllegalArgumentException.class, () -> load(STORE.replace(original,
                "[[" + condition + "],[" + condition + "]]")));
        assertThrows(IllegalArgumentException.class, () -> load(STORE.replace("\"concludeAny\":",
                "\"exclude\":[" + condition + "],\"concludeAny\":")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[[]]", "[null]", "[[null]]", "{}", "[[{\"matchType\":\"WORD\",\"value\":\"---\"}]]"})
    void shouldRejectInvalidPurposeGroups(String groups) {
        String invalid = STORE.replace("[[{\"matchType\":\"WORD\",\"value\":\"PRODUTO\"}]]", groups);
        assertThrows(IllegalArgumentException.class, () -> load(invalid));
    }

    @Test
    void shouldValidateVersionAndNormalizePurposeOnlyOnLoad() {
        assertThrows(IllegalArgumentException.class, () -> load(STORE.replace("\"version\":2", "\"version\":1")));
        var normalizer = spy(new DescriptionNormalizer());
        var compiled = new CategorizationCatalogLoader(new ObjectMapper(), normalizer).load(resource(STORE));
        verify(normalizer).normalize("PRODUTO");
        clearInvocations(normalizer);
        new RuleMatcher(compiled, normalizer).match("COMPRA LOJA TESTE PRODUTO");
        verify(normalizer).normalize("COMPRA LOJA TESTE PRODUTO");
        verifyNoMoreInteractions(normalizer);
    }
}
