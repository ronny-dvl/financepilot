
package com.financepilot.backend.statement.dto;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.PaymentMethod;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        LocalDate date,
        String description,
        BigDecimal amount,
        String type,
        TransactionCategory category,
        CategorizationSource source,
        boolean requiresReview,
        TransactionNature nature,
        PaymentMethod paymentMethod
) {

    // Construtor utilizado pelos parsers bancários
    public TransactionResponse(
            LocalDate date,
            String description,
            BigDecimal amount,
            String type
    ) {
        this(
                date, description, amount, type,
                TransactionCategory.OUTROS,
                CategorizationSource.UNKNOWN,
                true,
                TransactionNature.UNKNOWN,
                PaymentMethod.UNKNOWN
        );
    }

    // Construtor com categorização
    public TransactionResponse(
            LocalDate date,
            String description,
            BigDecimal amount,
            String type,
            TransactionCategory category,
            CategorizationSource source,
            boolean requiresReview
    ) {
        this(
                date, description, amount, type,
                category, source, requiresReview,
                TransactionNature.UNKNOWN,
                PaymentMethod.UNKNOWN
        );
    }

    // Construtor de compatibilidade com a versão anterior
    public TransactionResponse(
            LocalDate date,
            String description,
            BigDecimal amount,
            String type,
            TransactionCategory category,
            CategorizationSource source,
            boolean requiresReview,
            TransactionNature nature
    ) {
        this(
                date, description, amount, type,
                category, source, requiresReview,
                nature,
                PaymentMethod.UNKNOWN
        );
    }
}
