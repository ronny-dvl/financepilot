package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.CategorySummaryResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import com.financepilot.backend.statement.parser.ItauStatementParser;
import com.financepilot.backend.statement.parser.SantanderStatementParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real PDF extraction, bank parsers and financial services; no mocks or catalog-derived oracles. */
class FinancialCategorizationIntegrationTest {
    private static final TransactionCategorizationService CATEGORIZATION =
            new TransactionCategorizationService(new ObjectMapper());
    private final TransactionProcessingService processing = new TransactionProcessingService(
            new PdfExtractionService(), List.of(new ItauStatementParser(), new SantanderStatementParser()),
            CATEGORIZATION, new TransactionNatureService(), new PaymentMethodService());
    private final CategorySummaryService categorySummary = new CategorySummaryService();
    private final FinancialSummaryService financialSummary = new FinancialSummaryService();

    // Phase 4.2 deliberately changes COMPRA DE ACOES AWS from TECNOLOGIA to OUTROS:
    // independent investment/AWS evidence conflicts; all phase 4.1 monetary expectations stay unchanged.
    @ParameterizedTest(name = "{0}, {1}: {2}/{3}")
    @CsvSource(delimiter = '|', textBlock = """
            TAR TED | -5.00 | TARIFAS_BANCARIAS | EXPENSE | false | 5 | 5 | 0 | 0
            BOLETO SABESP | -100.00 | CONTAS_ESSENCIAIS | EXPENSE | false | 100 | 100 | 0 | 0
            PGTO IPVA | -200.00 | IMPOSTOS_TRIBUTOS | EXPENSE | false | 200 | 200 | 0 | 0
            MENSALIDADE ESCOLAR | -300.00 | EDUCACAO | EXPENSE | false | 300 | 300 | 0 | 0
            COMPRA UBER | -30.00 | TRANSPORTE | EXPENSE | false | 30 | 30 | 0 | 0
            PIX QRS UBER MESMA TITULARIDADE | -50.00 | TRANSPORTE | TRANSFER | false | 50 | 0 | 0 | 50
            PIX QRS UBER MESMA TITULARIDADE | 50.00 | TRANSPORTE | TRANSFER | false | 0 | 0 | 0 | 0
            PIX DEVOLVIDO UBER TRIP | -30.00 | TRANSPORTE | REFUND | true | 30 | 0 | 30 | 0
            PIX DEVOLVIDO UBER TRIP | 30.00 | TRANSPORTE | REFUND | false | 0 | 0 | 0 | 0
            APLIC CDB | -1000.00 | INVESTIMENTOS | UNKNOWN | true | 1000 | 0 | 1000 | 0
            COMPRA DE ACOES AWS | -1000.00 | OUTROS | UNKNOWN | true | 1000 | 0 | 1000 | 0
            COMPRA ASUS DROGASIL | -30.00 | OUTROS | EXPENSE | true | 30 | 0 | 30 | 0
            COMPRA ASUS PETZ | -30.00 | OUTROS | EXPENSE | true | 30 | 0 | 30 | 0
            PIX QRS UBER MESMA TITULARIDADE ASUS | -50.00 | OUTROS | TRANSFER | true | 50 | 0 | 0 | 50
            COMPRA 99 FOOD NIKE | -30.00 | OUTROS | EXPENSE | true | 30 | 0 | 30 | 0
            COMPRA 99FOOD NIKE | -30.00 | OUTROS | EXPENSE | true | 30 | 0 | 30 | 0
            COMPRA AMAZONMARKETPLACE AWS | -30.00 | OUTROS | EXPENSE | true | 30 | 0 | 30 | 0
            COMPRA AMAZON MARKETPLACE AMAZONWEBSERVICES | -30.00 | OUTROS | EXPENSE | true | 30 | 0 | 30 | 0
            RSCSS RENNER | -30.00 | VESTUARIO | EXPENSE | false | 30 | 30 | 0 | 0
            PIX QRS HIROTA FOOD | -30.00 | SUPERMERCADO | EXPENSE | false | 30 | 30 | 0 | 0
            RSCSS RENNER | 30.00 | VESTUARIO | UNKNOWN | true | 0 | 0 | 0 | 0
            RSCSS RENNER AGENDADO | -30.00 | VESTUARIO | UNKNOWN | true | 30 | 0 | 30 | 0
            PIX ENVIADO CARLOS RENNER | -30.00 | OUTROS | UNKNOWN | true | 30 | 0 | 30 | 0
            COMPRA UBER | 30.00 | TRANSPORTE | UNKNOWN | true | 0 | 0 | 0 | 0
            UBER | -30.00 | TRANSPORTE | UNKNOWN | true | 30 | 0 | 30 | 0
            BOLETO SABESP AGENDADO | -100.00 | CONTAS_ESSENCIAIS | UNKNOWN | true | 100 | 0 | 100 | 0
            PIX ENVIADO JOAO SILVA | -40.00 | OUTROS | UNKNOWN | true | 40 | 0 | 40 | 0
            COMPRA CLIENTE TESTE | -15.00 | OUTROS | EXPENSE | true | 15 | 0 | 15 | 0
            PIX ENVIADO CARLOS FERRETTO | -30 | OUTROS | UNKNOWN | true | 30 | 0 | 30 | 0
            PIX ENVIADO JOAO FUJIOKA | -30 | OUTROS | UNKNOWN | true | 30 | 0 | 30 | 0
            PAGAMENTO FERRETTO CURSOS ON LINE LTDA | -30 | EDUCACAO | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA FUJIOKA | -30 | TECNOLOGIA | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA FUJIOKA NOTEBOOK | -30 | TECNOLOGIA | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA APPLE.COM/BILL | -30 | ASSINATURAS | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA APPLE.COM/BILL ASSINATURA | -30 | ASSINATURAS | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA APPLE.COM/BILL ASSINATURA | 30 | ASSINATURAS | UNKNOWN | true | 0 | 0 | 0 | 0
            COMPRA CARREFOUR | -30 | SUPERMERCADO | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA CARREFOUR ALIMENTOS | -30 | SUPERMERCADO | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA SAMSUNG GELADEIRA | -30 | TECNOLOGIA | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA POSTO SHELL | -30 | TRANSPORTE | EXPENSE | false | 30 | 30 | 0 | 0
            COMPRA POSTO SHELL GASOLINA | -30 | TRANSPORTE | EXPENSE | false | 30 | 30 | 0 | 0
            PIX DEVOLVIDO UBER | -30 | TRANSPORTE | REFUND | true | 30 | 0 | 30 | 0
            PIX DEVOLVIDO UBER | 30 | TRANSPORTE | REFUND | false | 0 | 0 | 0 | 0
            PIX QRS APPLE.COM/BILL MESMA TITULARIDADE | -30 | ASSINATURAS | TRANSFER | false | 30 | 0 | 0 | 30
            PIX ENVIADO FERRETTO | -30 | EDUCACAO | EXPENSE | false | 30 | 30 | 0 | 0
            PIX ENVIADO UBER | 30 | TRANSPORTE | UNKNOWN | true | 0 | 0 | 0 | 0
            PIX ENVIADO UBER MESMA TITULARIDADE | -30 | TRANSPORTE | TRANSFER | false | 30 | 0 | 0 | 30
            PIX RECEBIDO CARREFOUR | 30 | SUPERMERCADO | UNKNOWN | true | 0 | 0 | 0 | 0
            PIX ENVIADO NETFLIX | -30 | ASSINATURAS | EXPENSE | false | 30 | 30 | 0 | 0
            PIX ENVIADO MERCADO LIVRE | -30 | COMPRAS | EXPENSE | false | 30 | 30 | 0 | 0
            PIX ENVIADO PETZ | -30.15 | PETS | EXPENSE | false | 30.15 | 30.15 | 0 | 0
            """)
    void shouldReconcileRealProcessingWithExplicitFinancialExpectations(String description, BigDecimal amount,
            TransactionCategory category, TransactionNature nature, boolean requiresReview,
            BigDecimal outflow, BigDecimal consumption, BigDecimal residual, BigDecimal transfers) throws IOException {
        var statement = processing.processStatement(itauPdf(movement(description, amount)));
        assertEquals(1, statement.transactions().size());
        TransactionResponse transaction = statement.transactions().getFirst();
        assertEquals(category, transaction.category());
        assertEquals(nature, transaction.nature());
        assertEquals(requiresReview, transaction.requiresReview());
        assertEquals(category == TransactionCategory.OUTROS ? CategorizationSource.UNKNOWN
                : CategorizationSource.KEYWORD_RULE, transaction.source());
        assertEquals(amount.signum() < 0 ? "EXPENSE" : "INCOME", transaction.type());
        assertMoney(amount, transaction.amount());

        CategorySummaryResponse summary = categorySummary.calculate(statement.transactions());
        assertMoney(outflow, summary.totalOutflow());
        assertMoney(consumption, summary.categorizedExpenses());
        assertMoney(residual, summary.uncategorizedOutflow());
        assertMoney(transfers, summary.transferOutflow());
        assertReconciled(summary);
        if (consumption.signum() > 0) {
            assertEquals(1, summary.expensesByCategory().size());
            assertMoney(consumption, summary.expensesByCategory().get(category));
        } else {
            assertTrue(summary.expensesByCategory().isEmpty());
        }
    }

