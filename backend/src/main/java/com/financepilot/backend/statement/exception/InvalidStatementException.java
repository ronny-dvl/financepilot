package com.financepilot.backend.statement.exception;

public class InvalidStatementException extends RuntimeException {
    public InvalidStatementException(String message) {
        super(message);
    }
}
