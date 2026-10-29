package com.financepilot.backend.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        LocalDate date,
        String description,
        BigDecimal amount,
        String type
) {
}