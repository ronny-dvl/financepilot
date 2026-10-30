
package com.financepilot.backend.statement.controller;

import com.financepilot.backend.statement.dto.FinancialSummaryResponse;
import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
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

    @Test
    void shouldUploadPdfSuccessfully() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes()
        );

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

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes()
        );

        TransactionResponse transaction = new TransactionResponse(
                LocalDate.of(2026, 10, 26),
                "PIX QRS SUPERMERCAD",
                new BigDecimal("-28.91"),
                "EXPENSE"
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
                        .value("EXPENSE"));
    }

    @Test
    void shouldReturnFinancialSummarySuccessfully() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes()
        );

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
}
