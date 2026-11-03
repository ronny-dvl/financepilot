
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.dto.CategorizationResult;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class TransactionCategorizationService {

    private final Map<String, TransactionCategory> rules =
            new LinkedHashMap<>();

    public TransactionCategorizationService() {
        rules.put("UBER", TransactionCategory.TRANSPORTE);
        rules.put("99APP", TransactionCategory.TRANSPORTE);
        rules.put("NETFLIX", TransactionCategory.ASSINATURAS);
        rules.put("SPOTIFY", TransactionCategory.ASSINATURAS);
        rules.put("IFOOD", TransactionCategory.ALIMENTACAO);
        rules.put("SUPERMERCADO", TransactionCategory.ALIMENTACAO);
        rules.put("ATACADAO", TransactionCategory.ALIMENTACAO);
        rules.put("FARMACIA", TransactionCategory.SAUDE);
    }

    public CategorizationResult categorize(String description) {

        if (description == null || description.isBlank()) {
            return unknown();
        }

        String normalized = normalize(description);

        for (Map.Entry<String, TransactionCategory> rule : rules.entrySet()) {

            String keyword = normalize(rule.getKey());

            if ((" " + normalized + " ").contains(" " + keyword + " ")) {
                return new CategorizationResult(
                        rule.getValue(),
                        CategorizationSource.KEYWORD_RULE,
                        false
                );
            }
        }

        return unknown();
    }

    private String normalize(String description) {

        String normalized = Normalizer.normalize(
                description, Normalizer.Form.NFD
        );

        return normalized
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
