
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.dto.FinancialSummaryResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialSummaryServiceTest {

    private final FinancialSummaryService service =
            new FinancialSummaryService();

    @Test
    void shouldCalculateFinancialSummary() {

        LocalDate date = LocalDate.of(2026, 10, 26);

        List<TransactionResponse> transactions = List.of(
                new TransactionResponse(
                        date, "PIX RECEBIDO",
                        new BigDecimal("1000.00"), "INCOME"
                ),
                new TransactionResponse(
                        date, "SUPERMERCADO",
                        new BigDecimal("-150.00"), "EXPENSE"
                ),
                new TransactionResponse(
                        date, "UBER",
                        new BigDecimal("-30.00"), "EXPENSE"
                )
        );

        FinancialSummaryResponse summary = service.calculate(transactions);

        assertEquals(new BigDecimal("1000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("180.00"), summary.totalExpenses());
        assertEquals(new BigDecimal("820.00"), summary.netBalance());
        assertEquals(3, summary.transactionCount());
    }
}
