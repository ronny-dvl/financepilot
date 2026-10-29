
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.dto.FinancialSummaryResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FinancialSummaryService {

    public FinancialSummaryResponse calculate(
            List<TransactionResponse> transactions
    ) {

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (TransactionResponse transaction : transactions) {

            BigDecimal amount = transaction.amount();

            if (amount.signum() > 0) {
                totalIncome = totalIncome.add(amount);
            } else if (amount.signum() < 0) {
                totalExpenses = totalExpenses.add(amount.abs());
            }
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        return new FinancialSummaryResponse(
                totalIncome,
                totalExpenses,
                netBalance,
                transactions.size()
        );
    }
}
