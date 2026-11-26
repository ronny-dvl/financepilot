package com.financepilot.backend.statement.categorization;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import static com.financepilot.backend.statement.categorization.CatalogFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategorizationCatalogLoaderTest {
    @Test
    void shouldLoadExactlyTheFourteenMigratedRules() {
        CompiledCatalog catalog = new CategorizationCatalogLoader(new ObjectMapper(), new DescriptionNormalizer()).load();
        assertEquals(14, catalog.rules().size());
        assertEquals(5, catalog.rules().stream().map(CompiledCatalog.CompiledRule::category).distinct().count());
        assertTrue(catalog.rules().stream().allMatch(rule -> rule.include().isEmpty() && rule.exclude().isEmpty()));
    }

    @Test
    void shouldNormalizeAliasesAndConditionsOnlyAtLoadTime() {
        DescriptionNormalizer normalizer = spy(new DescriptionNormalizer());
        String json = """
                {"version":1,"category":"SAUDE","rules":[{
                  "id":"saude.exemplo","aliases":["farmácia"],"priority":100,
                  "matchType":"WORD","specificity":"GENERIC",
                  "include":[{"matchType":"WORD","value":"débito"}],
                  "exclude":[{"matchType":"WORD","value":"estôrno"}]
                }]}
                """;
        CompiledCatalog catalog = new CategorizationCatalogLoader(new ObjectMapper(), normalizer).load(resource(json));
        verify(normalizer).normalize("farmácia");
        verify(normalizer).normalize("débito");
        verify(normalizer).normalize("estôrno");
        assertEquals("FARMACIA", catalog.rules().getFirst().aliases().getFirst().value());
        clearInvocations(normalizer);
        new RuleMatcher(catalog, normalizer).match("DEBITO FARMACIA");
        verify(normalizer).normalize("DEBITO FARMACIA");
        verifyNoMoreInteractions(normalizer);
        assertThrows(UnsupportedOperationException.class, () -> catalog.rules().clear());
        assertThrows(UnsupportedOperationException.class, () -> catalog.rules().getFirst().aliases().clear());
    }

    @Test
    void shouldRejectDuplicateIdsAcrossFiles() {
        assertThrows(IllegalArgumentException.class, () -> load(
                catalog("SAUDE", rule("same.id", "\"ALFA\"", "WORD", "SPECIFIC", 100)),
                catalog("COMPRAS", rule("same.id", "\"BETA\"", "WORD", "SPECIFIC", 100))));
    }

    @Test
    void shouldRejectConflictingAliasesAfterNormalization() {
        assertThrows(IllegalArgumentException.class, () -> load(
                catalog("SAUDE", rule("saude.a", "\"CAFÉ\"", "WORD", "SPECIFIC", 100)),
                catalog("COMPRAS", rule("compras.b", "\"cafe\"", "PHRASE", "SPECIFIC", 200))));
    }

    @Test
    void shouldRejectRedundantAliasesInsideOneRule() {
        assertThrows(IllegalArgumentException.class, () -> load(
                catalog("SAUDE", rule("saude.a", "\"CAFÉ\",\"CAFE\"", "WORD", "SPECIFIC", 100))));
    }

    @Test
    void shouldRejectInvalidRulesAndCatalogs() {
        List<String> invalidRules = List.of(
                rule("Bad Id", "\"UBER\"", "WORD", "SPECIFIC", 100),
                rule("test.a", "\"UBER\"", "WORD", "SPECIFIC", -1),
                rule("test.a", "\"---\"", "WORD", "SPECIFIC", 100),
                rule("test.a", "null", "WORD", "SPECIFIC", 100),
                rule("test.a", "", "WORD", "SPECIFIC", 100),
                rule("test.a", "\"DUAS PALAVRAS\"", "WORD", "SPECIFIC", 100),
                rule("test.a", "\"DUAS PALAVRAS\"", "PREFIX", "SPECIFIC", 100),
                rule("test.a", "\"UBER\"", "REGEX", "SPECIFIC", 100),
                rule("test.a", "\"UBER\"", "WORD", "UNKNOWN", 100),
                "{\"id\":\"test.a\",\"aliases\":[\"UBER\"]}", "null"
        );
        for (String invalid : invalidRules) {
            assertThrows(IllegalArgumentException.class, () -> load(catalog("SAUDE", invalid)));
        }
        assertThrows(IllegalArgumentException.class, () -> load(catalog("NEW_CATEGORY")));
        assertThrows(IllegalArgumentException.class, () -> load(catalog("SAUDE")));
        assertThrows(IllegalArgumentException.class, () -> load("{\"version\":2,\"category\":\"SAUDE\",\"rules\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> load("{\"version\":1,\"category\":\"SAUDE\",\"rules\":null}"));
        assertThrows(IllegalArgumentException.class, () -> load("not json"));
    }

    @Test
    void shouldRejectContradictoryConditionsAndUnknownConditionModes() {
        String rule = """
                {"id":"saude.a","aliases":["FARMA"],"priority":100,"matchType":"WORD","specificity":"GENERIC",
                 "include":[{"matchType":"WORD","value":"débito"}],
                 "exclude":[{"matchType":"PHRASE","value":"DEBITO"}]}
                """;
        assertThrows(IllegalArgumentException.class, () -> load(catalog("SAUDE", rule)));
        assertThrows(IllegalArgumentException.class, () -> load(catalog("SAUDE", rule.replace("PHRASE", "REGEX"))));
    }
}
