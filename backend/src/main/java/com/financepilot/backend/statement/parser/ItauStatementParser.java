
package com.financepilot.backend.statement.parser;

import com.financepilot.backend.statement.dto.TransactionResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ItauStatementParser implements BankStatementParser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern TRANSACTION_PATTERN =
            Pattern.compile(
                    "^(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+(-?[\\d.]+,\\d{2})$"
            );

    private static final Pattern TRAILING_DATE =
            Pattern.compile("(?:\\s*\\d{2}/\\d{2}|\\d{4})$");

    // Identifica se o extrato pertence ao Itaú
    @Override
    public boolean supports(String text) {

        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.toUpperCase(Locale.ROOT);

        return normalized.contains("ITAÚ")
                || normalized.contains("ITAU");
    }

    // Extrai somente a identificação do titular no cabeçalho da conta.
    @Override
    public Optional<String> extractHolderName(String text) {
        if (!supports(text)) return Optional.empty();
        List<String> lines = HolderNameSupport.headerLines(text);
        List<String> candidates = new ArrayList<>();
        Pattern nameField = Pattern.compile(
                "(?iu)^(?:NOME(?: DO TITULAR| DO CLIENTE)?|TITULAR)\\h*:\\h*(.*)$"
        );
        Pattern accountHeader = Pattern.compile(
                "(?iu)^(.*?)\\bAG[ÊE]NCIA\\h*:?\\h*\\d[\\d.\\-]*\\h+"
                        + "CONTA(?:\\h+CORRENTE)?\\h*:?\\h*\\d[\\d.\\-]*\\h*$"
        );
        Pattern agencyField = Pattern.compile("(?iu)^AG[ÊE]NCIA\\h*:?\\h*\\d[\\d.\\-]*\\h*$");
        Pattern accountField = Pattern.compile("(?iu)^CONTA(?:\\h+CORRENTE)?\\h*:?\\h*\\d[\\d.\\-]*\\h*$");
        boolean hasAccountHeader = lines.stream().anyMatch(line -> accountHeader.matcher(line).matches())
                || (lines.stream().anyMatch(line -> agencyField.matcher(line).matches())
                    && lines.stream().anyMatch(line -> accountField.matcher(line).matches()));
        if (!hasAccountHeader) return Optional.empty();

        for (int i = 0; i < lines.size(); i++) {
            Matcher labeled = nameField.matcher(lines.get(i));
            if (labeled.matches()) {
                if (!labeled.group(1).isBlank()) candidates.add(labeled.group(1));
                else if (i + 1 < lines.size()) candidates.add(lines.get(i + 1));
            }
            Matcher account = accountHeader.matcher(lines.get(i));
            if (account.matches()) {
                if (!account.group(1).isBlank()) candidates.add(account.group(1));
                else if (i > 0 && !nameField.matcher(lines.get(i - 1)).matches()) {
                    // Unlabelled names are accepted only immediately beside the account header.
                    candidates.add(lines.get(i - 1));
                }
            }
            if (i > 0 && i + 1 < lines.size()
                    && agencyField.matcher(lines.get(i)).matches()
                    && accountField.matcher(lines.get(i + 1)).matches()) {
                candidates.add(lines.get(i - 1));
            }
        }
        return HolderNameSupport.uniqueName(candidates);
    }

    // Extrai as transações do extrato Itaú.
    @Override
    public List<TransactionResponse> parse(String text) {

        List<TransactionResponse> transactions = new ArrayList<>();

        for (String line : text.split("\\R")) {

            String trimmedLine = line.trim();

            if (trimmedLine.contains("SALDO DO DIA")) {
                continue;
            }

            Matcher matcher =
                    TRANSACTION_PATTERN.matcher(trimmedLine);

            if (!matcher.matches()) {
                continue;
            }

            LocalDate date = LocalDate.parse(
                    matcher.group(1),
                    DATE_FORMAT
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