    // Commercial defaults change categories, not the gross-consumption and cash-flow invariants.
    @Test
    void shouldSeparateCashFlowFromGrossConsumptionWithoutDoubleCounting() throws IOException {
        var statement = processing.processStatement(itauPdf(
                "26/11/2026 TAR TED -5,00",
                "26/11/2026 BOLETO SABESP -100,00",
                "26/11/2026 PGTO IPVA -200,00",
                "26/11/2026 MENSALIDADE ESCOLAR -300,00",
                "26/11/2026 COMPRA UBER -30,00",
                "26/11/2026 PIX QRS UBER MESMA TITULARIDADE -50,00",
                "26/11/2026 PIX DEVOLVIDO UBER TRIP -30,00",
                "26/11/2026 APLIC CDB -1000,00",
                "26/11/2026 PIX ENVIADO JOAO SILVA -40,00",
                "26/11/2026 PIX DEVOLVIDO UBER TRIP 30,00",
                "26/11/2026 CREDITO DE SALARIO EMPRESA 2000,00",
                "26/11/2026 COMPRA UBER 15,00"));
        CategorySummaryResponse summary = categorySummary.calculate(statement.transactions());
        var flow = financialSummary.calculate(statement.transactions());

        assertEquals(12, flow.transactionCount());
        assertMoney("2045", flow.totalIncome());
        assertMoney("1755", flow.totalExpenses());
        assertMoney("290", flow.netBalance());
        assertMoney("1755", summary.totalOutflow());
        assertMoney("635", summary.categorizedExpenses());
        assertMoney("1070", summary.uncategorizedOutflow());
        assertMoney("50", summary.transferOutflow());
        assertEquals(5, summary.expensesByCategory().size());
        assertMoney("5", summary.expensesByCategory().get(TransactionCategory.TARIFAS_BANCARIAS));
        assertMoney("100", summary.expensesByCategory().get(TransactionCategory.CONTAS_ESSENCIAIS));
        assertMoney("200", summary.expensesByCategory().get(TransactionCategory.IMPOSTOS_TRIBUTOS));
        assertMoney("300", summary.expensesByCategory().get(TransactionCategory.EDUCACAO));
        // The positive refund does not offset the original purchase: policy remains gross consumption.
        assertMoney("30", summary.expensesByCategory().get(TransactionCategory.TRANSPORTE));
        assertReconciled(summary);
    }

