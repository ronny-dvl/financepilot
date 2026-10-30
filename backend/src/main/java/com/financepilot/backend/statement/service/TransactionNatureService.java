
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionNature;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class TransactionNatureService {

    public TransactionNature identify(String description) {

        if (description == null || description.isBlank()) {
            return TransactionNature.UNKNOWN;
        }

        String normalized = normalize(description);

        if (normalized.startsWith("DEV PIX")
                || normalized.startsWith("ESTORNO")
                || normalized.startsWith("DEVOLUCAO")) {
            return TransactionNature.REFUND;
        }

        if (normalized.startsWith("PIX TRANSF")) {
            return TransactionNature.TRANSFER;
        }

        return TransactionNature.UNKNOWN;
    }

    private String normalize(String description) {
        return Normalizer.normalize(description, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
    }
}
