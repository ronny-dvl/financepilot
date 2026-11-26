package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.service.TransactionCategorizationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit product-policy expectations, independent of the catalog contents. */
class SemanticCurationRegressionTest {
    private static final TransactionCategorizationService SERVICE = new TransactionCategorizationService(new ObjectMapper());

    @ParameterizedTest
    @ValueSource(strings = {
            "PIX ENVIADO CARLOS FERRETTO", "PIX ENVIADO JOAO FUJIOKA", "PIX ENVIADO CARLOS RENNER",
            "PIX RECEBIDO JOAO CALVIN KLEIN", "PIX ENVIADO MARIA LINDT", "PIX ENVIADO JOAO MARRIOTT",
            "PIX ENVIADO CARLOS PARDINI", "PIX ENVIADO CARLOS HERMES PARDINI",
            "PIX ENVIADO JOAO SALOMAO ZOPPI", "PIX ENVIADO COMPRA FERRETTO", "PIX RECEBIDO RSCSS JOHN JOHN",
            "PIX ENVIADO CARLOS RENNER CPF 12345678900", "PIX ENVIADO RENNER CPF 12345678900",
            "MERCADO PAGO", "PAGSEGURO", "STRIPE", "PAYPAL", "COMPRA MERCADO PAGO", "COMPRA PAYPAL",
            "COMPRA HORTIFRUTI RESTAURANTE", "COMPRA RESTAURANTE FARMACIA MEDICAMENTO",
            "UBERABA", "UBERLANDIA", "MOVIMENTO XYZ SEM IDENTIFICACAO", "VIVO", "LIGHT", "EDP"
    })
    void shouldReviewPersonalPayeesUnknownDescriptionsAndRealAmbiguities(String description) {
        var result = SERVICE.categorize(description);
        assertEquals(TransactionCategory.OUTROS, result.category());
        assertEquals(CategorizationSource.UNKNOWN, result.source());
        assertTrue(result.requiresReview());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            UBER | TRANSPORTE
            UBER EATS | ALIMENTACAO
            IFOOD | ALIMENTACAO
            PIX MADERO | ALIMENTACAO
            CARREFOUR | SUPERMERCADO
            SAMSUNG | TECNOLOGIA
            APPLE.COM/BILL | ASSINATURAS
            NETFLIX | ASSINATURAS
            RENNER | VESTUARIO
            FERRETTO | EDUCACAO
            PETZ | PETS
            SABESP | CONTAS_ESSENCIAIS
            MERCADO LIVRE | COMPRAS
            FUJIOKA | TECNOLOGIA
            CALVIN KLEIN | VESTUARIO
            JOHN JOHN | VESTUARIO
            PIX ENVIADO JOHN JOHN | VESTUARIO
            PIX ENVIADO FERRETTO | EDUCACAO
            PIX ENVIADO RENNER 123456 | VESTUARIO
            PIX ENVIADO RENNER PORTO ALEGRE | VESTUARIO
            PIX ENVIADO PETZ SAO PAULO | PETS
            PIX ENVIADO MADERO 26 11 2026 | ALIMENTACAO
            PIX ENVIADO MERCADO PAGO MADERO | ALIMENTACAO
            PIX ENVIADO CARLOS FERRETTO CURSO | EDUCACAO
            PIX ENVIADO JOAO RENNER CNPJ 12345678000100 | VESTUARIO
            RSCSS JOHN JOHN | VESTUARIO
            PAGAMENTO FERRETTO CURSOS ON LINE LTDA | EDUCACAO
            COMPRA FUJIOKA NOTEBOOK | TECNOLOGIA
            COMPRA CALVIN KLEIN PERFUME | VESTUARIO
            PIX QRS RENNER | VESTUARIO
            COMPRA O BOTICARIO | BELEZA_CUIDADOS_PESSOAIS
            COMPRA APPLE.COM/BILL APP AVULSO | ASSINATURAS
            TWITCH | ASSINATURAS
            COMPRA TWITCH BITS | ASSINATURAS
            SYMPLA | LAZER
            COMPRA SYMPLA CURSO TECNICO | LAZER
            COMPRA AMAZON MARKETPLACE | COMPRAS
            COMPRA MUNDO VERDE | ESPORTE_BEM_ESTAR
            COMPRA FAST SHOP | TECNOLOGIA
            ATACADAO | SUPERMERCADO
            COMPRA SAMSUNG GELADEIRA | TECNOLOGIA
            APPLE STORE | TECNOLOGIA
            POSTO SHELL | TRANSPORTE
            COMPRA POSTO SHELL GASOLINA CONVENIENCIA | TRANSPORTE
            FARMACIA | SAUDE
            COMPRA FARMACIA PERFUME | SAUDE
            DROGASIL | SAUDE
            PET SHOP | PETS
            HOSPITAL ALBERT EINSTEIN | SAUDE
            UBER TRIP | TRANSPORTE
            99 FOOD | ALIMENTACAO
            99FOOD | ALIMENTACAO
            99 APP | TRANSPORTE
            99APP | TRANSPORTE
            AMAZON WEB SERVICES | TECNOLOGIA
            AWS | TECNOLOGIA
            AMZN MKTP | COMPRAS
            COMPRA CASA RIACHUELO | COMPRAS
            NETFLIX GAMES | GAMES
            COPEL TELECOM | INTERNET_TELEFONIA
            PLANO DE SAUDE PET | PETS
            COMPRA UBER EATS | ALIMENTACAO
            COMPRA UBER ONE | ASSINATURAS
            COMPRA UBER TRIP | TRANSPORTE
            COMPRA DUOLINGO PLUS | ASSINATURAS
            COMPRA SUPER DUOLINGO | ASSINATURAS
            COMPRA NETFLIX GAMES | GAMES
            COMPRA COPEL TELECOM | INTERNET_TELEFONIA
            TAR TED | TARIFAS_BANCARIAS
            PGTO IPVA | IMPOSTOS_TRIBUTOS
            """)
    void shouldUsePredominantActivityWithoutRequiringProductOrPurchaseMarkers(
            String description, TransactionCategory expected) {
        var result = SERVICE.categorize(description);
        assertEquals(expected, result.category());
        assertEquals(CategorizationSource.KEYWORD_RULE, result.source());
        assertFalse(result.requiresReview());
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPRA ASUS DROGASIL", "COMPRA DROGASIL ASUS", "COMPRA AMAZONMARKETPLACE AWS",
            "COMPRA AWS AMAZONMARKETPLACE", "COMPRA AMAZON MARKETPLACE AMAZONWEBSERVICES",
            "COMPRA 99 FOOD NIKE", "COMPRA 99FOOD NIKE", "COMPRA UBER EATS ASUS"})
    void shouldKeepGenuinelyCompetingMerchantEvidenceForReview(String description) {
        var decision = SERVICE.categorizeWithDetails(description);
        assertTrue(decision.result().requiresReview());
        assertEquals(TransactionCategory.OUTROS, decision.result().category());
        assertTrue(decision.conflictingRuleIds().size() >= 2);
    }
}
