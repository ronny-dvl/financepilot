package com.financepilot.backend.statement.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipientSummaryResponse(BigDecimal totalSent, int recipientCount, int transferCount,
        List<PixRecipientResponse> recipients) {
    public RecipientSummaryResponse { recipients = List.copyOf(recipients); }

    public static RecipientSummaryResponse empty() {
        return new RecipientSummaryResponse(BigDecimal.ZERO, 0, 0, List.of());
    }

    @Override
    public String toString() {
        return "RecipientSummaryResponse[recipientCount=" + recipientCount + ", transferCount=" + transferCount + "]";
    }
}
