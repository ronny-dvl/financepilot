
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.PaymentMethod;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.CategorySummaryResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategorySummaryServiceTest {

    private final CategorySummaryService service =
            new CategorySummaryService();

    @Test
    void shouldCalculateCategorySummary() {

        List<TransactionResponse> transactions = List.of(
                transaction("-25.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.EXPENSE, false),

                transaction("-100.00", TransactionCategory.ALIMENTACAO,
                        TransactionNature.EXPENSE, false),

                transaction("-15.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.EXPENSE, false),

                transaction("-50.00", TransactionCategory.OUTROS,
                        TransactionNature.TRANSFER, true),

                transaction("10.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.REFUND, false)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertAmount("190.00", result.totalOutflow());
        assertAmount("140.00", result.categorizedExpenses());
        assertAmount("0.00", result.uncategorizedOutflow());
        assertAmount("50.00", result.transferOutflow());

        assertAmount(
                "40.00",
                result.expensesByCategory()
                        .get(TransactionCategory.TRANSPORTE)
        );

        assertAmount(
                "100.00",
                result.expensesByCategory()
                        .get(TransactionCategory.ALIMENTACAO)
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

        assertAmount("30.00", result.totalOutflow());
        assertAmount("30.00", result.uncategorizedOutflow());
        assertAmount("0.00", result.categorizedExpenses());
        assertAmount("0.00", result.transferOutflow());
    }

    @Test
    void shouldCountPixPurchaseAsCategorizedExpense() {

        List<TransactionResponse> transactions = List.of(
                transaction("-45.90", TransactionCategory.ALIMENTACAO,
                        TransactionNature.EXPENSE, false)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertAmount("45.90", result.totalOutflow());
        assertAmount("45.90", result.categorizedExpenses());
        assertAmount("0.00", result.transferOutflow());

        assertAmount(
                "45.90",
                result.expensesByCategory()
                        .get(TransactionCategory.ALIMENTACAO)
        );
    }

    @Test
    void shouldNotCountInternalTransferAsExpense() {

        List<TransactionResponse> transactions = List.of(
                transaction("-250.00", TransactionCategory.OUTROS,
                        TransactionNature.TRANSFER, true)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertAmount("250.00", result.totalOutflow());
        assertAmount("250.00", result.transferOutflow());
        assertAmount("0.00", result.categorizedExpenses());
        assertAmount("0.00", result.uncategorizedOutflow());

        assertTrue(result.expensesByCategory().isEmpty());
    }

    @Test
    void shouldKeepUnknownNatureOutOfCategorizedExpenses() {

        List<TransactionResponse> transactions = List.of(
                transaction("-80.00", TransactionCategory.ALIMENTACAO,
                        TransactionNature.UNKNOWN, false)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertAmount("80.00", result.totalOutflow());
        assertAmount("80.00", result.uncategorizedOutflow());
        assertAmount("0.00", result.categorizedExpenses());

        assertTrue(result.expensesByCategory().isEmpty());
    }

    @Test
    void shouldKeepTransactionsRequiringReviewUncategorized() {

        List<TransactionResponse> transactions = List.of(
                transaction("-60.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.EXPENSE, true)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertAmount("60.00", result.totalOutflow());
        assertAmount("60.00", result.uncategorizedOutflow());
        assertAmount("0.00", result.categorizedExpenses());
    }

    @Test
    void shouldIgnorePositiveRefundInOutflow() {

        List<TransactionResponse> transactions = List.of(
                transaction("25.00", TransactionCategory.TRANSPORTE,
                        TransactionNature.REFUND, false)
        );

        CategorySummaryResponse result =
                service.calculate(transactions);

        assertAmount("0.00", result.totalOutflow());
        assertAmount("0.00", result.categorizedExpenses());
        assertAmount("0.00", result.uncategorizedOutflow());
        assertAmount("0.00", result.transferOutflow());
    }

    @Test
    void shouldHandleEmptyTransactionList() {

        CategorySummaryResponse result =
                service.calculate(List.of());

        assertAmount("0.00", result.totalOutflow());
        assertAmount("0.00", result.categorizedExpenses());
        assertAmount("0.00", result.uncategorizedOutflow());
        assertAmount("0.00", result.transferOutflow());

        assertTrue(result.expensesByCategory().isEmpty());
    }

    private TransactionResponse transaction(
            String amount,
            TransactionCategory category,
            TransactionNature nature,
            boolean requiresReview
    ) {

        BigDecimal value = new BigDecimal(amount);

        return new TransactionResponse(
                LocalDate.of(2026, 10, 29),
                "TEST TRANSACTION",
                value,
                value.signum() < 0 ? "EXPENSE" : "INCOME",
                category,
                CategorizationSource.KEYWORD_RULE,
                requiresReview,
                nature,
                PaymentMethod.UNKNOWN
        );
    }

    private void assertAmount(
            String expected,
            BigDecimal actual
    ) {
        assertNotNull(actual);

        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual)
        );
    }
}
