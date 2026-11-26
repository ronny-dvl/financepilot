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
import java.util.regex.Pattern;

public final class RuleMatcher {
    private static final Pattern NAMED_PIX = Pattern.compile("^PIX (?:ENVIADO|RECEBIDO|TRANSF) ");
    private static final Pattern PAYEE_CONTEXT = Pattern.compile(
            "\\b(?:CNPJ|LTDA|EIRELI|LOJAS?|SUPERMERCADOS?|MERCADINHO|ATACADISTA|RESTAURANTE|"
                    + "LANCHONETE|FARMACIAS?|DROGARIAS?|HOSPITAL|LABORATORIO|CLINICA|CURSOS?|"
                    + "ESCOLA|FACULDADE|UNIVERSIDADE|MENSALIDADES?|ASSINATURAS?|TELEFONIA|ENERGIA|"
                    + "MESMA TITULARIDADE|ENTRE MINHAS CONTAS|CONTAS PROPRIAS)\\b");
    private static final Pattern PAYMENT_PROCESSOR = Pattern.compile(
            "\\b(?:MERCADO PAGO|MERCADOPAGO|PAGSEGURO|PAGBANK|PAYPAL|STRIPE|CIELO|GETNET|STONE)\\b");
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
                    .noneMatch(pattern -> matches(pattern, normalized, padded, words))
                    && (!rule.conclusionRequired() || rule.concludeAny().stream().anyMatch(group ->
                        group.stream().allMatch(pattern -> matches(pattern, normalized, padded, words))));
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
                    int end = index + alias.value().length();
                    if (!isPersonalPixPayee(rule, normalized, index)) {
                        result.add(new RuleMatch(rule, alias, index, end, eligible));
                    }
                    from = index + 1;
                }
            }
            case PREFIX -> {
                int offset = 0;
                for (String word : words) {
                    if (word.startsWith(alias.value())) {
                        if (!isPersonalPixPayee(rule, normalized, offset)) {
                            result.add(new RuleMatch(rule, alias, offset, offset + word.length(), eligible));
                        }
                    }
                    offset += word.length() + 1;
                }
            }
            case STARTS_WITH -> throw new IllegalArgumentException("STARTS_WITH não é modalidade de alias.");
        }
    }

    // Recognize the merchant without demanding a purchase marker. In an explicitly named Pix
    // payee, a name before a merchant surname (or an explicit CPF) needs commercial evidence.
    // Merchant suffixes such as city/branch and bank references remain valid; no name list is used.
    private boolean isPersonalPixPayee(CompiledRule rule, String description, int start) {
        switch (rule.category()) {
            case INVESTIMENTOS, TRANSFERENCIAS, TARIFAS_BANCARIAS, IMPOSTOS_TRIBUTOS -> { return false; }
            default -> { }
        }
        var prefix = NAMED_PIX.matcher(description);
        if (!prefix.find() || start < prefix.end() || PAYEE_CONTEXT.matcher(description).find()) return false;
        if ((" " + description + " ").contains(" CPF ")) return true;
        String remainder = description.substring(prefix.end(), start);
        remainder = PAYMENT_PROCESSOR.matcher(remainder).replaceAll(" ");
        for (String word : remainder.trim().split(" +")) {
            if (!word.isEmpty() && word.chars().allMatch(Character::isLetter)) return true;
        }
        return false;
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
