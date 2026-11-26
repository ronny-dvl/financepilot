package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.PaymentMethod;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.TransactionResponse;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class PixRecipientExtractionService {
    private static final Pattern PREFIX = Pattern.compile(
            "(?iu)^\\s*PIX\\s+(?:ENVIADO|TRANSF|QRS)\\b\\s*(?:[-–—:]\\s*)?(.+?)\\s*$");
    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{M}]+(?:[ '\u2019-][\\p{L}\\p{M}]+)*");
    private static final Pattern IDENTIFIER = Pattern.compile("(?iu)\\s+CPF\\b.*$");

    public Optional<String> extract(TransactionResponse transaction) {
        if (transaction.category() != TransactionCategory.TRANSFERENCIAS_PESSOAIS
                || transaction.nature() != TransactionNature.TRANSFER
                || transaction.paymentMethod() != PaymentMethod.PIX || transaction.requiresReview()
                || transaction.amount() == null || transaction.amount().signum() >= 0
                || transaction.description() == null || transaction.date() == null) return Optional.empty();
        String description = transaction.description().strip().replaceAll("[\\p{Z}\\s]+", " ");
        String normalized = " " + normalizedName(description) + " ";
        if (normalized.contains(" MESMA TITULARIDADE ") || normalized.contains(" ENTRE MINHAS CONTAS ")
                || normalized.contains(" TRANSFERENCIA ENTRE CONTAS PROPRIAS ")
                || normalized.contains(" CNPJ ")) return Optional.empty();
        var matcher = PREFIX.matcher(description);
        if (!matcher.matches()) return Optional.empty();
        String name = IDENTIFIER.matcher(matcher.group(1)).replaceFirst("").strip();
        if (name.length() < 2 || !NAME.matcher(name).matches()) return Optional.empty();
        return Optional.of(name);
    }

    // Preserve punctuation and every truncated letter: no fuzzy matching, completion or prefix merge.
    public String normalizedName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).strip().replaceAll("[\\p{Z}\\s]+", " ");
    }
}
