
package com.financepilot.backend.statement.controller;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.PaymentMethod;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;

import com.financepilot.backend.statement.dto.CategorySummaryResponse;
import com.financepilot.backend.statement.dto.FinancialSummaryResponse;
import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;

import com.financepilot.backend.statement.service.CategorySummaryService;
import com.financepilot.backend.statement.service.FinancialSummaryService;
import com.financepilot.backend.statement.service.PdfExtractionService;
import com.financepilot.backend.statement.service.TransactionProcessingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementController.class)
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
                "PIX QRS SUPERMERCAD",
                new BigDecimal("-28.91"),
                "EXPENSE",
                TransactionCategory.ALIMENTACAO,
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
                        .value("PIX QRS SUPERMERCAD"))
                .andExpect(jsonPath("$[0].amount")
                        .value(-28.91))
                .andExpect(jsonPath("$[0].type")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$[0].category")
                        .value("ALIMENTACAO"))
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
                        TransactionCategory.ALIMENTACAO,
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
                        TransactionCategory.ALIMENTACAO,
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
                .andExpect(jsonPath("$.expensesByCategory.ALIMENTACAO")
                        .value(100.00));

        verify(transactionProcessingService).process(any());
        verify(categorySummaryService).calculate(transactions);
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
