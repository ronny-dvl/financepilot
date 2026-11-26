
package com.financepilot.backend.statement.controller;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.PaymentMethod;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;

import com.financepilot.backend.statement.dto.CategorySummaryResponse;
import com.financepilot.backend.statement.dto.FinancialSummaryResponse;
import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import com.financepilot.backend.statement.exception.InvalidPdfException;

import com.financepilot.backend.statement.service.CategorySummaryService;
import com.financepilot.backend.statement.service.FinancialSummaryService;
import com.financepilot.backend.statement.service.PdfExtractionService;
import com.financepilot.backend.statement.service.TransactionProcessingService;
import com.financepilot.backend.statement.service.RecipientSummaryService;
import com.financepilot.backend.statement.service.PixRecipientExtractionService;
import com.financepilot.backend.statement.service.TransactionProcessingService.ProcessedStatement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementController.class)
@Import({RecipientSummaryService.class, PixRecipientExtractionService.class})
class StatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PdfExtractionService pdfExtractionService;

    @MockitoBean
    private TransactionProcessingService transactionProcessingService;

    @MockitoBean
    private FinancialSummaryService financialSummaryService;

    @MockitoBean
    private CategorySummaryService categorySummaryService;

    @Test
    void shouldUploadPdfSuccessfully() throws Exception {

        MockMultipartFile file = createMockPdf();

        StatementTextResponse response = new StatementTextResponse(
                "extrato.pdf",
                1,
                20,
                "Sample statement text"
        );

        when(pdfExtractionService.extract(any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/statements/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName")
                        .value("extrato.pdf"))
                .andExpect(jsonPath("$.pages")
                        .value(1));
    }

    @Test
    void shouldReturnTransactionsSuccessfully() throws Exception {

        MockMultipartFile file = createMockPdf();

        TransactionResponse transaction = new TransactionResponse(
                LocalDate.of(2026, 10, 26),
                "PIX QRS SUPERMERCADO",
                new BigDecimal("-28.91"),
                "EXPENSE",
                TransactionCategory.SUPERMERCADO,
                CategorizationSource.KEYWORD_RULE,
                false,
                TransactionNature.EXPENSE,
                PaymentMethod.PIX
        );

        when(transactionProcessingService.process(any()))
                .thenReturn(List.of(transaction));

        mockMvc.perform(multipart("/api/statements/transactions")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date")
                        .value("2026-10-26"))
                .andExpect(jsonPath("$[0].description")
                        .value("PIX QRS SUPERMERCADO"))
                .andExpect(jsonPath("$[0].amount")
                        .value(-28.91))
                .andExpect(jsonPath("$[0].type")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$[0].category")
                        .value("SUPERMERCADO"))
                .andExpect(jsonPath("$[0].source")
                        .value("KEYWORD_RULE"))
                .andExpect(jsonPath("$[0].nature")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$[0].paymentMethod")
                        .value("PIX"))
                .andExpect(jsonPath("$[0].requiresReview")
                        .value(false));

        verify(transactionProcessingService).process(any());
    }

    @Test
    void shouldReturnFinancialSummarySuccessfully() throws Exception {

        MockMultipartFile file = createMockPdf();

        List<TransactionResponse> transactions = List.of(
                new TransactionResponse(
                        LocalDate.of(2026, 10, 26),
                        "SALARIO",
                        new BigDecimal("1500.00"),
                        "INCOME"
                ),
                new TransactionResponse(
                        LocalDate.of(2026, 10, 26),
                        "SUPERMERCADO",
                        new BigDecimal("-200.00"),
                        "EXPENSE"
                )
        );

        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                new BigDecimal("1500.00"),
                new BigDecimal("200.00"),
                new BigDecimal("1300.00"),
                2
        );

        when(transactionProcessingService.process(any()))
                .thenReturn(transactions);

        when(financialSummaryService.calculate(transactions))
                .thenReturn(summary);

        mockMvc.perform(multipart("/api/statements/summary")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome")
                        .value(1500.00))
                .andExpect(jsonPath("$.totalExpenses")
                        .value(200.00))
                .andExpect(jsonPath("$.netBalance")
                        .value(1300.00))
                .andExpect(jsonPath("$.transactionCount")
                        .value(2));

        verify(financialSummaryService).calculate(transactions);
    }

    @Test
    void shouldReturnCategorySummarySuccessfully() throws Exception {

        MockMultipartFile file = createMockPdf();

        List<TransactionResponse> transactions = List.of(
                new TransactionResponse(
                        LocalDate.of(2026, 10, 26),
                        "UBER TRIP",
                        new BigDecimal("-25.00"),
                        "EXPENSE",
                        TransactionCategory.TRANSPORTE,
                        CategorizationSource.KEYWORD_RULE,
                        false,
                        TransactionNature.UNKNOWN,
                        PaymentMethod.UNKNOWN
                ),
                new TransactionResponse(
                        LocalDate.of(2026, 10, 26),
                        "SUPERMERCADO",
                        new BigDecimal("-100.00"),
                        "EXPENSE",
                        TransactionCategory.SUPERMERCADO,
                        CategorizationSource.KEYWORD_RULE,
                        false,
                        TransactionNature.UNKNOWN,
                        PaymentMethod.UNKNOWN
                ),
                new TransactionResponse(
                        LocalDate.of(2026, 10, 26),
                        "PIX TRANSF",
                        new BigDecimal("-50.00"),
                        "EXPENSE",
                        TransactionCategory.OUTROS,
                        CategorizationSource.UNKNOWN,
                        true,
                        TransactionNature.TRANSFER,
                        PaymentMethod.PIX
                )
        );

        CategorySummaryResponse summary = new CategorySummaryResponse(
                new BigDecimal("175.00"),
                new BigDecimal("125.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                Map.of(
                        TransactionCategory.TRANSPORTE,
                        new BigDecimal("25.00"),
                        TransactionCategory.SUPERMERCADO,
                        new BigDecimal("100.00")
                )
        );

        when(transactionProcessingService.process(any()))
                .thenReturn(transactions);

        when(categorySummaryService.calculate(transactions))
                .thenReturn(summary);

        mockMvc.perform(
                        multipart("/api/statements/summary/categories")
                                .file(file)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOutflow")
                        .value(175.00))
                .andExpect(jsonPath("$.categorizedExpenses")
                        .value(125.00))
                .andExpect(jsonPath("$.uncategorizedOutflow")
                        .value(0))
                .andExpect(jsonPath("$.transferOutflow")
                        .value(50.00))
                .andExpect(jsonPath("$.expensesByCategory.TRANSPORTE")
                        .value(25.00))
                .andExpect(jsonPath("$.expensesByCategory.SUPERMERCADO")
                        .value(100.00));

        verify(transactionProcessingService).process(any());
        verify(categorySummaryService).calculate(transactions);
    }

    @Test
    void shouldAnalyzePdfWithOneProcessingPass() throws Exception {
        List<TransactionResponse> transactions = List.of(
                new TransactionResponse(LocalDate.of(2026, 10, 26),
                        "COMPRA EXEMPLO", new BigDecimal("-25.00"), "EXPENSE",
                        TransactionCategory.TRANSPORTE, CategorizationSource.KEYWORD_RULE,
                        false, TransactionNature.EXPENSE, PaymentMethod.DEBIT_CARD),
                new TransactionResponse(LocalDate.of(2026, 10, 26),
                        "CREDITO DE SALARIO", new BigDecimal("100.00"), "INCOME")
        );
        when(transactionProcessingService.processStatement(any()))
                .thenReturn(new ProcessedStatement(transactions, "Ana Exemplo"));
        when(financialSummaryService.calculate(transactions))
                .thenReturn(new FinancialSummaryService().calculate(transactions));
        when(categorySummaryService.calculate(transactions))
                .thenReturn(new CategorySummaryService().calculate(transactions));

        mockMvc.perform(multipart("/api/statements/analyze").file(createMockPdf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderName").value("Ana Exemplo"))
                .andExpect(jsonPath("$.summary.totalIncome").value(100))
                .andExpect(jsonPath("$.summary.totalExpenses").value(25))
                .andExpect(jsonPath("$.summary.netBalance").value(75))
                .andExpect(jsonPath("$.summary.transactionCount").value(2))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].date").value("2026-10-26"))
                .andExpect(jsonPath("$.transactions[0].amount").value(-25))
                .andExpect(jsonPath("$.transactions[0].category").value("TRANSPORTE"))
                .andExpect(jsonPath("$.transactions[0].source").value("KEYWORD_RULE"))
                .andExpect(jsonPath("$.transactions[0].requiresReview").value(false))
                .andExpect(jsonPath("$.transactions[0].nature").value("EXPENSE"))
                .andExpect(jsonPath("$.transactions[0].paymentMethod").value("DEBIT_CARD"))
                .andExpect(jsonPath("$.categorySummary.totalOutflow").value(25))
                .andExpect(jsonPath("$.categorySummary.categorizedExpenses").value(25))
                .andExpect(jsonPath("$.categorySummary.uncategorizedOutflow").value(0))
                .andExpect(jsonPath("$.categorySummary.transferOutflow").value(0))
                .andExpect(jsonPath("$.categorySummary.expensesByCategory.TRANSPORTE").value(25));

        verify(transactionProcessingService, times(1)).processStatement(any());
        verify(financialSummaryService).calculate(transactions);
        verify(categorySummaryService).calculate(transactions);
        verifyNoMoreInteractions(transactionProcessingService,
                financialSummaryService, categorySummaryService);
        verifyNoInteractions(pdfExtractionService);
    }

    @Test
    void shouldAnalyzeStatementWithoutTransactions() throws Exception {
        List<TransactionResponse> transactions = List.of();
        when(transactionProcessingService.processStatement(any()))
                .thenReturn(new ProcessedStatement(transactions, null));
        when(financialSummaryService.calculate(transactions))
                .thenReturn(new FinancialSummaryService().calculate(transactions));
        when(categorySummaryService.calculate(transactions))
                .thenReturn(new CategorySummaryService().calculate(transactions));

        mockMvc.perform(multipart("/api/statements/analyze").file(createMockPdf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderName").value(nullValue()))
                .andExpect(jsonPath("$.summary.transactionCount").value(0))
                .andExpect(jsonPath("$.summary.netBalance").value(0))
                .andExpect(jsonPath("$.transactions").isEmpty())
                .andExpect(jsonPath("$.categorySummary.expensesByCategory").isEmpty());
        verify(transactionProcessingService, times(1)).processStatement(any());
        verify(financialSummaryService).calculate(transactions);
        verify(categorySummaryService).calculate(transactions);
        verifyNoMoreInteractions(transactionProcessingService,
                financialSummaryService, categorySummaryService);
        verifyNoInteractions(pdfExtractionService);
    }

    @Test
    void shouldRejectInvalidPdfDuringAnalysis() throws Exception {
        when(transactionProcessingService.processStatement(any()))
                .thenThrow(new InvalidPdfException("O arquivo não é um PDF válido."));

        mockMvc.perform(multipart("/api/statements/analyze").file(createMockPdf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("O arquivo não é um PDF válido."));
        verify(transactionProcessingService, times(1)).processStatement(any());
        verifyNoMoreInteractions(transactionProcessingService);
        verifyNoInteractions(financialSummaryService, categorySummaryService, pdfExtractionService);
    }

    @Test
    void shouldRequireFileForAnalysis() throws Exception {
        mockMvc.perform(multipart("/api/statements/analyze"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(transactionProcessingService,
                financialSummaryService, categorySummaryService);
    }

    @Test
    void shouldExposeAllCategoriesWithoutChangingAnalysisContract() throws Exception {
        TransactionCategory[] categories = TransactionCategory.values();
        List<TransactionResponse> transactions = Arrays.stream(categories)
                .map(category -> new TransactionResponse(LocalDate.of(2026, 10, 26),
                        "EXEMPLO", new BigDecimal("-1.00"), "EXPENSE", category,
                        CategorizationSource.UNKNOWN, true, TransactionNature.UNKNOWN, PaymentMethod.PIX))
                .toList();
        when(transactionProcessingService.processStatement(any()))
                .thenReturn(new ProcessedStatement(transactions, null));
        when(financialSummaryService.calculate(transactions))
                .thenReturn(new FinancialSummaryService().calculate(transactions));
        when(categorySummaryService.calculate(transactions))
                .thenReturn(new CategorySummaryService().calculate(transactions));

        var response = mockMvc.perform(multipart("/api/statements/analyze").file(createMockPdf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.holderName").value(nullValue()))
                .andExpect(jsonPath("$.summary.length()").value(4))
                .andExpect(jsonPath("$.summary.transactionCount").value(categories.length))
                .andExpect(jsonPath("$.transactions.length()").value(categories.length))
                .andExpect(jsonPath("$.categorySummary.length()").value(5))
                .andExpect(jsonPath("$.categorySummary.totalOutflow").value(categories.length))
                .andExpect(jsonPath("$.categorySummary.uncategorizedOutflow").value(categories.length))
                .andExpect(jsonPath("$.categorySummary.categorizedExpenses").value(0))
                .andExpect(jsonPath("$.categorySummary.expensesByCategory").isEmpty());

        for (int index = 0; index < categories.length; index++) {
            String path = "$.transactions[" + index + "]";
            response.andExpect(jsonPath(path + ".length()").value(9))
                    .andExpect(jsonPath(path + ".category").value(categories[index].name()))
                    .andExpect(jsonPath(path + ".source").value("UNKNOWN"))
                    .andExpect(jsonPath(path + ".requiresReview").value(true))
                    .andExpect(jsonPath(path + ".nature").value("UNKNOWN"))
                    .andExpect(jsonPath(path + ".paymentMethod").value("PIX"));
        }
        verify(transactionProcessingService, times(1)).processStatement(any());
        verify(financialSummaryService).calculate(transactions);
        verify(categorySummaryService).calculate(transactions);
        verifyNoInteractions(pdfExtractionService);
    }

    @Test
    void shouldIncludeRecipientDetailsInAnalysisWithoutProcessingThePdfAgain() throws Exception {
        var transactions = List.of(
                new TransactionResponse(LocalDate.of(2026, 11, 26), "PIX ENVIADO - JOÃO EXEMPLO",
                        new BigDecimal("-10.10"), "EXPENSE", TransactionCategory.TRANSFERENCIAS_PESSOAIS,
                        CategorizationSource.KEYWORD_RULE, false, TransactionNature.TRANSFER, PaymentMethod.PIX),
                new TransactionResponse(LocalDate.of(2026, 11, 25), "PIX TRANSF joao exemplo",
                        new BigDecimal("-5.20"), "EXPENSE", TransactionCategory.TRANSFERENCIAS_PESSOAIS,
                        CategorizationSource.KEYWORD_RULE, false, TransactionNature.TRANSFER, PaymentMethod.PIX),
                new TransactionResponse(LocalDate.of(2026, 11, 25), "PIX QRS SUPERMERCADO",
                        new BigDecimal("-3.00"), "EXPENSE", TransactionCategory.SUPERMERCADO,
                        CategorizationSource.KEYWORD_RULE, false, TransactionNature.EXPENSE, PaymentMethod.PIX));
        when(transactionProcessingService.processStatement(any())).thenReturn(new ProcessedStatement(transactions, null));
        when(financialSummaryService.calculate(transactions)).thenReturn(new FinancialSummaryService().calculate(transactions));
        when(categorySummaryService.calculate(transactions)).thenReturn(new CategorySummaryService().calculate(transactions));

        mockMvc.perform(multipart("/api/statements/analyze").file(createMockPdf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(3))
                .andExpect(jsonPath("$.summary.totalExpenses").value(18.30))
                .andExpect(jsonPath("$.categorySummary.totalOutflow").value(18.30))
                .andExpect(jsonPath("$.recipientSummary.totalSent").value(15.30))
                .andExpect(jsonPath("$.recipientSummary.recipientCount").value(1))
                .andExpect(jsonPath("$.recipientSummary.transferCount").value(2))
                .andExpect(jsonPath("$.recipientSummary.recipients[0].name").value("JOÃO EXEMPLO"))
                .andExpect(jsonPath("$.recipientSummary.recipients[0].totalSent").value(15.30))
                .andExpect(jsonPath("$.recipientSummary.recipients[0].transferCount").value(2))
                .andExpect(jsonPath("$.recipientSummary.recipients[0].transactionIndexes[0]").value(0))
                .andExpect(jsonPath("$.recipientSummary.recipients[0].transactionIndexes[1]").value(1));
        verify(transactionProcessingService, times(1)).processStatement(any());
        verifyNoInteractions(pdfExtractionService);
    }

    private MockMultipartFile createMockPdf() {
        return new MockMultipartFile(
                "file",
                "extrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes()
        );
    }
}
