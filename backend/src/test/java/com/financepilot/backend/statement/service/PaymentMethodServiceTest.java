
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentMethodServiceTest {

    private final PaymentMethodService service =
            new PaymentMethodService();

    @Test
    void shouldIdentifySentPix() {
        assertEquals(
                PaymentMethod.PIX,
                service.identify("Pix enviado - Mercado")
        );
    }

    @Test
    void shouldIdentifyReceivedPix() {
        assertEquals(
                PaymentMethod.PIX,
                service.identify("Pix recebido - Maria")
        );
    }

    @Test
    void shouldIdentifyPixRefund() {
        assertEquals(
                PaymentMethod.PIX,
                service.identify("DEV PIX UBER")
        );
    }

    @Test
    void shouldIdentifyCreditCard() {
        assertEquals(
                PaymentMethod.CREDIT_CARD,
                service.identify("Compra cartao de credito")
        );
    }

    @Test
    void shouldIdentifyDebitCard() {
        assertEquals(
                PaymentMethod.DEBIT_CARD,
                service.identify("Compra cartao de debito")
        );
    }

    @Test
    void shouldIdentifyBoleto() {
        assertEquals(
                PaymentMethod.BOLETO,
                service.identify("Pagamento boleto")
        );
    }

    @Test
    void shouldIdentifyUnknown() {
        assertEquals(
                PaymentMethod.UNKNOWN,
                service.identify("OPERACAO NAO IDENTIFICADA")
        );
    }

    @Test
    void shouldHandleNull() {
        assertEquals(
                PaymentMethod.UNKNOWN,
                service.identify(null)
        );
    }
}
