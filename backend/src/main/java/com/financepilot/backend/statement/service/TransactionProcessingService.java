
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.PaymentMethod;
import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.CategorizationResult;
import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import com.financepilot.backend.statement.parser.BankStatementParser;
import com.financepilot.backend.statement.exception.InvalidStatementException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.DateTimeException;

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
                .orElseThrow(() -> new InvalidStatementException(
                        "Não reconhecemos um extrato bancário neste PDF. Envie um extrato Itaú ou Santander em um formato compatível."
                ));

        // 3. Extrai as transações
        List<TransactionResponse> transactions;
        try {
            transactions = parser.parse(text);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new InvalidStatementException(
                    "Não foi possível interpretar as movimentações deste PDF. Envie um extrato Itaú ou Santander em um formato compatível."
            );
        }

        // A bank marker alone is insufficient: require structured movements from its parser.
        if (transactions == null || transactions.isEmpty()
                || transactions.stream().anyMatch(transaction ->
                        transaction == null
                                || transaction.date() == null
                                || transaction.description() == null
                                || transaction.description().isBlank()
                                || transaction.amount() == null
                                || !("INCOME".equals(transaction.type())
                                    || "EXPENSE".equals(transaction.type())))
                || transactions.stream().noneMatch(transaction -> transaction.amount().signum() != 0)) {
            throw new InvalidStatementException(
                    "Não encontramos transações válidas neste PDF. Envie um extrato Itaú ou Santander com movimentações em um formato compatível."
            );
        }

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
