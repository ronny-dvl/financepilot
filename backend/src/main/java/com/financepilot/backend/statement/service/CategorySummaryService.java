
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

            // 1. Ignora entradas e valores inválidos
            if (transaction.amount() == null
                    || transaction.amount().signum() >= 0) {
                continue;
            }

            BigDecimal amount = transaction.amount().abs();

            // 2. Toda saída compõe o fluxo de saída
            totalOutflow = totalOutflow.add(amount);

            TransactionNature nature = transaction.nature();
            TransactionCategory category = transaction.category();

            // 3. Transferências internas confirmadas
            if (nature == TransactionNature.TRANSFER) {
                transferOutflow = transferOutflow.add(amount);
                continue;
            }

            // 4. Estornos não são tratados como despesas
            if (nature == TransactionNature.REFUND) {
                continue;
            }

            // 5. Operações sem classificação confiável
            if (category == null
                    || category == TransactionCategory.OUTROS
                    || transaction.requiresReview()
                    || nature == TransactionNature.UNKNOWN) {

                uncategorizedOutflow =
                        uncategorizedOutflow.add(amount);

                continue;
            }

            // 6. Despesas categorizadas
            if (nature == TransactionNature.EXPENSE) {

                categorizedExpenses =
                        categorizedExpenses.add(amount);

                expensesByCategory.merge(
                        category,
                        amount,
                        BigDecimal::add
                );

            } else {

                // Outras saídas não confirmadas como despesas
                uncategorizedOutflow =
                        uncategorizedOutflow.add(amount);
            }
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
