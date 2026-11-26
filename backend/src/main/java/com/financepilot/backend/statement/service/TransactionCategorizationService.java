package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.categorization.CategorizationCatalogLoader;
import com.financepilot.backend.statement.categorization.CategorizationDecision;
import com.financepilot.backend.statement.categorization.CategoryResolver;
import com.financepilot.backend.statement.categorization.DescriptionNormalizer;
import com.financepilot.backend.statement.categorization.RuleMatcher;
import com.financepilot.backend.statement.dto.CategorizationResult;
import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TransactionCategorizationService {
    private static final Pattern OUTGOING_PIX = Pattern.compile("^PIX (?:ENVIADO|TRANSF|QRS) (.+)$");
    private static final Pattern NON_PERSONAL_PAYEE = Pattern.compile(
            "\\b(?:BRASIL|BRASILEIR[OA]S?|TECN|TEC|TECNOLOGIA|INSTITUIC(?:AO|OES)?|"
                    + "EMPRESA|EMPRESAS|COMERCIO|COMERCIAL|INDUSTRIA|SERVICOS|LTDA|LT|EIRELI|EPP|ME|CNPJ|"
                    + "BANCO|BANK|FINANCEIR[OA]|PAGAMENTOS?|PAGSEGURO|PAGBANK|PAYPAL|STRIPE|MERCADO|"
                    + "MERCADOPAGO|CIELO|GETNET|STONE|PICPAY|HOTMART|NUBANK|ITAU|SANTANDER|BRADESCO|INTER|"
                    + "COMPRA|RSCSS|AGENDAD[OA]|PENDENTE|CANCELAD[OA]|NAO|DEVOLVID[OA]|"
                    + "TURISMO|SHOPPING|LOJAS?|SUPERMERC[A-Z]*|MERCADIN[A-Z]*|COSMET[A-Z]*|"
                    + "DISTRIBUIDORA|TRANSPORTES?|UTILIDADES|PAPELARIA|IOF|IPVA|IPTU|DARF|DAS|INSS|"
                    + "SALARIO|TARIFA|APLICACAO|RESGATE|DEVOLUCAO|ESTORNO|ESTORNAD[OA]|REEMBOLSO|"
                    + "SEM|IDENTIFICACAO|DESCONHECID[OA]|INFORMAD[OA]|TESOURO|INVESTIMENTOS?|"
                    + "FUNDOS?|ACOES|CDB|LCI|LCA|ETF|FII)\\b");
    private final DescriptionNormalizer normalizer = new DescriptionNormalizer();
    private final RuleMatcher matcher;
    private final CategoryResolver resolver;

    public TransactionCategorizationService(ObjectMapper objectMapper) {
        var catalog = new CategorizationCatalogLoader(objectMapper, normalizer).load();
        matcher = new RuleMatcher(catalog, normalizer);
        resolver = new CategoryResolver();
    }

    public CategorizationResult categorize(String description) {
        return categorizeWithDetails(description).result();
    }

    // Internal diagnostics only: no HTTP endpoint, description storage, or logging.
    public CategorizationDecision categorizeWithDetails(String description) {
        String normalized = normalizer.normalize(description);
        String padded = " " + normalized + " ";
        String ownRule = padded.contains(" MESMA TITULARIDADE ")
                ? "transferencias.transferencia-mesma-titularidade"
                : padded.contains(" ENTRE MINHAS CONTAS ") ? "transferencias.transferencia-entre-minhas-contas"
                : padded.contains(" TRANSFERENCIA ENTRE CONTAS PROPRIAS ")
                ? "transferencias.transferencia-entre-contas-proprias" : null;
        if (ownRule != null) return identified(TransactionCategory.TRANSFERENCIAS, ownRule);

        var matches = matcher.matchNormalized(normalized);
        var decision = resolver.resolve(matches);
        // Never replace a merchant, vetoed commercial evidence or an unresolved catalog conflict
        // with a guessed personal name. This fallback has no list of individual names.
        if (decision.result().category() != TransactionCategory.OUTROS || !matches.isEmpty()) return decision;
        var pix = OUTGOING_PIX.matcher(normalized);
        if (pix.matches() && looksLikePersonalName(pix.group(1))) {
            return identified(TransactionCategory.TRANSFERENCIAS_PESSOAIS, "transferencias-pessoais.nome-provavel");
        }
        return decision;
    }

    private boolean looksLikePersonalName(String payee) {
        if (NON_PERSONAL_PAYEE.matcher(payee).find()) return false;
        String name = payee.replaceFirst("\\s+CPF\\s+[0-9 ]+$", "");
        String[] words = name.split(" ");
        if (words.length > 6 || !words[0].matches("[A-Z]{3,20}")) return false;
        if (words.length == 1 && words[0].length() < 4) return false;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i > 0 && List.of("DA", "DAS", "DE", "DO", "DOS", "E").contains(word)) continue;
            if (!word.matches("[A-Z]{2,20}") || !word.matches(".*[AEIOUY].*")) return false;
        }
        return true;
    }

    private CategorizationDecision identified(TransactionCategory category, String ruleId) {
        return new CategorizationDecision(new CategorizationResult(category, CategorizationSource.KEYWORD_RULE, false),
                ruleId, List.of());
    }
}
