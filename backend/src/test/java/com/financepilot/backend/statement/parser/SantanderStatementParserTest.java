
package com.financepilot.backend.statement.parser;

import com.financepilot.backend.statement.dto.TransactionResponse;
import com.financepilot.backend.statement.service.PdfExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SantanderStatementParserTest {

    private final SantanderStatementParser parser =
            new SantanderStatementParser();

    @Test
    void shouldParseSantanderTransactions() {

        String text = """
                EXTRATO DE CONTA
                Saldo inicial Entradas Saídas Saldo final
                R$ 100,00 R$ 15,00 R$ 5,40 R$ 109,60
                DETALHE DOS MOVIMENTOS
                Data Descrição Valor Saldo
                Terça, 27 de outubro de 2026
                Pix enviado
                27/10/2026 -R$ 5,40 R$ 155,44
                Mercadinho Ilha Bela
                Pix recebido
                27/10/2026 R$ 15,00 R$ 160,84
                Cliente Exemplo
                1/12
                """;

        List<TransactionResponse> transactions = parser.parse(text);

        assertEquals(2, transactions.size());

        TransactionResponse expense = transactions.get(0);

        assertEquals(LocalDate.of(2026, 10, 27), expense.date());
        assertEquals(
                "Pix enviado - Mercadinho Ilha Bela",
                expense.description()
        );
        assertEquals(new BigDecimal("-5.40"), expense.amount());
        assertEquals("EXPENSE", expense.type());

        TransactionResponse income = transactions.get(1);

        assertEquals(new BigDecimal("15.00"), income.amount());
        assertEquals("INCOME", income.type());
    }

    @Test
    void shouldIgnoreBalanceAndHandleNegativeBalance() {

        String text = """
                DETALHE DOS MOVIMENTOS
                Pix enviado
                31/08/2026 -R$ 605,00 -R$ 31,27
                Destinatario Exemplo
                """;

        List<TransactionResponse> transactions = parser.parse(text);

        assertEquals(1, transactions.size());
        assertEquals(
                new BigDecimal("-605.00"),
                transactions.get(0).amount()
        );
    }

    @Test
    void shouldParseSalaryAndRefund() {

        String text = """
                DETALHE DOS MOVIMENTOS
                Credito de salario
                15/10/2026 R$ 423,66 R$ 457,85
                Empresa Exemplo
                Pix devolvido
                21/09/2026 R$ 8,90 R$ 187,87
                Uber Do Brasil Tecnol
                """;

        List<TransactionResponse> transactions = parser.parse(text);

        assertEquals(2, transactions.size());
        assertEquals("INCOME", transactions.get(0).type());
        assertEquals(
                new BigDecimal("8.90"),
                transactions.get(1).amount()
        );
    }

    @Test
    void shouldRejectIncompleteTransaction() {

        String text = """
                DETALHE DOS MOVIMENTOS
                Pix enviado
                27/10/2026 -R$ 5,40 R$ 155,44
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(text)
        );
    }

    @Test
    void shouldParseRealSantanderStatement() throws Exception {

        // Caminho relativo à pasta backend
        Path pdfPath = Path.of(
                "src/test/resources/santander_extrato.pdf"
        );

        assertTrue(
                Files.exists(pdfPath),
                "PDF de teste não encontrado: " +
                        pdfPath.toAbsolutePath()
        );

        byte[] pdfBytes = Files.readAllBytes(pdfPath);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "santander_extrato.pdf",
                "application/pdf",
                pdfBytes
        );

        // Extrai o texto utilizando o serviço real
        PdfExtractionService pdfService =
                new PdfExtractionService();

        String extractedText = pdfService.extract(file).text();

        assertNotNull(extractedText);
        assertFalse(extractedText.isBlank());

        // Converte o texto em transações
        List<TransactionResponse> transactions =
                parser.parse(extractedText);

        // Calcula as entradas
        BigDecimal totalIncome = transactions.stream()
                .map(TransactionResponse::amount)
                .filter(amount -> amount.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcula as saídas, convertendo para valor positivo
        BigDecimal totalExpense = transactions.stream()
                .map(TransactionResponse::amount)
                .filter(amount -> amount.signum() < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Confere os totais do extrato Santander
        assertEquals(
                0,
                new BigDecimal("5721.98").compareTo(totalIncome),
                "O total de entradas não corresponde ao extrato"
        );

        assertEquals(
                0,
                new BigDecimal("5680.82").compareTo(totalExpense),
                "O total de saídas não corresponde ao extrato"
        );

        System.out.println("=== SANTANDER ===");
        System.out.println("Transações: " + transactions.size());
        System.out.println("Entradas: R$ " + totalIncome);
        System.out.println("Saídas: R$ " + totalExpense);
    }
}
