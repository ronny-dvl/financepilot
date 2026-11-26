package com.financepilot.backend.statement.dto;

import java.math.BigDecimal;
import java.util.List;

/** Indexes reference the unchanged transactions array in the analysis response. */
public record PixRecipientResponse(String name, BigDecimal totalSent, int transferCount,
        List<Integer> transactionIndexes) {
    public PixRecipientResponse { transactionIndexes = List.copyOf(transactionIndexes); }

    @Override
    public String toString() {
        return "PixRecipientResponse[transferCount=" + transferCount + "]";
    }
}
