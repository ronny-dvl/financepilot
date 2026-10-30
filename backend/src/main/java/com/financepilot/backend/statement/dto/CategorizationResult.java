
package com.financepilot.backend.statement.dto;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;

public record CategorizationResult(
        TransactionCategory category,
        CategorizationSource source,
        boolean requiresReview
) {
}
