package com.financepilot.backend.statement.categorization;

import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

final class CatalogFixtures {
    private CatalogFixtures() { }

    static ByteArrayResource resource(String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
    }

    static String catalog(String category, String... rules) {
        return "{\"version\":1,\"category\":\"" + category + "\",\"rules\":[" + String.join(",", rules) + "]}";
    }

    static String rule(String id, String aliases, String mode, String specificity, int priority) {
        return "{\"id\":\"" + id + "\",\"aliases\":[" + aliases + "],\"priority\":" + priority
                + ",\"matchType\":\"" + mode + "\",\"specificity\":\"" + specificity + "\"}";
    }

    static CompiledCatalog load(String... catalogs) {
        return new CategorizationCatalogLoader(new ObjectMapper(), new DescriptionNormalizer())
                .load(java.util.Arrays.stream(catalogs).map(CatalogFixtures::resource).toArray(ByteArrayResource[]::new));
    }

    static CategorizationDecision decide(CompiledCatalog catalog, String description) {
        return new CategoryResolver().resolve(new RuleMatcher(catalog, new DescriptionNormalizer()).match(description));
    }
}
