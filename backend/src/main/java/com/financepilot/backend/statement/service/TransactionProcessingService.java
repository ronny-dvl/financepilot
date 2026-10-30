
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.CategorizationResult;
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
    private final TransactionCategorizationService categorizationService;
    private final TransactionNatureService natureService;

    public TransactionProcessingService(
            PdfExtractionService pdfExtractionService,
            ItauStatementParser itauStatementParser,
            TransactionCategorizationService categorizationService,
            TransactionNatureService natureService
    ) {
        this.pdfExtractionService = pdfExtractionService;
        this.itauStatementParser = itauStatementParser;
        this.categorizationService = categorizationService;
        this.natureService = natureService;
    }

    public List<TransactionResponse> process(MultipartFile file) {

        StatementTextResponse response =
                pdfExtractionService.extract(file);

        List<TransactionResponse> transactions =
                itauStatementParser.parse(response.text());

        return transactions.stream()
                .map(transaction -> {

                    CategorizationResult category =
                            categorizationService.categorize(
                                    transaction.description()
                            );

                    TransactionNature nature =
                            natureService.identify(
                                    transaction.description()
                            );

                    return new TransactionResponse(
                            transaction.date(),
                            transaction.description(),
                            transaction.amount(),
                            transaction.type(),
                            category.category(),
                            category.source(),
                            category.requiresReview(),
                            nature
                    );
                })
                .toList();
    }
}
