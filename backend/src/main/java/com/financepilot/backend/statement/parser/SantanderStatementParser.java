
package com.financepilot.backend.statement.parser;

import com.financepilot.backend.statement.dto.TransactionResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SantanderStatementParser implements BankStatementParser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String MONEY = "-?R\\$\\s*[\\d.]+,\\d{2}";

    private static final Pattern MOVEMENT_PATTERN = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4})\\s+(" + MONEY + ")\\s+(" + MONEY + ")$"
    );

    private static final Pattern PAGE_NUMBER_PATTERN =
            Pattern.compile("^\\d+/\\d+$");

    private static final Pattern OPERATION_PATTERN = Pattern.compile(
            "^(Pix enviado|Pix recebido|Pix devolvido|" +
                    "Credito de salario|Crédito de salário|" +
                    "Iof adicional - automatico|IOF adicional - automático)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    @Override
    public boolean supports(String text) {

        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.toUpperCase(Locale.ROOT);

        return normalized.contains("DETALHE DOS MOVIMENTOS")
                && normalized.contains("PIX ENVIADO")
                && normalized.contains("DATA DESCRIÇÃO VALOR SALDO");
    }

    @Override
    public List<TransactionResponse> parse(String text) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> lines = Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        List<TransactionResponse> transactions = new ArrayList<>();

        boolean insideMovements = false;

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);

            if (line.equalsIgnoreCase("DETALHE DOS MOVIMENTOS")) {
                insideMovements = true;
                continue;
            }

            if (!insideMovements) {
                continue;
            }

            if (!OPERATION_PATTERN.matcher(line).matches()) {
                continue;
            }

            if (i + 2 >= lines.size()) {
                throw new IllegalArgumentException(
                        "Movimentação Santander incompleta: " + line
                );
            }

            Matcher matcher = MOVEMENT_PATTERN.matcher(lines.get(i + 1));

            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        "Data ou valores inválidos após: " + line
                );
            }

            LocalDate date = LocalDate.parse(
                    matcher.group(1),
                    DATE_FORMAT
            );

            BigDecimal amount = parseMoney(matcher.group(2));

            // O terceiro grupo representa o saldo bancário.
            // Ele não deve ser contabilizado como transação.

            String merchant = lines.get(i + 2);

            if (merchant.isBlank()
                    || PAGE_NUMBER_PATTERN.matcher(merchant).matches()
                    || merchant.equalsIgnoreCase("DETALHE DOS MOVIMENTOS")) {
                throw new IllegalArgumentException(
                        "Descrição da movimentação ausente: " + line
                );
            }

            String description = line + " - " + merchant;

            String type = amount.signum() < 0
                    ? "EXPENSE"
                    : "INCOME";

            transactions.add(new TransactionResponse(
                    date,
                    description,
                    amount,
                    type
            ));

            i += 2;
        }

        return transactions;
    }

    private BigDecimal parseMoney(String value) {
        return new BigDecimal(
                value.replace("R$", "")
                        .replaceAll("\\s+", "")
                        .replace(".", "")
                        .replace(",", ".")
        );
    }
}
