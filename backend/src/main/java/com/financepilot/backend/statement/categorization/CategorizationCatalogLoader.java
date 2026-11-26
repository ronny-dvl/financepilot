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
            if (catalog == null || !Integer.valueOf(1).equals(catalog.version())
                    || catalog.category() == null || catalog.rules() == null) {
                throw invalid("Versão, categoria ou lista de regras inválida.");
            }
            for (RuleDefinition rule : catalog.rules()) {
                if (rule == null || rule.id() == null || !ID.matcher(rule.id()).matches()) {
                    throw invalid("Identificador de regra inválido.");
                }
                if (!ids.add(rule.id())) throw invalid("ID duplicado: " + rule.id());
                if (rule.priority() == null || rule.priority() < 0 || rule.specificity() == null
                        || rule.matchType() == null || rule.aliases() == null || rule.aliases().isEmpty()) {
                    throw invalid("Configuração incompleta ou inválida: " + rule.id());
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
                Set<String> required = new HashSet<>();
                for (NormalizedPattern condition : include) required.add(condition.collisionKey());
                if (exclude.stream().anyMatch(condition -> required.contains(condition.collisionKey()))) {
                    throw invalid("Condições de inclusão e exclusão contraditórias: " + rule.id());
                }
                compiled.add(new CompiledRule(rule.id(), catalog.category(), rule.priority(),
                        rule.specificity(), patterns, include, exclude));
            }
        }
        if (compiled.isEmpty()) throw invalid("Catálogo sem regras.");
        return new CompiledCatalog(compiled);
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
