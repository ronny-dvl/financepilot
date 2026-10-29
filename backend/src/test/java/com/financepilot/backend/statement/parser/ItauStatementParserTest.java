
package com.financepilot.backend.statement.parser;

import com.financepilot.backend.statement.dto.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItauStatementParserTest {

    private final ItauStatementParser parser =
            new ItauStatementParser();

    @Test
    void shouldParseTransactionsAndIgnoreBalance() {

        String text = """
                26/10/2026 PIX QRS SUPERMERCAD24/10 -28,91
                26/10/2026 SALDO DO DIA 202,71
                13/10/2026 PIX TRANSF APOLLO 10/10 0,24
                """;

        List<TransactionResponse> transactions = parser.parse(text);

        assertEquals(2, transactions.size());

        TransactionResponse expense = transactions.get(0);

        assertEquals("PIX QRS SUPERMERCAD", expense.description());
        assertEquals(new BigDecimal("-28.91"), expense.amount());
        assertEquals("EXPENSE", expense.type());

        TransactionResponse income = transactions.get(1);

        assertEquals(new BigDecimal("0.24"), income.amount());
        assertEquals("INCOME", income.type());
    }
}
