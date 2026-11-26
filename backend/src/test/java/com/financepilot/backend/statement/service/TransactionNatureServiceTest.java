
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionNatureServiceTest {

    private final TransactionNatureService service =
            new TransactionNatureService();

    // ===== DEVOLUÇÕES =====

    @Test
    void shouldIdentifyPixRefund() {
        assertEquals(
                TransactionNature.REFUND,
                service.identify("DEV PIX UBER DO BRA")
        );
    }

    @Test
    void shouldIdentifySantanderRefund() {
        assertEquals(
                TransactionNature.REFUND,
                service.identify("Pix devolvido - Uber")
        );
    }

    // ===== TRANSFERÊNCIAS AMBÍGUAS =====

    @Test
    void shouldKeepGenericPixTransferUnknown() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify("PIX TRANSF KETHELY")
        );
    }

    @Test
    void shouldKeepSentPixToPersonUnknown() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify("Pix enviado - João")
        );
    }

    @Test
    void shouldKeepReceivedPixUnknown() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify("Pix recebido - Maria")
        );
    }

    // ===== TRANSFERÊNCIAS INTERNAS =====

    @Test
    void shouldIdentifyOwnAccountTransfer() {
        assertEquals(
                TransactionNature.TRANSFER,
                service.identify("Transferencia entre minhas contas")
        );
    }

    @Test
    void shouldIdentifySameOwnershipTransfer() {
        assertEquals(
                TransactionNature.TRANSFER,
                service.identify("Transferência mesma titularidade")
        );
    }

    // ===== RECEITAS =====

    @Test
    void shouldIdentifySalary() {
        assertEquals(
                TransactionNature.INCOME,
                service.identify("Credito de salario - Empresa")
        );
    }

    @Test
    void shouldIdentifySalaryWithAccent() {
        assertEquals(
                TransactionNature.INCOME,
                service.identify("Crédito de salário - Empresa")
        );
    }

    // ===== DESPESAS =====

    @Test
    void shouldIdentifyPixPurchase() {
        assertEquals(
                TransactionNature.EXPENSE,
                service.identify("PIX QRS SUPERMERCADO")
        );
    }

    @Test
    void shouldIdentifyCardPurchase() {
        assertEquals(
                TransactionNature.EXPENSE,
                service.identify("RSCSS SUPERMERCADO")
        );
    }

    @Test
    void shouldIdentifyPurchase() {
        assertEquals(
                TransactionNature.EXPENSE,
                service.identify("COMPRA MERCADO")
        );
    }

    @Test
    void shouldIdentifySantanderSupermarketPix() {
        assertEquals(
                TransactionNature.EXPENSE,
                service.identify(
                        "Pix enviado - Supermercado Nova Eta",
                        TransactionCategory.SUPERMERCADO
                )
        );
    }

    @Test
    void shouldIdentifySantanderUberPix() {
        assertEquals(
                TransactionNature.EXPENSE,
                service.identify(
                        "Pix enviado - Uber Do Brasil Tecnol",
                        TransactionCategory.TRANSPORTE
                )
        );
    }

    @Test
    void shouldNotAssumeUncategorizedPixIsExpense() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify(
                        "Pix enviado - João",
                        TransactionCategory.OUTROS
                )
        );
    }

    @Test
    void shouldNotTreatReceivedPixAsExpense() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify(
                        "Pix recebido - Maria",
                        TransactionCategory.ALIMENTACAO
                )
        );
    }

    @Test
    void shouldNotInferConsumptionForInvestmentsOrTransfers() {
        for (TransactionCategory category : new TransactionCategory[]{
                TransactionCategory.INVESTIMENTOS, TransactionCategory.TRANSFERENCIAS}) {
            for (String description : new String[]{"PIX ENVIADO EXEMPLO", "PIX TRANSF EXEMPLO",
                    "PIX QRS EXEMPLO", "RSCSS EXEMPLO", "COMPRA EXEMPLO", "PAGAMENTO CARTAO EXEMPLO"}) {
                assertEquals(TransactionNature.UNKNOWN, service.identify(description, category));
            }
        }
    }

    @Test
    void shouldPreserveExplicitNaturesForNonConsumptionCategories() {
        for (TransactionCategory category : new TransactionCategory[]{
                TransactionCategory.INVESTIMENTOS, TransactionCategory.TRANSFERENCIAS}) {
            assertEquals(TransactionNature.TRANSFER,
                    service.identify("PIX ENVIADO ENTRE MINHAS CONTAS", category));
            assertEquals(TransactionNature.REFUND,
                    service.identify("ESTORNO EXEMPLO", category));
            assertEquals(TransactionNature.INCOME,
                    service.identify("CREDITO DE SALARIO EXEMPLO", category));
        }
    }

    // ===== CASOS DESCONHECIDOS =====

    @Test
    void shouldIdentifyUnknownTransaction() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify("OPERACAO NAO IDENTIFICADA")
        );
    }

    @Test
    void shouldHandleNullDescription() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify(null)
        );
    }

    @Test
    void shouldHandleBlankDescription() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify("   ")
        );
    }
}
