package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.categorization.RuleMatcher.RuleMatch;
import com.financepilot.backend.statement.categorization.CategoryCatalog.Specificity;
import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.dto.CategorizationResult;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.financepilot.backend.statement.categorization.CategoryCatalog.MatchMode;

public final class CategoryResolver {
    // Priority chooses a diagnostic rule only AFTER all remaining evidence agrees on a category.
    // Neither priority nor alias length can settle independent cross-category merchant evidence.
    private static final Comparator<RuleMatch> DIAGNOSTIC_RANK = Comparator
            .comparingInt((RuleMatch match) -> match.rule().priority())
            .thenComparingInt(match -> switch (match.alias().mode()) {
                case EXACT -> 3;
                case WORD, PHRASE -> 2;
                case PREFIX -> 1;
                case STARTS_WITH -> 0;
            })
            .thenComparing(match -> match.rule().id(), Comparator.reverseOrder());

    public CategorizationDecision resolve(List<RuleMatch> matches) {
        if (matches.isEmpty()) return unknown(List.of());
        Set<RuleMatch> refined = findRefinedOccurrences(matches);
        List<RuleMatch> remaining = matches.stream().filter(match -> !refined.contains(match)).toList();
        boolean hasSpecific = remaining.stream().anyMatch(match -> match.rule().specificity() == Specificity.SPECIFIC);
        List<RuleMatch> evidence = remaining.stream().filter(match -> !hasSpecific
                || match.rule().specificity() == Specificity.SPECIFIC).toList();
        if (evidence.stream().map(match -> match.rule().category()).distinct().count() > 1) {
            return unknown(evidence.stream().map(match -> match.rule().id()).distinct().sorted().toList());
        }
        var best = evidence.stream().filter(RuleMatch::eligible).max(DIAGNOSTIC_RANK);
        if (best.isEmpty()) return unknown(List.of());
        RuleMatch winner = best.orElseThrow();
        return new CategorizationDecision(new CategorizationResult(winner.rule().category(),
                CategorizationSource.KEYWORD_RULE, false), winner.rule().id(), List.of());
    }

    private Set<RuleMatch> findRefinedOccurrences(List<RuleMatch> matches) {
        Map<String, List<RuleMatch>> byId = new HashMap<>();
        List<RuleMatch> prefixes = new ArrayList<>();
        for (RuleMatch match : matches) {
            byId.computeIfAbsent(match.rule().id(), key -> new ArrayList<>()).add(match);
            if (match.alias().mode() == MatchMode.PREFIX) prefixes.add(match);
        }
        Set<RuleMatch> refined = new HashSet<>();
        for (RuleMatch child : matches) {
            if (!child.eligible()) continue;
            for (String parentId : child.rule().refines()) {
                for (RuleMatch parent : byId.getOrDefault(parentId, List.of())) {
                    if (covers(child, parent)) refined.add(parent);
                }
            }
            // A full literal at this occurrence refines a partial prefix of the same lexeme.
            if (child.alias().mode() != MatchMode.PREFIX) {
                for (RuleMatch parent : prefixes) {
                    if (child.rule().specificity() == Specificity.GENERIC
                            && parent.rule().specificity() == Specificity.SPECIFIC) continue;
                    if (covers(child, parent) && child.alias().value().startsWith(parent.alias().value())) {
                        refined.add(parent);
                    }
                }
            }
        }
        return refined;
    }

    private boolean covers(RuleMatch child, RuleMatch parent) {
        return child.start() <= parent.start() && child.end() >= parent.end();
    }

    private CategorizationDecision unknown(List<String> conflicts) {
        return new CategorizationDecision(new CategorizationResult(TransactionCategory.OUTROS,
                CategorizationSource.UNKNOWN, true), null, conflicts);
    }
}
