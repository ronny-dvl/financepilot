package com.financepilot.backend.statement.categorization;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Metadata consistency is not proof of merchant identity or of an observed bank descriptor. */
class CurationEvidenceRegistryTest {
    private static final CompiledCatalog CATALOG = new CategorizationCatalogLoader(
            new ObjectMapper(), new DescriptionNormalizer()).load();

    @Test
    void shouldDescribeEveryActiveRuleAndPreserveRetirementHistory() throws Exception {
        var ledger = ledger();
        var records = rows(ledger.get("records"));
        Set<String> allIds = new HashSet<>();
        records.forEach(row -> assertTrue(allIds.add((String) row.get("id"))));
        var active = records.stream().filter(row -> "ACTIVE".equals(row.get("status")))
                .collect(Collectors.toMap(row -> (String) row.get("id"), Function.identity()));
        assertEquals(CATALOG.rules().stream().map(CompiledCatalog.CompiledRule::id).collect(Collectors.toSet()),
                active.keySet());
        for (var rule : CATALOG.rules()) {
            var row = active.get(rule.id());
            assertEquals(rule.category().name(), row.get("category"), rule.id());
            assertEquals(rule.aliases().stream().map(CompiledCatalog.NormalizedPattern::value)
                    .collect(Collectors.toSet()), rows(row.get("aliases")).stream()
                    .map(alias -> (String) alias.get("normalized")).collect(Collectors.toSet()), rule.id());
            assertFalse(((List<?>) row.get("decisionReasons")).isEmpty(), rule.id());
            if ("PREDOMINANT_ACTIVITY_DEFAULT".equals(row.get("runtimePolicy"))) {
                assertFalse(rule.conclusionRequired(), rule.id());
                assertTrue(rule.include().isEmpty(), rule.id());
                assertTrue(rule.includeAny().isEmpty(), rule.id());
            }
            if ("DISAMBIGUATING_CONTEXT".equals(row.get("runtimePolicy"))) {
                assertFalse(rule.include().isEmpty() && rule.includeAny().isEmpty(), rule.id());
            }
            if ("REVIEW_ONLY".equals(row.get("runtimePolicy"))) {
                assertTrue(rule.conclusionRequired(), rule.id());
                assertTrue(rule.concludeAny().isEmpty(), rule.id());
            }
        }
        for (var row : records) {
            if ("CONSOLIDATED".equals(row.get("status"))) {
                assertFalse(active.containsKey(row.get("id")));
                assertTrue(active.containsKey(row.get("consolidatedInto")));
            } else if ("REMOVED".equals(row.get("status"))) {
                assertFalse(active.containsKey(row.get("id")));
            }
        }
        var statistics = object(ledger.get("statistics"));
        assertEquals(CATALOG.rules().size(), ((Number) statistics.get("activeRules")).intValue());
        assertEquals(CATALOG.rules().stream().mapToInt(rule -> rule.aliases().size()).sum(),
                ((Number) statistics.get("aliases")).intValue());
        assertEquals(CATALOG.rules().stream().filter(rule -> rule.conclusionRequired()
                        && rule.concludeAny().isEmpty()).count(),
                ((Number) statistics.get("reviewOnly")).longValue());
    }

    @Test
    void shouldPreserveAllFourteenOriginalIdsWithPracticalMerchantDefaults() {
        var ids = CATALOG.rules().stream().map(CompiledCatalog.CompiledRule::id).collect(Collectors.toSet());
        assertTrue(ids.containsAll(Set.of("compras.mercado-livre", "transporte.uber", "transporte.99app",
                "assinaturas.netflix", "assinaturas.spotify", "alimentacao.ifood", "alimentacao.keeta-delivery",
                "alimentacao.99-food", "alimentacao.supermercado", "alimentacao.mercadinho",
                "alimentacao.atacadao", "saude.farmacia", "saude.drogaria", "saude.farma")));
    }

    @Test
    void shouldSeparatePartialOfficialIdentityEvidenceFromBankObservations() throws Exception {
        var ledger = ledger();
        assertEquals(false, ledger.get("fullyCommerciallyValidated"));
        assertEquals(false, ledger.get("allBankAliasesVerified"));
        var sources = rows(ledger.get("sources")).stream()
                .collect(Collectors.toMap(row -> (String) row.get("id"), Function.identity()));
        for (var row : rows(ledger.get("records"))) {
            var entity = object(row.get("commercialEntity"));
            if ("IDENTITY_ONLY_VERIFIED".equals(entity.get("status"))) {
                assertNotNull(entity.get("name"));
                assertNotNull(LocalDate.parse((String) entity.get("verifiedAt")));
                var sourceIds = (List<?>) entity.get("sourceIds");
                assertFalse(sourceIds.isEmpty());
                for (var id : sourceIds) {
                    var source = sources.get(id);
                    assertNotNull(source);
                    assertTrue(((String) source.get("url")).startsWith("https://"));
                    assertFalse(((String) source.get("scope")).isBlank());
                    assertEquals(false, source.get("bankAliasObserved"));
                }
            } else if ("PENDING".equals(entity.get("status"))) {
                assertNull(entity.get("verifiedAt"));
            }
            for (var alias : rows(row.get("aliases"))) {
                assertEquals("NOT_OBSERVED", alias.get("bankEvidence"));
            }
        }
    }

    @Test
    void shouldNotPromoteInferredAppleSpellingToOfficialBankEvidence() throws Exception {
        var apple = rows(ledger().get("records")).stream()
                .filter(row -> "assinaturas.apple-com-bill".equals(row.get("id"))).findFirst().orElseThrow();
        var aliases = rows(apple.get("aliases")).stream()
                .collect(Collectors.toMap(row -> (String) row.get("normalized"), Function.identity()));
        assertEquals(true, aliases.get("APPLE COM BILL").get("officialBillingDescriptor"));
        assertEquals(false, aliases.get("APPLECOMBILL").get("officialBillingDescriptor"));
        assertEquals("LEGACY_OR_SYNTHETIC_INFERENCE", aliases.get("APPLECOMBILL").get("nameEvidence"));
        assertEquals("NOT_OBSERVED", aliases.get("APPLE COM BILL").get("bankEvidence"));
    }

    private static Map<String, Object> ledger() throws Exception {
        Path base = Path.of(System.getProperty("basedir", System.getProperty("user.dir")));
        Path path = base.resolve("docs/categorization-curation.json");
        if (!Files.isRegularFile(path)) path = base.resolve("backend/docs/categorization-curation.json");
        return object(new ObjectMapper().readValue(Files.readString(path), Map.class));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    private static List<Map<String, Object>> rows(Object value) {
        return ((List<?>) value).stream().map(CurationEvidenceRegistryTest::object).toList();
    }
}
