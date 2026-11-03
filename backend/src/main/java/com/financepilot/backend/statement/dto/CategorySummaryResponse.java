
package com.financepilot.backend.statement.dto;

import com.financepilot.backend.statement.domain.TransactionCategory;

import java.math.BigDecimal;
import java.util.Map;

public record CategorySummaryResponse(
        BigDecimal totalOutflow,
        BigDecimal categorizedExpenses,
        BigDecimal uncategorizedOutflow,
        BigDecimal transferOutflow,
        Map<TransactionCategory, BigDecimal> expensesByCategory
) {
}
