package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionNatureFinancialRegressionTest {
    private final TransactionNatureService service = new TransactionNatureService();

    @ParameterizedTest(name = "{0}, {2} -> {3}")
    @CsvSource(delimiter = '|', textBlock = """
            TAR TED | TARIFAS_BANCARIAS | -5 | EXPENSE
            BOLETO SABESP | CONTAS_ESSENCIAIS | -100 | EXPENSE
            PGTO IPVA | IMPOSTOS_TRIBUTOS | -200 | EXPENSE
            MENSALIDADE ESCOLAR | EDUCACAO | -300 | EXPENSE
            COMPRA UBER | TRANSPORTE | -30 | EXPENSE
            RSCSS UBER | TRANSPORTE | -30 | EXPENSE
            PIX QRS UBER | TRANSPORTE | -30 | EXPENSE
            PIX ENVIADO UBER | TRANSPORTE | -30 | EXPENSE
            PIX QRS UBER MESMA TITULARIDADE | TRANSPORTE | -50 | TRANSFER
            COMPRA UBER ENTRE MINHAS CONTAS | TRANSPORTE | -50 | TRANSFER
            PIX QRS UBER MESMA TITULARIDADE | TRANSPORTE | 50 | TRANSFER
            TRANSFERENCIA ENTRE CONTAS PROPRIAS | TRANSFERENCIAS | -50 | TRANSFER
            PIX DEVOLVIDO UBER | TRANSPORTE | -30 | REFUND
            PIX DEVOLVIDO UBER | TRANSPORTE | 30 | REFUND
            ESTORNO COMPRA UBER | TRANSPORTE | 30 | REFUND
            DEV PIX UBER | TRANSPORTE | -30 | REFUND
            COMPRA UBER | TRANSPORTE | 30 | UNKNOWN
            PIX ENVIADO UBER | TRANSPORTE | 30 | UNKNOWN
            PIX RECEBIDO UBER | TRANSPORTE | -30 | UNKNOWN
            TAR TED | TARIFAS_BANCARIAS | 5 | UNKNOWN
            PGTO IPVA | IMPOSTOS_TRIBUTOS | 200 | UNKNOWN
            CREDITO DE SALARIO EMPRESA | OUTROS | 2000 | INCOME
            CREDITO DE SALARIO EMPRESA | OUTROS | -2000 | UNKNOWN
            APLIC CDB | INVESTIMENTOS | -1000 | UNKNOWN
            COMPRA DE ACOES | INVESTIMENTOS | -1000 | UNKNOWN
            COMPRA DE ACOES AWS | TECNOLOGIA | -1000 | UNKNOWN
            RESGATE CDB | INVESTIMENTOS | 1000 | UNKNOWN
            PIX ENVIADO EXEMPLO | TRANSFERENCIAS | -50 | UNKNOWN
            UBER | TRANSPORTE | -30 | UNKNOWN
            NETFLIX | ASSINATURAS | -30 | UNKNOWN
            PIX ENVIADO JOAO SILVA | OUTROS | -40 | UNKNOWN
            PIX TRANSF JOAO SILVA | OUTROS | -40 | UNKNOWN
            PIX RECEBIDO JOAO SILVA | OUTROS | 40 | UNKNOWN
            BOLETO CLIENTE TESTE | OUTROS | -100 | UNKNOWN
            BOLETO SABESP AGENDADO | CONTAS_ESSENCIAIS | -100 | UNKNOWN
            BOLETO SABESP NAO PAGO | CONTAS_ESSENCIAIS | -100 | UNKNOWN
            MENSALIDADE ESCOLAR A PAGAR | EDUCACAO | -300 | UNKNOWN
            COMPRA CANCELADA UBER | TRANSPORTE | -30 | UNKNOWN
            COMPRAZ UBER | TRANSPORTE | -30 | UNKNOWN
            COMPRA UBER | TRANSPORTE | 0 | UNKNOWN
            """)
    void shouldUseSignedAmountAndExplicitOperationEvidence(String description,
            TransactionCategory category, BigDecimal amount, TransactionNature expected) {
        assertEquals(expected, service.identify(description, category, amount));
    }

    @Test
    void shouldKeepMissingEvidenceUnknown() {
        assertEquals(TransactionNature.UNKNOWN,
                service.identify("COMPRA UBER", TransactionCategory.TRANSPORTE, null));
        assertEquals(TransactionNature.UNKNOWN,
                service.identify(null, TransactionCategory.TRANSPORTE, new BigDecimal("-30")));
        assertEquals(TransactionNature.UNKNOWN,
                service.identify(" ", TransactionCategory.TRANSPORTE, new BigDecimal("-30")));
        assertEquals(TransactionNature.UNKNOWN,
                service.identify("BOLETO SABESP", null, new BigDecimal("-100")));
    }
}
