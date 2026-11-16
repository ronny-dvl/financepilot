
package com.financepilot.backend.statement.parser;

import com.financepilot.backend.statement.dto.TransactionResponse;

import java.util.List;
import java.util.Optional;

public interface BankStatementParser {

    boolean supports(String text);

    List<TransactionResponse> parse(String text);

    default Optional<String> extractHolderName(String text) {
        return Optional.empty();
    }
}
