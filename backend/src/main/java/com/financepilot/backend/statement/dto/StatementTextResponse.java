package com.financepilot.backend.statement.dto;

public record StatementTextResponse(
        String fileName,
        int pages,
        int characters,
        String text
) {
}