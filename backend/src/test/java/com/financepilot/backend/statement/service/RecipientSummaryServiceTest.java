package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.PaymentMethod;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import com.financepilot.backend.statement.dto.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipientSummaryServiceTest {
    private final PixRecipientExtractionService extraction = new PixRecipientExtractionService();
    private final RecipientSummaryService service = new RecipientSummaryService(extraction);

    @Test
    void shouldGroupEquivalentNamesAcrossBankPrefixesWithExactDecimalTotalsAndOriginalIndexes() {
        var transactions = List.of(
                personal("PIX ENVIADO - JOÃO DA SILVA", "-10.10"),
                personal("Pix transf joao   da  silva", "-5.20"),
                personal("PIX QRS JOAO DA SILVA", "-4.70"),
                personal("PIX TRANSF VINICIU", "-1.10"),
                personal("PIX TRANSF VINICIUS", "-2.20"));
        var result = service.calculate(transactions);
        assertEquals(3, result.recipientCount());
        assertEquals(5, result.transferCount());
        assertEquals(0, new BigDecimal("23.30").compareTo(result.totalSent()));
        var first = result.recipients().getFirst();
        assertEquals("JOÃO DA SILVA", first.name());
        assertEquals(3, first.transferCount());
        assertEquals(List.of(0, 1, 2), first.transactionIndexes());
        assertEquals(0, new BigDecimal("20.00").compareTo(first.totalSent()));
        assertEquals(List.of("VINICIU", "VINICIUS"), result.recipients().subList(1, 3).stream()
                .map(recipient -> recipient.name()).toList());
        assertEquals(5, result.recipients().stream().flatMap(recipient -> recipient.transactionIndexes().stream()).distinct().count());
        assertThrows(UnsupportedOperationException.class, () -> first.transactionIndexes().add(99));
    }

    @Test
    void shouldExcludeIncomingCommercialRefundOwnAccountUnresolvedAndUnextractableMovements() {
        var result = service.calculate(List.of(
                personal("PIX ENVIADO - MARIA EXEMPLO", "10.00"),
                transaction("PIX ENVIADO - MERCADINHO", "-10.00", TransactionCategory.SUPERMERCADO, TransactionNature.EXPENSE, PaymentMethod.PIX, false),
                transaction("PIX DEVOLVIDO - MARIA EXEMPLO", "-10.00", TransactionCategory.TRANSFERENCIAS_PESSOAIS, TransactionNature.REFUND, PaymentMethod.PIX, false),
                transaction("PIX TRANSF ENTRE MINHAS CONTAS", "-10.00", TransactionCategory.TRANSFERENCIAS, TransactionNature.TRANSFER, PaymentMethod.PIX, false),
                personal("PIX ENVIADO - MARIA EXEMPLO MESMA TITULARIDADE", "-10.00"),
                personal("PIX ENVIADO - TRANSFERENCIA ENTRE CONTAS PROPRIAS", "-10.00"),
                transaction("PIX ENVIADO - MARIA EXEMPLO", "-10.00", TransactionCategory.TRANSFERENCIAS_PESSOAIS, TransactionNature.TRANSFER, PaymentMethod.PIX, true),
                transaction("PIX ENVIADO - MARIA EXEMPLO", "-10.00", TransactionCategory.TRANSFERENCIAS_PESSOAIS, TransactionNature.TRANSFER, PaymentMethod.UNKNOWN, false),
                personal("PIX RECEBIDO - MARIA EXEMPLO", "-10.00"),
                personal("PIX ENVIADO - ", "-10.00"),
                personal("PIX TRANSF 12345", "-10.00")));
        assertEquals(0, result.recipientCount());
        assertEquals(0, result.transferCount());
        assertEquals(0, result.totalSent().signum());
        assertTrue(result.recipients().isEmpty());
    }

    @Test
    void shouldPreserveTruncationRemoveTaggedCpfAndNeverMergeByNameSimilarity() {
        assertEquals("JOAO PEREIRA DOS SA", extraction.extract(
                personal("PIX ENVIADO - JOAO PEREIRA DOS SA", "-1.00")).orElseThrow());
        assertEquals("MARIA EXEMPLO", extraction.extract(
                personal("Pix enviado - MARIA EXEMPLO CPF ***.***.***-**", "-1.00")).orElseThrow());
        var result = service.calculate(List.of(
                personal("PIX QRS ANA-MARIA EXEMPLO", "-1.00"),
                personal("PIX ENVIADO - ANA MARIA EXEMPLO", "-1.00"),
                personal("PIX ENVIADO - JOAO PEREIRA DOS SA", "-1.00"),
                personal("PIX TRANSF JOAO PEREIRA DOS SANTOS", "-1.00")));
        assertEquals(4, result.recipientCount());
        assertEquals(4, result.transferCount());
        assertFalse(result.toString().contains("MARIA"));
        assertFalse(result.recipients().getFirst().toString().contains("MARIA"));
    }

    private TransactionResponse personal(String description, String amount) {
        return transaction(description, amount, TransactionCategory.TRANSFERENCIAS_PESSOAIS,
                TransactionNature.TRANSFER, PaymentMethod.PIX, false);
    }

    private TransactionResponse transaction(String description, String amount, TransactionCategory category,
            TransactionNature nature, PaymentMethod payment, boolean review) {
        var value = new BigDecimal(amount);
        return new TransactionResponse(LocalDate.of(2026, 11, 26), description, value,
                value.signum() < 0 ? "EXPENSE" : "INCOME", category, CategorizationSource.KEYWORD_RULE,
                review, nature, payment);
    }
}
