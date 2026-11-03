
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.CategorySummaryResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class CategorySummaryService {

    public CategorySummaryResponse calculate(
            List<TransactionResponse> transactions
    ) {

        BigDecimal totalOutflow = BigDecimal.ZERO;
        BigDecimal categorizedExpenses = BigDecimal.ZERO;
        BigDecimal uncategorizedOutflow = BigDecimal.ZERO;
        BigDecimal transferOutflow = BigDecimal.ZERO;

        Map<TransactionCategory, BigDecimal> expensesByCategory =
                new EnumMap<>(TransactionCategory.class);

        for (TransactionResponse transaction : transactions) {

            if (transaction.amount() == null
                    || transaction.amount().signum() >= 0) {
                continue;
            }

            BigDecimal amount = transaction.amount().abs();

            totalOutflow = totalOutflow.add(amount);

            if (transaction.nature() == TransactionNature.TRANSFER) {
                transferOutflow = transferOutflow.add(amount);
                continue;
            }

            if (transaction.nature() == TransactionNature.REFUND) {
                continue;
            }

            if (transaction.category() == null
                    || transaction.category() == TransactionCategory.OUTROS
                    || transaction.requiresReview()) {

                uncategorizedOutflow =
                        uncategorizedOutflow.add(amount);

                continue;
            }

            categorizedExpenses = categorizedExpenses.add(amount);

            expensesByCategory.merge(
                    transaction.category(),
                    amount,
                    BigDecimal::add
            );
        }

        return new CategorySummaryResponse(
                totalOutflow,
                categorizedExpenses,
                uncategorizedOutflow,
                transferOutflow,
                expensesByCategory
        );
    }
}