    @Test
    void shouldPreserveSantanderPixPurchasesAndReconcileRefundsWithRealParser() throws IOException {
        var statement = processing.processStatement(pdf(List.of(
                "SANTANDER", "DETALHE DOS MOVIMENTOS", "DATA DESCRIÇÃO VALOR SALDO",
                "Pix enviado", "26/11/2026 -R$ 30,00 R$ 100,00", "Uber Trip",
                "Pix devolvido", "26/11/2026 -R$ 10,00 R$ 90,00", "Uber Trip",
                "Pix devolvido", "26/11/2026 R$ 5,00 R$ 95,00", "Uber Trip")));
        var transactions = statement.transactions();
        assertEquals(3, transactions.size());
        assertEquals(TransactionCategory.TRANSPORTE, transactions.getFirst().category());
        assertEquals(TransactionNature.EXPENSE, transactions.getFirst().nature());
        assertFalse(transactions.getFirst().requiresReview());
        assertEquals(TransactionNature.REFUND, transactions.get(1).nature());
        assertTrue(transactions.get(1).requiresReview());
        assertEquals(TransactionNature.REFUND, transactions.get(2).nature());
        assertFalse(transactions.get(2).requiresReview());
        var summary = categorySummary.calculate(transactions);
        assertMoney("40", summary.totalOutflow());
        assertMoney("30", summary.categorizedExpenses());
        assertMoney("10", summary.uncategorizedOutflow());
        assertMoney("0", summary.transferOutflow());
        assertReconciled(summary);
    }

    @Test
    void shouldKeepSantanderPixToUnqualifiedPersonalNameInResidualOutflow() throws IOException {
        var statement = processing.processStatement(pdf(List.of(
                "SANTANDER", "DETALHE DOS MOVIMENTOS", "DATA DESCRIÇÃO VALOR SALDO",
                "Pix enviado", "26/11/2026 -R$ 30,00 R$ 100,00", "Carlos Ferretto")));
        var transaction = statement.transactions().getFirst();
        assertEquals(TransactionCategory.OUTROS, transaction.category());
        assertEquals(TransactionNature.UNKNOWN, transaction.nature());
        assertTrue(transaction.requiresReview());
        var summary = categorySummary.calculate(statement.transactions());
        assertMoney("30", summary.totalOutflow());
        assertMoney("0", summary.categorizedExpenses());
        assertMoney("30", summary.uncategorizedOutflow());
        assertMoney("0", summary.transferOutflow());
        assertReconciled(summary);
    }

    private static void assertReconciled(CategorySummaryResponse summary) {
        assertMoney(summary.totalOutflow(), summary.categorizedExpenses()
                .add(summary.uncategorizedOutflow()).add(summary.transferOutflow()));
        assertMoney(summary.categorizedExpenses(), summary.expensesByCategory().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertMoney(new BigDecimal(expected), actual);
    }

    private static void assertMoney(BigDecimal expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, expected.compareTo(actual));
    }

    private static String movement(String description, BigDecimal amount) {
        return "26/11/2026 " + description + " " + amount.setScale(2).toPlainString().replace('.', ',');
    }

    private static MockMultipartFile itauPdf(String... movements) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("ITAU - EXTRATO SINTETICO");
        lines.addAll(List.of(movements));
        return pdf(lines);
    }

    private static MockMultipartFile pdf(List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.setLeading(16);
                content.newLineAtOffset(40, 740);
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            document.save(output);
            return new MockMultipartFile("file", "synthetic.pdf", "application/pdf", output.toByteArray());
        }
    }
}
