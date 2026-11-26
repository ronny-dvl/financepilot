package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.categorization.CompiledCatalog.CompiledRule;
import com.financepilot.backend.statement.categorization.CompiledCatalog.NormalizedPattern;
import com.financepilot.backend.statement.categorization.CategoryCatalog.MatchMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RuleMatcher {
    /** Offsets refer only to normalized text; no financial description is retained in diagnostics. */
    public record RuleMatch(CompiledRule rule, NormalizedPattern alias, int start, int end, boolean eligible) {
        public RuleMatch(CompiledRule rule, NormalizedPattern alias) {
            this(rule, alias, 0, alias.value().length(), true);
        }
    }
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
                    || (!rule.includeAny().isEmpty() && rule.includeAny().stream().noneMatch(group ->
                        group.stream().allMatch(pattern -> matches(pattern, normalized, padded, words))))) continue;
            boolean eligible = rule.exclude().stream()
                    .noneMatch(pattern -> matches(pattern, normalized, padded, words));
            // Keep vetoed alias evidence and every occurrence. Exclusions cannot erase competing
            // merchants, and a refinement at one occurrence cannot hide an unrelated occurrence.
            for (NormalizedPattern alias : rule.aliases()) {
                addOccurrences(result, rule, alias, normalized, padded, words, eligible);
            }
        }
        return List.copyOf(result);
    }

    private void addOccurrences(List<RuleMatch> result, CompiledRule rule, NormalizedPattern alias,
            String normalized, String padded, List<String> words, boolean eligible) {
        switch (alias.mode()) {
            case EXACT -> {
                if (normalized.equals(alias.value())) {
                    result.add(new RuleMatch(rule, alias, 0, normalized.length(), eligible));
                }
            }
            case WORD, PHRASE -> {
                int from = 0;
                int index;
                while ((index = padded.indexOf(alias.paddedValue(), from)) >= 0) {
                    result.add(new RuleMatch(rule, alias, index, index + alias.value().length(), eligible));
                    from = index + 1;
                }
            }
            case PREFIX -> {
                int offset = 0;
                for (String word : words) {
                    if (word.startsWith(alias.value())) {
                        result.add(new RuleMatch(rule, alias, offset, offset + word.length(), eligible));
                    }
                    offset += word.length() + 1;
                }
            }
            case STARTS_WITH -> throw new IllegalArgumentException("STARTS_WITH não é modalidade de alias.");
        }
    }

    private boolean matches(NormalizedPattern pattern, String normalized, String padded, List<String> words) {
        return switch (pattern.mode()) {
            case EXACT -> normalized.equals(pattern.value());
            case WORD, PHRASE -> padded.contains(pattern.paddedValue());
            case PREFIX -> words.stream().anyMatch(word -> word.startsWith(pattern.value()));
            case STARTS_WITH -> normalized.equals(pattern.value()) || normalized.startsWith(pattern.value() + " ");
        };
    }
}
