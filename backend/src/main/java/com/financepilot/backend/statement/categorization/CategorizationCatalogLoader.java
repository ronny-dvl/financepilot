package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.categorization.CategoryCatalog.Condition;
import com.financepilot.backend.statement.categorization.CategoryCatalog.MatchMode;
import com.financepilot.backend.statement.categorization.CategoryCatalog.RuleDefinition;
import com.financepilot.backend.statement.categorization.CompiledCatalog.CompiledRule;
import com.financepilot.backend.statement.categorization.CompiledCatalog.NormalizedPattern;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class CategorizationCatalogLoader {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*");
    private final ObjectMapper mapper;
    private final DescriptionNormalizer normalizer;

    public CategorizationCatalogLoader(ObjectMapper mapper, DescriptionNormalizer normalizer) {
        this.mapper = mapper;
        this.normalizer = normalizer;
    }

    public CompiledCatalog load() {
        try {
            return load(new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:categorization/*.json"));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível localizar o catálogo de categorização.", e);
        }
    }

    public CompiledCatalog load(Resource... resources) {
        if (resources == null || resources.length == 0) throw invalid("Catálogo ausente.");
        List<CompiledRule> compiled = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Map<String, String> aliases = new HashMap<>();
        List<Resource> ordered = Arrays.stream(resources)
                .sorted(Comparator.comparing(resource -> Objects.toString(resource.getFilename(), resource.getDescription())))
                .toList();
        for (Resource resource : ordered) {
            CategoryCatalog catalog;
            try (var input = resource.getInputStream()) {
                catalog = mapper.readValue(input, CategoryCatalog.class);
            } catch (IOException | RuntimeException e) {
                throw new IllegalArgumentException("JSON de categorização inválido: " + resource.getDescription(), e);
            }
            if (catalog == null || !(Integer.valueOf(1).equals(catalog.version())
                    || Integer.valueOf(2).equals(catalog.version()))
                    || catalog.category() == null || catalog.rules() == null) {
                throw invalid("Versão, categoria ou lista de regras inválida.");
            }
            for (RuleDefinition rule : catalog.rules()) {
                if (rule == null || rule.id() == null || !ID.matcher(rule.id()).matches()) {
                    throw invalid("Identificador de regra inválido.");
                }
                if (!ids.add(rule.id())) throw invalid("ID duplicado: " + rule.id());
                if (Integer.valueOf(1).equals(catalog.version())
                        && (rule.includeAny() != null || rule.refines() != null)) {
                    throw invalid("includeAny e refines exigem catálogo versão 2: " + rule.id());
                }
                if (rule.priority() == null || rule.priority() < 0 || rule.specificity() == null
                        || rule.matchType() == null || rule.aliases() == null || rule.aliases().isEmpty()) {
                    throw invalid("Configuração incompleta ou inválida: " + rule.id());
                }
                if (rule.matchType() == MatchMode.STARTS_WITH) {
                    throw invalid("STARTS_WITH é reservado a condições de contexto: " + rule.id());
                }
                List<NormalizedPattern> patterns = rule.aliases().stream()
                        .map(alias -> compile(rule.matchType(), alias)).toList();
                for (NormalizedPattern alias : patterns) {
                    String previous = aliases.putIfAbsent(alias.collisionKey(), rule.id());
                    if (previous != null) {
                        throw invalid("Alias duplicado ou conflitante entre " + previous + " e " + rule.id());
                    }
                }
                List<NormalizedPattern> include = compileConditions(rule.include());
                List<NormalizedPattern> exclude = compileConditions(rule.exclude());
                List<List<NormalizedPattern>> includeAny = compileAlternatives(rule.includeAny());
                if (Integer.valueOf(1).equals(catalog.version())
                        && java.util.stream.Stream.concat(include.stream(), exclude.stream())
                        .anyMatch(condition -> condition.mode() == MatchMode.STARTS_WITH)) {
                    throw invalid("STARTS_WITH exige catálogo versão 2: " + rule.id());
                }
                Set<String> required = new HashSet<>();
                for (NormalizedPattern condition : include) required.add(condition.collisionKey());
                if (exclude.stream().anyMatch(condition -> required.contains(condition.collisionKey()))) {
                    throw invalid("Condições de inclusão e exclusão contraditórias: " + rule.id());
                }
                for (List<NormalizedPattern> group : includeAny) {
                    if (group.stream().anyMatch(condition -> exclude.stream().anyMatch(veto ->
                            veto.collisionKey().equals(condition.collisionKey())))) {
                        throw invalid("Alternativa contraditória com exclusão: " + rule.id());
                    }
                }
                List<String> refines = rule.refines() == null ? List.of() : rule.refines();
                Set<String> parents = new HashSet<>();
                for (String parent : refines) {
                    if (parent == null || !ID.matcher(parent).matches()
                            || parent.equals(rule.id()) || !parents.add(parent)) {
                        throw invalid("Referência de refinamento inválida: " + rule.id());
                    }
                }
                compiled.add(new CompiledRule(rule.id(), catalog.category(), rule.priority(),
                        rule.specificity(), patterns, include, exclude, includeAny, refines));
            }
        }
        if (compiled.isEmpty()) throw invalid("Catálogo sem regras.");
        // Validate references and cycles and compile the transitive closure once, never per transaction.
        Map<String, CompiledRule> byId = new HashMap<>();
        compiled.forEach(rule -> byId.put(rule.id(), rule));
        Map<String, List<String>> ancestors = new HashMap<>();
        for (CompiledRule rule : compiled) collectAncestors(rule.id(), byId, ancestors, new HashSet<>());
        return new CompiledCatalog(compiled.stream().map(rule -> new CompiledRule(rule.id(), rule.category(),
                rule.priority(), rule.specificity(), rule.aliases(), rule.include(), rule.exclude(),
                rule.includeAny(), ancestors.get(rule.id()))).toList());
    }

    private List<String> collectAncestors(String id, Map<String, CompiledRule> rules,
            Map<String, List<String>> compiled, Set<String> visiting) {
        if (compiled.containsKey(id)) return compiled.get(id);
        if (!visiting.add(id)) throw invalid("Ciclo de refinamentos: " + id);
        CompiledRule rule = rules.get(id);
        if (rule == null) throw invalid("Regra de refinamento não encontrada: " + id);
        Set<String> ancestors = new HashSet<>();
        for (String parent : rule.refines()) {
            CompiledRule parentRule = rules.get(parent);
            if (parentRule != null
                    && rule.specificity() == CategoryCatalog.Specificity.GENERIC
                    && parentRule.specificity() == CategoryCatalog.Specificity.SPECIFIC) {
                throw invalid("Regra genérica não pode refinar evidência específica: " + id);
            }
            ancestors.add(parent);
            ancestors.addAll(collectAncestors(parent, rules, compiled, visiting));
        }
        visiting.remove(id);
        List<String> result = ancestors.stream().sorted().toList();
        compiled.put(id, result);
        return result;
    }

    private List<List<NormalizedPattern>> compileAlternatives(List<List<Condition>> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        List<List<NormalizedPattern>> result = new ArrayList<>();
        Set<List<String>> seen = new HashSet<>();
        for (List<Condition> group : raw) {
            if (group == null || group.isEmpty()) throw invalid("Alternativa vazia ou nula.");
            List<NormalizedPattern> patterns = compileConditions(group);
            List<String> signature = patterns.stream().map(NormalizedPattern::collisionKey).sorted().toList();
            if (signature.stream().distinct().count() != signature.size() || !seen.add(signature)) {
                throw invalid("Condição ou alternativa redundante após normalização.");
            }
            result.add(patterns);
        }
        return List.copyOf(result);
    }

    private List<NormalizedPattern> compileConditions(List<Condition> conditions) {
        if (conditions == null) return List.of();
        return conditions.stream().map(condition -> {
            if (condition == null) throw invalid("Condição nula.");
            return compile(condition.matchType(), condition.value());
        }).toList();
    }

    private NormalizedPattern compile(MatchMode mode, String raw) {
        if (mode == null) throw invalid("Modalidade de correspondência obrigatória.");
        String value = normalizer.normalize(raw);
        if (value.isEmpty()) throw invalid("Alias ou condição sem conteúdo normalizado.");
        List<String> tokens = List.of(value.split(" "));
        if ((mode == MatchMode.WORD || mode == MatchMode.PREFIX) && tokens.size() != 1) {
            throw invalid("WORD e PREFIX exigem um único token.");
        }
        return new NormalizedPattern(mode, value, tokens, " " + value + " ");
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
