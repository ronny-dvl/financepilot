
package com.financepilot.backend.statement.parser;

import com.financepilot.backend.statement.dto.TransactionResponse;

import java.util.List;

public interface BankStatementParser {

    boolean supports(String text);

    List<TransactionResponse> parse(String text);
}
