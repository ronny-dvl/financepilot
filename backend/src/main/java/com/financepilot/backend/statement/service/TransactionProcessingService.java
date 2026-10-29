
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import com.financepilot.backend.statement.parser.ItauStatementParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TransactionProcessingService {

    private final PdfExtractionService pdfExtractionService;
    private final ItauStatementParser itauStatementParser;

    public TransactionProcessingService(
            PdfExtractionService pdfExtractionService,
            ItauStatementParser itauStatementParser
    ) {
        this.pdfExtractionService = pdfExtractionService;
        this.itauStatementParser = itauStatementParser;
    }

    public List<TransactionResponse> process(MultipartFile file) {

        StatementTextResponse response =
                pdfExtractionService.extract(file);

        String text = response.text();

        return itauStatementParser.parse(text);
    }
}
