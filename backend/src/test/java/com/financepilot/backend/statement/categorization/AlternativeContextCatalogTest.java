package com.financepilot.backend.statement.categorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import static com.financepilot.backend.statement.categorization.CatalogFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlternativeContextCatalogTest {
    private static final String CONDITIONAL = """
            {"id":"compras.guard","aliases":["MARCA"],"priority":100,"matchType":"WORD","specificity":"SPECIFIC",
             "include":[{"matchType":"WORD","value":"CARTAO"}],
             "includeAny":[[{"matchType":"STARTS_WITH","value":"COMPRA"},{"matchType":"WORD","value":"DEBITO"}],
                           [{"matchType":"STARTS_WITH","value":"RSCSS"}]],
             "exclude":[{"matchType":"WORD","value":"BLOQUEADO"}]}
            """;

    @Test
    void shouldRequireAllMandatoryConditionsOneCompleteAlternativeAndNoVeto() {
        var compiled = load(v2(catalog("COMPRAS", CONDITIONAL)));
        assertFalse(decide(compiled, "COMPRA DEBITO CARTAO MARCA").result().requiresReview());
        assertFalse(decide(compiled, "RSCSS CARTAO MARCA").result().requiresReview());
        for (String description : new String[]{"COMPRA CARTAO MARCA", "COMPRA DEBITO MARCA",
                "PIX ENVIADO COMPRA DEBITO CARTAO MARCA", "RSCSSX CARTAO MARCA",
                "RSCSS CARTAO MARCA BLOQUEADO"}) {
            assertTrue(decide(compiled, description).result().requiresReview());
        }
        assertThrows(UnsupportedOperationException.class, () -> compiled.rules().getFirst().includeAny().clear());
        assertThrows(UnsupportedOperationException.class, () -> compiled.rules().getFirst().includeAny().getFirst().clear());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "\"COMPRA\"", "[{}]", "[[]]", "[null]", "[[null]]", "[[{\"matchType\":\"INVALID\",\"value\":\"COMPRA\"}]]",
            "[[{\"matchType\":\"WORD\",\"value\":\"---\"}]]",
            "[[{\"matchType\":\"WORD\",\"value\":\"café\"},{\"matchType\":\"PHRASE\",\"value\":\"CAFE\"}]]",
            "[[{\"matchType\":\"WORD\",\"value\":\"COMPRA\"}],[{\"matchType\":\"PHRASE\",\"value\":\"compra\"}]]"})
    void shouldRejectInvalidAlternativeGroups(String groups) {
        String base = rule("compras.invalid", "\"MARCA\"", "WORD", "SPECIFIC", 100);
        String extended = base.substring(0, base.length() - 1) + ",\"includeAny\":" + groups + "}";
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS", extended))));
    }

    @Test
    void shouldValidateSchemaVersionsAndKeepLegacyRulesCompatible() {
        assertFalse(decide(load(catalog("COMPRAS", rule("legacy.a", "\"MARCA\"", "WORD", "SPECIFIC", 1))),
                "MARCA").result().requiresReview());
        assertThrows(IllegalArgumentException.class, () -> load(catalog("COMPRAS", CONDITIONAL)));
        assertThrows(IllegalArgumentException.class, () -> load(catalog("COMPRAS", """
                {"id":"legacy.anchor","aliases":["MARCA"],"priority":1,"matchType":"WORD","specificity":"SPECIFIC",
                 "include":[{"matchType":"STARTS_WITH","value":"COMPRA"}]}
                """)));
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS",
                rule("invalid.mode", "\"MARCA\"", "STARTS_WITH", "SPECIFIC", 1)))));
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS", CONDITIONAL))
                .replace("\"version\":2", "\"version\":3")));
    }

    @Test
    void shouldValidateRefinementReferencesAndCycles() {
        String a = refinement("compras.a", "ALFA", "compras.b");
        String b = refinement("compras.b", "BETA", "compras.a");
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS", a))));
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS", a, b))));
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS",
                refinement("compras.a", "ALFA", "compras.a")))));
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS",
                a.replace("[\"compras.b\"]", "[\"compras.b\",\"compras.b\"]"),
                rule("compras.b", "\"BETA\"", "WORD", "SPECIFIC", 1)))));
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS",
                a.replace("\"specificity\":\"SPECIFIC\"", "\"specificity\":\"GENERIC\""),
                rule("compras.b", "\"BETA\"", "WORD", "SPECIFIC", 1)))));
    }

    @Test
    void shouldRejectAnAlternativeContradictingAnExclusion() {
        assertThrows(IllegalArgumentException.class, () -> load(v2(catalog("COMPRAS",
                CONDITIONAL.replace("\"value\":\"BLOQUEADO\"", "\"value\":\"DEBITO\"")))));
    }

    @Test
    void shouldCompileTransitiveRefinementsAndLimitThemToTheSameOccurrence() {
        var compiled = load(v2(catalog("COMPRAS", refinement("compras.full", "LOJA ALFA", "compras.parent"),
                refinement("compras.child", "SUPER LOJA ALFA", "compras.full"))),
                catalog("SAUDE", rule("compras.parent", "\"ALFA\"", "WORD", "SPECIFIC", 999)));
        assertEquals("compras.child", decide(compiled, "SUPER LOJA ALFA").ruleId());
        // The separate ALFA occurrence is independent evidence and cannot be swallowed by the first phrase.
        assertTrue(decide(compiled, "SUPER LOJA ALFA ALFA").result().requiresReview());
    }

    @Test
    void shouldNormalizeNewConditionsOnlyAtLoadTime() {
        var normalizer = spy(new DescriptionNormalizer());
        var compiled = new CategorizationCatalogLoader(new ObjectMapper(), normalizer)
                .load(resource(v2(catalog("COMPRAS", CONDITIONAL))));
        verify(normalizer).normalize("RSCSS");
        clearInvocations(normalizer);
        new RuleMatcher(compiled, normalizer).match("RSCSS CARTAO MARCA");
        verify(normalizer).normalize("RSCSS CARTAO MARCA");
        verifyNoMoreInteractions(normalizer);
    }

    private static String v2(String json) { return json.replace("\"version\":1", "\"version\":2"); }

    private static String refinement(String id, String alias, String parent) {
        String base = rule(id, "\"" + alias + "\"", "PHRASE", "SPECIFIC", 1);
        return base.substring(0, base.length() - 1) + ",\"refines\":[\"" + parent + "\"]}";
    }
}
