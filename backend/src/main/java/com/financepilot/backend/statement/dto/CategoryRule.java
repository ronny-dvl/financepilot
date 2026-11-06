
package com.financepilot.backend.statement.dto;

import com.financepilot.backend.statement.domain.TransactionCategory;

public record CategoryRule(
        String keyword,
        TransactionCategory category,
        String matchType,
        int priority
) {
}
