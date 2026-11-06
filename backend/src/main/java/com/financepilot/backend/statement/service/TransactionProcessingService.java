
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.PaymentMethod;
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
    private final PaymentMethodService paymentMethodService;

    public TransactionProcessingService(
            PdfExtractionService pdfExtractionService,
            List<BankStatementParser> parsers,
            TransactionCategorizationService categorizationService,
            TransactionNatureService natureService,
            PaymentMethodService paymentMethodService
    ) {
        this.pdfExtractionService = pdfExtractionService;
        this.parsers = parsers;
        this.categorizationService = categorizationService;
        this.natureService = natureService;
        this.paymentMethodService = paymentMethodService;
    }

    public List<TransactionResponse> process(MultipartFile file) {

        // 1. Extrai o texto do PDF
        StatementTextResponse response =
                pdfExtractionService.extract(file);

        String text = response.text();

        // 2. Identifica automaticamente o banco
        BankStatementParser parser = parsers.stream()
                .filter(candidate -> candidate.supports(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Banco não reconhecido ou extrato não suportado."
                ));

        // 3. Extrai as transações
        List<TransactionResponse> transactions =
                parser.parse(text);

        // 4. Enriquece as transações
        return transactions.stream()
                .map(transaction -> {

                    String description = transaction.description();

                    // Identifica a categoria
                    CategorizationResult category =
                            categorizationService.categorize(description);

                    // Identifica a natureza financeira
                    TransactionNature nature =
                            natureService.identify(
                                    description,
                                    category.category()
                            );

                    // Identifica o meio de pagamento
                    PaymentMethod paymentMethod =
                            paymentMethodService.identify(description);

                    // Monta a resposta completa
                    return new TransactionResponse(
                            transaction.date(),
                            description,
                            transaction.amount(),
                            transaction.type(),
                            category.category(),
                            category.source(),
                            category.requiresReview(),
                            nature,
                            paymentMethod
                    );
                })
                .toList();
    }
}
