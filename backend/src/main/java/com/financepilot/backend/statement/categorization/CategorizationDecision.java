package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.dto.CategorizationResult;
import java.util.List;

/** Internal explanation: contains stable catalog IDs, never the financial description. */
public record CategorizationDecision(CategorizationResult result, String ruleId, List<String> conflictingRuleIds) {
    public CategorizationDecision { conflictingRuleIds = List.copyOf(conflictingRuleIds); }
}
