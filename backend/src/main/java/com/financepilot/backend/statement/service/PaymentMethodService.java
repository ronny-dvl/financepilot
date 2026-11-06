
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.PaymentMethod;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class PaymentMethodService {

    public PaymentMethod identify(String description) {

        if (description == null || description.isBlank()) {
            return PaymentMethod.UNKNOWN;
        }

        String normalized = normalize(description);

        if (normalized.startsWith("PIX ")
                || normalized.startsWith("DEV PIX")) {
            return PaymentMethod.PIX;
        }

        if (normalized.contains("CARTAO DE CREDITO")
                || normalized.contains("COMPRA CREDITO")) {
            return PaymentMethod.CREDIT_CARD;
        }

        if (normalized.contains("CARTAO DE DEBITO")
                || normalized.contains("COMPRA DEBITO")) {
            return PaymentMethod.DEBIT_CARD;
        }

        if (normalized.contains("TED")
                || normalized.contains("DOC TRANSFERENCIA")) {
            return PaymentMethod.BANK_TRANSFER;
        }

        if (normalized.contains("BOLETO")) {
            return PaymentMethod.BOLETO;
        }

        if (normalized.contains("SAQUE DINHEIRO")) {
            return PaymentMethod.CASH;
        }

        return PaymentMethod.UNKNOWN;
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
