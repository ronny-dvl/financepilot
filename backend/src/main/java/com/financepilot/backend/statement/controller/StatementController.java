
package com.financepilot.backend.statement.controller;

import com.financepilot.backend.statement.dto.FinancialSummaryResponse;
import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import com.financepilot.backend.statement.service.FinancialSummaryService;
import com.financepilot.backend.statement.service.PdfExtractionService;
import com.financepilot.backend.statement.service.TransactionProcessingService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final PdfExtractionService pdfExtractionService;
    private final TransactionProcessingService transactionProcessingService;
    private final FinancialSummaryService financialSummaryService;

    public StatementController(
            PdfExtractionService pdfExtractionService,
            TransactionProcessingService transactionProcessingService,
            FinancialSummaryService financialSummaryService
    ) {
        this.pdfExtractionService = pdfExtractionService;
        this.transactionProcessingService = transactionProcessingService;
        this.financialSummaryService = financialSummaryService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StatementTextResponse> upload(
            @RequestParam("file") MultipartFile file
    ) {
        StatementTextResponse response =
                pdfExtractionService.extract(file);

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/transactions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam("file") MultipartFile file
    ) {
        List<TransactionResponse> transactions =
                transactionProcessingService.process(file);

        return ResponseEntity.ok(transactions);
    }

    @PostMapping(
            value = "/summary",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FinancialSummaryResponse> getSummary(
            @RequestParam("file") MultipartFile file
    ) {
        List<TransactionResponse> transactions =
                transactionProcessingService.process(file);

        FinancialSummaryResponse summary =
                financialSummaryService.calculate(transactions);

        return ResponseEntity.ok(summary);
    }
}
