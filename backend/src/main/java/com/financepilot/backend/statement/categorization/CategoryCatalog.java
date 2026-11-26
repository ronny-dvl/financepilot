package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.TransactionCategory;
import java.util.List;

/** Internal configuration model, independent of the public API DTOs. */
public record CategoryCatalog(Integer version, TransactionCategory category, List<RuleDefinition> rules) {
    public enum MatchMode { WORD, PHRASE, EXACT, PREFIX, STARTS_WITH }
    public enum Specificity { GENERIC, SPECIFIC }

    public record RuleDefinition(String id, List<String> aliases, Integer priority,
            MatchMode matchType, Specificity specificity,
            List<Condition> include, List<Condition> exclude,
            List<List<Condition>> includeAny, List<String> refines,
            List<List<Condition>> concludeAny) { }

    /** include/includeAny identify the entity; concludeAny gates purpose without erasing evidence.
     * Missing concludeAny preserves legacy behavior; [] retains review-only identity evidence.
     * Alternatives are OR of nonempty AND groups; any exclude vetoes eligibility. */
    public record Condition(MatchMode matchType, String value) { }
}
