package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.categorization.CategoryCatalog.MatchMode;
import com.financepilot.backend.statement.categorization.CategoryCatalog.Specificity;
import java.util.List;

public record CompiledCatalog(List<CompiledRule> rules) {
    public CompiledCatalog { rules = List.copyOf(rules); }

    public record CompiledRule(String id, TransactionCategory category, int priority,
            Specificity specificity, List<NormalizedPattern> aliases,
            List<NormalizedPattern> include, List<NormalizedPattern> exclude,
            List<List<NormalizedPattern>> includeAny, List<String> refines,
            boolean conclusionRequired, List<List<NormalizedPattern>> concludeAny) {
        public CompiledRule {
            aliases = List.copyOf(aliases);
            include = List.copyOf(include);
            exclude = List.copyOf(exclude);
            includeAny = includeAny.stream().map(List::copyOf).toList();
            refines = List.copyOf(refines);
            concludeAny = concludeAny.stream().map(List::copyOf).toList();
        }

        public CompiledRule(String id, TransactionCategory category, int priority, Specificity specificity,
                List<NormalizedPattern> aliases, List<NormalizedPattern> include, List<NormalizedPattern> exclude,
                List<List<NormalizedPattern>> includeAny, List<String> refines) {
            this(id, category, priority, specificity, aliases, include, exclude, includeAny, refines, false, List.of());
        }

        public CompiledRule(String id, TransactionCategory category, int priority, Specificity specificity,
                List<NormalizedPattern> aliases, List<NormalizedPattern> include, List<NormalizedPattern> exclude) {
            this(id, category, priority, specificity, aliases, include, exclude, List.of(), List.of());
        }
    }

    public record NormalizedPattern(MatchMode mode, String value, List<String> tokens, String paddedValue) {
        public NormalizedPattern { tokens = List.copyOf(tokens); }

        // WORD and a single-token PHRASE have identical matching semantics.
        public String collisionKey() {
            String kind = mode == MatchMode.WORD || mode == MatchMode.PHRASE ? "TOKENS" : mode.name();
            return kind + ":" + value;
        }
    }
}
