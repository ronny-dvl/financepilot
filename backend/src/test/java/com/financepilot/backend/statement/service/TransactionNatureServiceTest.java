
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionNature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionNatureServiceTest {

    private final TransactionNatureService service =
            new TransactionNatureService();

    @Test
    void shouldIdentifyPixRefund() {
        assertEquals(
                TransactionNature.REFUND,
                service.identify("DEV PIX UBER DO BRA")
        );
    }

    @Test
    void shouldIdentifyTransfer() {
        assertEquals(
                TransactionNature.TRANSFER,
                service.identify("PIX TRANSF KETHELY")
        );
    }

    @Test
    void shouldIdentifyUnknownTransaction() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify("PIX QRS SUPERMERCADO")
        );
    }

    @Test
    void shouldHandleNullDescription() {
        assertEquals(
                TransactionNature.UNKNOWN,
                service.identify(null)
        );
    }
}
