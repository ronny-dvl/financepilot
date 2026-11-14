package com.financepilot.backend.statement.dto;

import java.util.List;

public record StatementAnalysisResponse(
        FinancialSummaryResponse summary,
        List<TransactionResponse> transactions,
        CategorySummaryResponse categorySummary
) {
}
