
package com.financepilot.backend.statement.parser;

import com.financepilot.backend.statement.dto.TransactionResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ItauStatementParser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern TRANSACTION_PATTERN =
            Pattern.compile(
                    "^(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+(-?[\\d.]+,\\d{2})$"
            );

    private static final Pattern TRAILING_DATE =
            Pattern.compile("(?:\\s*\\d{2}/\\d{2}|\\d{4})$");

    public List<TransactionResponse> parse(String text) {

        List<TransactionResponse> transactions = new ArrayList<>();

        for (String line : text.split("\\R")) {

            String trimmedLine = line.trim();

            if (trimmedLine.contains("SALDO DO DIA")) {
                continue;
            }

            Matcher matcher = TRANSACTION_PATTERN.matcher(trimmedLine);

            if (!matcher.matches()) {
                continue;
            }

            LocalDate date = LocalDate.parse(
                    matcher.group(1), DATE_FORMAT
            );

            String description = matcher.group(2).trim();

            description = TRAILING_DATE.matcher(description)
                    .replaceFirst("")
                    .trim();

            String amountText = matcher.group(3)
                    .replace(".", "")
                    .replace(",", ".");

            BigDecimal amount = new BigDecimal(amountText);

            String type = amount.signum() < 0
                    ? "EXPENSE"
                    : "INCOME";

            transactions.add(new TransactionResponse(
                    date,
                    description,
                    amount,
                    type
            ));
        }

        return transactions;
    }
}
