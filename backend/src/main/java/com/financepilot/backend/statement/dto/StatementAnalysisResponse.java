package com.financepilot.backend.statement.dto;

import java.util.List;

public record StatementAnalysisResponse(
        FinancialSummaryResponse summary,
        List<TransactionResponse> transactions,
        CategorySummaryResponse categorySummary,
        String holderName
) {
    public StatementAnalysisResponse(FinancialSummaryResponse summary,
            List<TransactionResponse> transactions, CategorySummaryResponse categorySummary) {
        this(summary, transactions, categorySummary, null);
    }

    @Override
    public String toString() {
        return "StatementAnalysisResponse[transactionCount=" + summary.transactionCount() + "]";
    }
}
