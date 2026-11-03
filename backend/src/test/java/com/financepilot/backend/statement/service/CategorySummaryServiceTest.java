
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.*;
import com.financepilot.backend.statement.dto.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategorySummaryServiceTest {

    private final CategorySummaryService service =
            new CategorySummaryService();

    @Test
    void shouldCalculateCategorySummary() {

        List<TransactionResponse> transactions = List.of(
                transaction("-25.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.UNKNOWN, false),

                transaction("-100.00", TransactionCategory.ALIMENTACAO,
                        TransactionNature.UNKNOWN, false),

                transaction("-15.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.UNKNOWN, false),

                transaction("-50.00", TransactionCategory.OUTROS,
                        TransactionNature.TRANSFER, true),

                transaction("10.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.REFUND, false)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertEquals(
                0,
                new BigDecimal("190.00")
                        .compareTo(result.totalOutflow())
        );

        assertEquals(
                0,
                new BigDecimal("140.00")
                        .compareTo(result.categorizedExpenses())
        );

        assertEquals(
                0,
                new BigDecimal("50.00")
                        .compareTo(result.transferOutflow())
        );

        assertEquals(
                0,
                new BigDecimal("40.00")
                        .compareTo(result.expensesByCategory()
                                .get(TransactionCategory.TRANSPORTE))
        );
    }

    @Test
    void shouldSeparateUncategorizedOutflow() {

        List<TransactionResponse> transactions = List.of(
                transaction("-30.00", TransactionCategory.OUTROS,
                        TransactionNature.UNKNOWN, true)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertEquals(
                0,
                new BigDecimal("30.00")
                        .compareTo(result.uncategorizedOutflow())
        );
    }

    private TransactionResponse transaction(
            String amount,
            TransactionCategory category,
            TransactionNature nature,
            boolean requiresReview
    ) {
        return new TransactionResponse(
                LocalDate.of(2026, 10, 29),
                "TEST TRANSACTION",
                new BigDecimal(amount),
                new BigDecimal(amount).signum() < 0
                        ? "EXPENSE" : "INCOME",
                category,
                CategorizationSource.KEYWORD_RULE,
                requiresReview,
                nature
        );
    }
}
