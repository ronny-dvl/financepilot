
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.CategorizationResult;
import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import com.financepilot.backend.statement.parser.BankStatementParser;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TransactionProcessingService {

    private final PdfExtractionService pdfExtractionService;
    private final List<BankStatementParser> parsers;
    private final TransactionCategorizationService categorizationService;
    private final TransactionNatureService natureService;

    public TransactionProcessingService(
            PdfExtractionService pdfExtractionService,
            List<BankStatementParser> parsers,
            TransactionCategorizationService categorizationService,
            TransactionNatureService natureService
    ) {
        this.pdfExtractionService = pdfExtractionService;
        this.parsers = parsers;
        this.categorizationService = categorizationService;
        this.natureService = natureService;
    }

    public List<TransactionResponse> process(MultipartFile file) {

        // 1. Extrai o texto do PDF
        StatementTextResponse response =
                pdfExtractionService.extract(file);

        String text = response.text();

        // 2. Identifica qual parser reconhece o extrato
        BankStatementParser parser = parsers.stream()
                .filter(candidate -> candidate.supports(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Banco não reconhecido ou extrato não suportado."
                ));

        System.out.println(
                "Parser selecionado: " + parser.getClass().getSimpleName()
        );

        // 3. Extrai as transações com o parser correto
        List<TransactionResponse> transactions =
                parser.parse(text);

        // 4. Aplica categorização e natureza financeira
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
