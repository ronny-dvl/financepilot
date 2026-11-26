package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.TransactionCategory;
import java.util.List;

/** Internal configuration model, independent of the public API DTOs. */
public record CategoryCatalog(Integer version, TransactionCategory category, List<RuleDefinition> rules) {
    public enum MatchMode { WORD, PHRASE, EXACT, PREFIX }
    public enum Specificity { GENERIC, SPECIFIC }

    public record RuleDefinition(String id, List<String> aliases, Integer priority,
            MatchMode matchType, Specificity specificity,
            List<Condition> include, List<Condition> exclude) { }

    /** All include conditions must match; any exclude condition vetoes the rule. */
    public record Condition(MatchMode matchType, String value) { }
}
