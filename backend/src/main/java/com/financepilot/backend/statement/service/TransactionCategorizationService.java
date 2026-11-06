
package com.financepilot.backend.statement.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.dto.CategorizationResult;
import com.financepilot.backend.statement.dto.CategoryRule;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionCategorizationService {

    private final List<CategoryRule> rules;

    public TransactionCategorizationService(
            ObjectMapper objectMapper
    ) {
        try {
            ClassPathResource resource =
                    new ClassPathResource("category-rules.json");

            List<CategoryRule> loadedRules;

            try (var inputStream = resource.getInputStream()) {
                loadedRules = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<CategoryRule>>() {}
                );
            }

            this.rules = loadedRules.stream()
                    .sorted(
                            Comparator
                                    .comparingInt(CategoryRule::priority)
                                    .reversed()
                                    .thenComparing(
                                            rule -> rule.keyword().length(),
                                            Comparator.reverseOrder()
                                    )
                    )
                    .toList();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Erro ao carregar category-rules.json",
                    e
            );
        }
    }

    public CategorizationResult categorize(String description) {

        if (description == null || description.isBlank()) {
            return unknown();
        }

        String normalized = normalize(description);

        for (CategoryRule rule : rules) {

            String keyword = normalize(rule.keyword());

            if (matches(
                    normalized,
                    keyword,
                    rule.matchType()
            )) {
                return new CategorizationResult(
                        rule.category(),
                        CategorizationSource.KEYWORD_RULE,
                        false
                );
            }
        }

        return unknown();
    }

    private boolean matches(
            String description,
            String keyword,
            String matchType
    ) {

        return switch (matchType.toUpperCase(Locale.ROOT)) {

            case "WORD", "PHRASE" ->
                    (" " + description + " ")
                            .contains(" " + keyword + " ");

            case "PREFIX" -> {
                boolean found = false;

                for (String word : description.split(" ")) {
                    if (word.startsWith(keyword)) {
                        found = true;
                        break;
                    }
                }

                yield found;
            }

            default -> false;
        };
    }

    private String normalize(String text) {

        return Normalizer.normalize(
                        text,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
    }

    private CategorizationResult unknown() {

        return new CategorizationResult(
                TransactionCategory.OUTROS,
                CategorizationSource.UNKNOWN,
                true
        );
    }
}
