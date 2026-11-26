package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.categorization.CategorizationCatalogLoader;
import com.financepilot.backend.statement.categorization.CategorizationDecision;
import com.financepilot.backend.statement.categorization.CategoryResolver;
import com.financepilot.backend.statement.categorization.DescriptionNormalizer;
import com.financepilot.backend.statement.categorization.RuleMatcher;
import com.financepilot.backend.statement.dto.CategorizationResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class TransactionCategorizationService {
    private final RuleMatcher matcher;
    private final CategoryResolver resolver;

    public TransactionCategorizationService(ObjectMapper objectMapper) {
        DescriptionNormalizer normalizer = new DescriptionNormalizer();
        var catalog = new CategorizationCatalogLoader(objectMapper, normalizer).load();
        matcher = new RuleMatcher(catalog, normalizer);
        resolver = new CategoryResolver();
    }

    public CategorizationResult categorize(String description) {
        return categorizeWithDetails(description).result();
    }

    // Internal diagnostics only: no HTTP endpoint, description storage, or logging.
    public CategorizationDecision categorizeWithDetails(String description) {
        return resolver.resolve(matcher.match(description));
    }
}
