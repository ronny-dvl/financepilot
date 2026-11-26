
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.math.BigDecimal;
import java.util.Locale;

@Service
public class TransactionNatureService {

    public TransactionNature identify(String description) {
        return identify(description, TransactionCategory.OUTROS);
    }

    /** Compatibility for operation-only callers. Statement processing must use the signed overload. */
    public TransactionNature identify(
            String description,
            TransactionCategory category
    ) {
        if (description == null || description.isBlank()) {
            return TransactionNature.UNKNOWN;
        }
        return identifyOperation(normalize(description), category, false);
    }

    /**
     * A negative parsed statement amount is evidence of an outflow, not by itself of consumption.
     * Billing evidence additionally requires a known consumption category and a settled operation.
     * Positive purchases/charges and negative salaries are contradictory and remain UNKNOWN.
     * TRANSFER and REFUND can legitimately occur in either direction. Investments have no distinct
     * nature in the existing contract, so remain UNKNOWN and are never inferred to be consumption.
     */
    public TransactionNature identify(String description, TransactionCategory category, BigDecimal amount) {
        if (description == null || description.isBlank() || amount == null || amount.signum() == 0) {
            return TransactionNature.UNKNOWN;
        }
        TransactionNature nature = identifyOperation(normalize(description), category, amount.signum() < 0);
        if ((amount.signum() > 0 && nature == TransactionNature.EXPENSE)
                || (amount.signum() > 0 && category == TransactionCategory.TRANSFERENCIAS_PESSOAIS
                    && nature == TransactionNature.TRANSFER)
                || (amount.signum() < 0 && nature == TransactionNature.INCOME)) {
            return TransactionNature.UNKNOWN;
        }
        return nature;
    }

    private TransactionNature identifyOperation(String normalized, TransactionCategory category,
            boolean confirmedDebit) {
        boolean consumptionCategory = category != TransactionCategory.INVESTIMENTOS
                && category != TransactionCategory.TRANSFERENCIAS
                && category != TransactionCategory.TRANSFERENCIAS_PESSOAIS;

        if (containsAnyPhrase(normalized, "AGENDADO", "AGENDADA", "PENDENTE", "CANCELADO", "CANCELADA",
                "ESTORNADO", "ESTORNADA", "NAO PAGO", "NAO PAGA", "A PAGAR")) {
            return TransactionNature.UNKNOWN;
        }

        // Explicit own-account transfers take precedence over purchase/Pix indicators.
        if (containsAnyPhrase(normalized, "ENTRE MINHAS CONTAS", "MESMA TITULARIDADE",
                "TRANSFERENCIA ENTRE CONTAS PROPRIAS")) {
            return TransactionNature.TRANSFER;
        }

        // 1. Devoluções e estornos
        if (startsWithAnyPhrase(normalized, "DEV PIX", "PIX DEVOLVIDO", "ESTORNO", "DEVOLUCAO")) {

            return TransactionNature.REFUND;
        }

        // 2. Recebimentos de salário
        if (startsWithAnyPhrase(normalized, "CREDITO DE SALARIO", "PAGAMENTO DE SALARIO")) {

            return TransactionNature.INCOME;
        }

        if (category == TransactionCategory.TRANSFERENCIAS_PESSOAIS
                && startsWithAnyPhrase(normalized, "PIX ENVIADO", "PIX TRANSF", "PIX QRS")) {
            return TransactionNature.TRANSFER;
        }

        // 3. Compras e pagamentos explícitos
        // Financial operation evidence protects investment outflows even if a competing merchant
        // alias won categorization. Do not change that category or resolve catalog conflicts here.
        if (startsWithAnyPhrase(normalized, "APLICACAO", "APLIC", "RESGATE", "RESG",
                "COMPRA DE ACOES", "COMPRA DE ETF", "COMPRA DE FUNDOS IMOBILIARIOS",
                "COMPRA TESOURO", "VENDA DE ACOES", "VENDA TESOURO", "INTEGRALIZACAO COTAS FUNDO")) {
            return TransactionNature.UNKNOWN;
        }

        if (consumptionCategory && startsWithAnyPhrase(normalized,
                "PIX QRS", "RSCSS", "COMPRA", "PAGAMENTO CARTAO")) {

            return TransactionNature.EXPENSE;
        }

        // 5. Pix enviado para estabelecimento reconhecido
        if (startsWithAnyPhrase(normalized, "PIX ENVIADO", "PIX TRANSF")
                && category != null
                && consumptionCategory
                && category != TransactionCategory.OUTROS) {

            return TransactionNature.EXPENSE;
        }

        // Known category alone is insufficient. These markers describe payment/recurring charges,
        // and only a negative movement from the statement provides debit evidence.
        if (confirmedDebit && category != null && consumptionCategory
                && category != TransactionCategory.OUTROS) {
            if (startsWithAnyPhrase(normalized, "BOLETO", "PGTO", "PAGTO", "PG", "PAGAMENTO", "MENSALIDADE")) {
                return TransactionNature.EXPENSE;
            }
            if (category == TransactionCategory.TARIFAS_BANCARIAS && startsWithAnyPhrase(normalized,
                    "TAR", "TARIFA", "ANUID", "ANUIDADE", "CESTA DE SERVICOS BANCARIOS",
                    "PACOTE DE SERVICOS BANCARIOS", "COMISSAO DE PERMANENCIA")) {
                return TransactionNature.EXPENSE;
            }
            // Bank tax debits need no purchase marker. Recognition by the catalog and the
            // negative amount are both required; scheduled/refunded/own-account operations
            // have already been handled above.
            if (category == TransactionCategory.IMPOSTOS_TRIBUTOS && startsWithAnyPhrase(normalized,
                    "IOF", "IPVA", "IPTU", "DARF", "DAS", "GPS INSS", "GUIA DE TRIBUTOS")) {
                return TransactionNature.EXPENSE;
            }
        }

        // 6. Demais operações permanecem desconhecidas
        return TransactionNature.UNKNOWN;
    }

    private boolean startsWithAnyPhrase(String normalized, String... phrases) {
        for (String phrase : phrases) {
            if (normalized.equals(phrase) || normalized.startsWith(phrase + " ")) return true;
        }
        return false;
    }

    private boolean containsAnyPhrase(String normalized, String... phrases) {
        String padded = " " + normalized + " ";
        for (String phrase : phrases) {
            if (padded.contains(" " + phrase + " ")) return true;
        }
        return false;
    }

    private String normalize(String description) {

        return Normalizer.normalize(
                        description, Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
    }
}
