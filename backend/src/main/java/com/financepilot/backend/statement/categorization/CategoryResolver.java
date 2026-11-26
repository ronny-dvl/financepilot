package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.categorization.RuleMatcher.RuleMatch;
import com.financepilot.backend.statement.categorization.CategoryCatalog.Specificity;
import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.dto.CategorizationResult;
import java.util.Comparator;
import java.util.List;

public final class CategoryResolver {
    // Explicit precedence; file order is never a category tie-breaker.
    private static final Comparator<RuleMatch> RANK = Comparator
            .comparingInt((RuleMatch match) -> match.rule().specificity() == Specificity.SPECIFIC ? 1 : 0)
            .thenComparingInt(match -> match.rule().priority())
            .thenComparingInt(match -> switch (match.alias().mode()) {
                case EXACT -> 3;
                case WORD, PHRASE -> 2;
                case PREFIX -> 1;
            })
            .thenComparingInt(match -> match.alias().tokens().size())
            .thenComparingInt(match -> match.alias().value().length());

    public CategorizationDecision resolve(List<RuleMatch> matches) {
        if (matches.isEmpty()) return unknown(List.of());
        RuleMatch best = matches.stream().max(RANK).orElseThrow();
        List<RuleMatch> winners = matches.stream().filter(match -> RANK.compare(match, best) == 0)
                .sorted(Comparator.comparing(match -> match.rule().id())).toList();
        if (winners.stream().map(match -> match.rule().category()).distinct().count() > 1) {
            return unknown(winners.stream().map(match -> match.rule().id()).toList());
        }
        return new CategorizationDecision(new CategorizationResult(best.rule().category(),
                CategorizationSource.KEYWORD_RULE, false), winners.getFirst().rule().id(), List.of());
    }

    private CategorizationDecision unknown(List<String> conflicts) {
        return new CategorizationDecision(new CategorizationResult(TransactionCategory.OUTROS,
                CategorizationSource.UNKNOWN, true), null, conflicts);
    }
}
