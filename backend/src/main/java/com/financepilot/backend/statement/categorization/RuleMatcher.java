package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.categorization.CompiledCatalog.CompiledRule;
import com.financepilot.backend.statement.categorization.CompiledCatalog.NormalizedPattern;
import com.financepilot.backend.statement.categorization.CategoryCatalog.MatchMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RuleMatcher {
    public record RuleMatch(CompiledRule rule, NormalizedPattern alias) { }
    private final DescriptionNormalizer normalizer;
    private final Map<String, List<CompiledRule>> tokenIndex;
    private final Map<Character, List<CompiledRule>> prefixIndex;

    public RuleMatcher(CompiledCatalog catalog, DescriptionNormalizer normalizer) {
        this.normalizer = normalizer;
        Map<String, Set<CompiledRule>> tokens = new HashMap<>();
        Map<Character, Set<CompiledRule>> prefixes = new HashMap<>();
        for (CompiledRule rule : catalog.rules()) {
            for (NormalizedPattern alias : rule.aliases()) {
                if (alias.mode() == MatchMode.PREFIX) {
                    prefixes.computeIfAbsent(alias.value().charAt(0), key -> new LinkedHashSet<>()).add(rule);
                } else {
                    tokens.computeIfAbsent(alias.tokens().getFirst(), key -> new LinkedHashSet<>()).add(rule);
                }
            }
        }
        Map<String, List<CompiledRule>> frozenTokens = new HashMap<>();
        tokens.forEach((key, value) -> frozenTokens.put(key, List.copyOf(value)));
        tokenIndex = Map.copyOf(frozenTokens);
        Map<Character, List<CompiledRule>> frozenPrefixes = new HashMap<>();
        prefixes.forEach((key, value) -> frozenPrefixes.put(key, List.copyOf(value)));
        prefixIndex = Map.copyOf(frozenPrefixes);
    }

    public List<RuleMatch> match(String description) {
        String normalized = normalizer.normalize(description);
        if (normalized.isEmpty()) return List.of();
        List<String> words = List.of(normalized.split(" "));
        String padded = " " + normalized + " ";
        Set<CompiledRule> candidates = new LinkedHashSet<>();
        for (String word : words) {
            candidates.addAll(tokenIndex.getOrDefault(word, List.of()));
            candidates.addAll(prefixIndex.getOrDefault(word.charAt(0), List.of()));
        }
        List<RuleMatch> result = new ArrayList<>();
        for (CompiledRule rule : candidates) {
            if (!rule.include().stream().allMatch(pattern -> matches(pattern, normalized, padded, words))
                    || rule.exclude().stream().anyMatch(pattern -> matches(pattern, normalized, padded, words))) continue;
            rule.aliases().stream().filter(alias -> matches(alias, normalized, padded, words))
                    .max(Comparator.comparingInt((NormalizedPattern alias) -> alias.tokens().size())
                            .thenComparingInt(alias -> alias.value().length()))
                    .ifPresent(alias -> result.add(new RuleMatch(rule, alias)));
        }
        return List.copyOf(result);
    }

    private boolean matches(NormalizedPattern pattern, String normalized, String padded, List<String> words) {
        return switch (pattern.mode()) {
            case EXACT -> normalized.equals(pattern.value());
            case WORD, PHRASE -> padded.contains(pattern.paddedValue());
            case PREFIX -> words.stream().anyMatch(word -> word.startsWith(pattern.value()));
        };
    }
}
