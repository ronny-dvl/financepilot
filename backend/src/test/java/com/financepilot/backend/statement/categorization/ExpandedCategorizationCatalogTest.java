package com.financepilot.backend.statement.categorization;

import com.financepilot.backend.statement.domain.CategorizationSource;
import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.service.TransactionCategorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ExpandedCategorizationCatalogTest {
    private static final CompiledCatalog CATALOG = new CategorizationCatalogLoader(
            new ObjectMapper(), new DescriptionNormalizer()).load();
    private static final TransactionCategorizationService SERVICE =
            new TransactionCategorizationService(new ObjectMapper());

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("catalogAliases")
    void shouldApplyEveryCatalogAliasPolicyWithItsRequiredContext(
            String ruleId, String description, TransactionCategory expected) {
        var decision = SERVICE.categorizeWithDetails(description);
        assertEquals(expected, decision.result().category(), ruleId);
        boolean reviewOnly = expected == TransactionCategory.OUTROS;
        assertEquals(reviewOnly ? CategorizationSource.UNKNOWN : CategorizationSource.KEYWORD_RULE,
                decision.result().source());
        assertEquals(reviewOnly, decision.result().requiresReview());
        if (reviewOnly) assertNull(decision.ruleId());
        else assertNotNull(decision.ruleId());
    }

    // Structural self-consistency only; commercial correctness has independent semantic oracles.
    private static Stream<Arguments> catalogAliases() {
        return CATALOG.rules().stream().flatMap(rule -> rule.aliases().stream().map(alias -> {
            String description = alias.mode() == CategoryCatalog.MatchMode.PREFIX
                    ? alias.value() + "O" : alias.value();
            if (alias.mode() != CategoryCatalog.MatchMode.EXACT) {
                var conditions = new ArrayList<>(rule.include());
                if (!rule.includeAny().isEmpty()) conditions.addAll(rule.includeAny().getFirst());
                if (!rule.concludeAny().isEmpty()) conditions.addAll(rule.concludeAny().getFirst());
                String prefix = conditions.stream().filter(condition ->
                        condition.mode() == CategoryCatalog.MatchMode.STARTS_WITH)
                        .map(CompiledCatalog.NormalizedPattern::value).findFirst().orElse("COMPRA");
                description = prefix + " " + description;
                for (var condition : conditions) {
                    if (condition.mode() != CategoryCatalog.MatchMode.STARTS_WITH) description += " " + condition.value();
                }
            }
            return Arguments.of(rule.id(), description,
                    rule.conclusionRequired() && rule.concludeAny().isEmpty() ? TransactionCategory.OUTROS : rule.category());
        }));
    }

    @ParameterizedTest(name = "Sem contexto obrigatório: {0}")
    @MethodSource("guardedAliases")
    void shouldRecognizePersonalPayeesWithoutInferringAnAmbiguousUtilityCategory(String description) {
        var result = SERVICE.categorize(description);
        assertEquals(TransactionCategory.TRANSFERENCIAS_PESSOAIS, result.category());
        assertEquals(CategorizationSource.KEYWORD_RULE, result.source());
        assertFalse(result.requiresReview());
    }

    private static Stream<String> guardedAliases() {
        return CATALOG.rules().stream().filter(rule -> !rule.include().isEmpty() || !rule.includeAny().isEmpty())
                .flatMap(rule -> rule.aliases().stream().filter(alias -> rule.includeAny().stream().noneMatch(group ->
                        group.stream().allMatch(condition ->
                                (condition.mode() == CategoryCatalog.MatchMode.WORD
                                        || condition.mode() == CategoryCatalog.MatchMode.PHRASE)
                                        && alias.value().equals(condition.value())))))
                .map(alias -> "PIX ENVIADO CLIENTE TESTE " + alias.value());
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
            COMPRA McDonald's | ALIMENTACAO
            PIX QRS BURGERKING | ALIMENTACAO
            COMPRA iFood RESTAURANTE | ALIMENTACAO
            COMPRA I FOOD | ALIMENTACAO
            COMPRA UBER*EATS | ALIMENTACAO
            COMPRA UBEREATS | ALIMENTACAO
            COMPRA 99 FOOD | ALIMENTACAO
            COMPRA 99FOOD | ALIMENTACAO
            COMPRA KEETADELIVERY | ALIMENTACAO
            COMPRA ATACADÃO ALIMENTOS | SUPERMERCADO
            Pix enviado - Assaí Atacadista | SUPERMERCADO
            RSCSS CARREFOUR ALIMENTOS | SUPERMERCADO
            COMPRA PÃO DE AÇÚCAR | SUPERMERCADO
            COMPRA SUPERMERCADOS PAGUE MENOS | SUPERMERCADO
            COMPRA FARMACIAS PAGUE MENOS MEDICAMENTO | SAUDE
            COMPRA DROGARAIA MEDICAMENTO | SAUDE
            COMPRA FLEURY | SAUDE
            COMPRA PLANO DE SAUDE | SAUDE
            COMPRA UBER TRIP | TRANSPORTE
            COMPRA 99APP | TRANSPORTE
            COMPRA 99 APP | TRANSPORTE
            COMPRA CPTM | TRANSPORTE
            COMPRA POSTO SHELL GASOLINA | TRANSPORTE
            COMPRA ESTACIONAMENTO CENTRO | TRANSPORTE
            COMPRA ESTACIONAMENTOS CENTRO | TRANSPORTE
            COMPRA NETFLIX ASSINATURA | ASSINATURAS
            COMPRA APPLE.COM/BILL ASSINATURA | ASSINATURAS
            COMPRA AMAZON PRIME | ASSINATURAS
            COMPRA PRIMEVIDEO ASSINATURA | ASSINATURAS
            COMPRA AMAZON MUSIC ASSINATURA | ASSINATURAS
            COMPRA UBER ONE | ASSINATURAS
            ALUGUEL RESIDENCIAL | MORADIA
            BOLETO QUINTOANDAR | MORADIA
            COMPRA MERCADO LIVRE | COMPRAS
            COMPRA MERCADOLIVRE | COMPRAS
            MERCADO PAGO MERCADO LIVRE | COMPRAS
            COMPRA AMAZON MARKETPLACE | COMPRAS
            COMPRA AMZN MKTP | COMPRAS
            COMPRA AMAZON.COM.BR | COMPRAS
            COMPRA ALURA | EDUCACAO
            COMPRA UDEMY | EDUCACAO
            MENSALIDADE ESCOLAR | EDUCACAO
            COMPRA CINEMARK | LAZER
            COMPRA SYMPLA INGRESSO SHOW | LAZER
            COMPRA AMAZON WEB SERVICES | TECNOLOGIA
            COMPRA AMAZONWEBSERVICES | TECNOLOGIA
            COMPRA AWS | TECNOLOGIA
            COMPRA KABUM | TECNOLOGIA
            COMPRA APPLE STORE IPHONE | TECNOLOGIA
            CONTA DE LUZ | CONTAS_ESSENCIAIS
            BOLETO CEMIG | CONTAS_ESSENCIAIS
            BOLETO SABESP | CONTAS_ESSENCIAIS
            BOLETO EDP ENERGIA | CONTAS_ESSENCIAIS
            BOLETO LIGHT ENERGIA | CONTAS_ESSENCIAIS
            BOLETO VIVO FIBRA | INTERNET_TELEFONIA
            BOLETO TELEFONICA BRASIL | INTERNET_TELEFONIA
            BOLETO CLARO TELEFONIA | INTERNET_TELEFONIA
            RECARGA CELULAR | INTERNET_TELEFONIA
            COMPRA STEAM | GAMES
            COMPRA XBOX GAME PASS | GAMES
            COMPRA NINTENDO ESHOP | GAMES
            COMPRA GOOGLE PLAY GAMES | GAMES
            COMPRA LOJAS RENNER | VESTUARIO
            COMPRA RENNER | VESTUARIO
            COMPRA HERING | VESTUARIO
            COMPRA NIKE | VESTUARIO
            COMPRA SMARTFIT | ESPORTE_BEM_ESTAR
            COMPRA DECATHLON | ESPORTE_BEM_ESTAR
            MENSALIDADE ACADEMIA | ESPORTE_BEM_ESTAR
            COMPRA O BOTICÁRIO | BELEZA_CUIDADOS_PESSOAIS
            COMPRA SEPHORA | BELEZA_CUIDADOS_PESSOAIS
            COMPRA BARBEARIA | BELEZA_CUIDADOS_PESSOAIS
            COMPRA PETLOVE | PETS
            PLANO DE SAUDE PET | PETS
            CONSULTA VETERINARIA | PETS
            COMPRA AIRBNB HOSPEDAGEM | VIAGENS
            COMPRA LATAM | VIAGENS
            COMPRA AZUL LINHAS AEREAS | VIAGENS
            APLIC CDB | INVESTIMENTOS
            RESG RF | INVESTIMENTOS
            COMPRA TESOURO SELIC | INVESTIMENTOS
            TAR PAC SERV | TARIFAS_BANCARIAS
            TAR TED | TARIFAS_BANCARIAS
            ANUID CARTAO | TARIFAS_BANCARIAS
            PGTO IPVA | IMPOSTOS_TRIBUTOS
            DAS MEI | IMPOSTOS_TRIBUTOS
            GPS INSS | IMPOSTOS_TRIBUTOS
            IOF | IMPOSTOS_TRIBUTOS
            IPTU | IMPOSTOS_TRIBUTOS
            DARF | IMPOSTOS_TRIBUTOS
            PIX TRANSFERENCIA ENTRE MINHAS CONTAS | TRANSFERENCIAS
            TRANSFERENCIA MESMA TITULARIDADE | TRANSFERENCIAS
            PIX ENVIADO MARIA RENNER | TRANSFERENCIAS_PESSOAIS
            PIX ENVIADO CARLOS FLEURY | TRANSFERENCIAS_PESSOAIS
            PIX ENVIADO ANA GRANADO | TRANSFERENCIAS_PESSOAIS
            PIX ENVIADO JOAO PERINI | TRANSFERENCIAS_PESSOAIS
            PIX ENVIADO JOAO MADERO | TRANSFERENCIAS_PESSOAIS
            PIX ENVIADO ANA CLARO | TRANSFERENCIAS_PESSOAIS
            PIX ENVIADO MARIA SILVA | TRANSFERENCIAS_PESSOAIS
            """)
    void shouldClassifySyntheticBankDescriptions(String description, TransactionCategory expected) {
        var result = SERVICE.categorize(description);
        assertEquals(expected, result.category());
        assertEquals(CategorizationSource.KEYWORD_RULE, result.source());
        assertFalse(result.requiresReview());
    }

    @ParameterizedTest(name = "Revisão: {0}")
    @ValueSource(strings = {
            "MERCADO PAGO", "MERCADOPAGO", "PAGSEGURO", "PAGBANK", "STRIPE", "PAYPAL",
            "CIELO", "GETNET", "STONE", "REDE", "PICPAY", "HOTMART", "RAPPI",
            "MERCADO PAGO MARIA SILVA", "PAYPAL JOAO SILVA", "PAGSEGURO CLIENTE TESTE",
            "TED JOAO SILVA",
            "NUBANK", "ITAU", "SANTANDER", "BRADESCO", "BANCO DO BRASIL", "INTER",
            "XP", "RICO", "CLEAR", "INSS", "SALARIO INSS", "APLICACAO", "RESGATE",
            "AMAZON", "AMZN", "APPLE", "GOOGLE", "MICROSOFT", "99", "EATS",
            "UBERABA TURISMO", "UBERLANDIA SHOPPING", "XUBER", "UBERXAVIER",
            "SUBWAYSON", "PANVELINA", "KABUMBA", "NETFLIXANA", "STEAMER",
            "COMPRA ESTACIONARIO", "COMPRA VIVO", "COMPRA LIGHT", "COMPRA EDP",
            "COMPRA EXTRA", "DAS", "TARIFA", "IOF JOAO", "PIX ENVIADO IPTU JOAO",
            "99 APP FOOD", "IFOOD BENEFICIOS", "UBER CASH"
    })
    void shouldRequireReviewWithoutSufficientEvidence(String description) {
        var result = SERVICE.categorize(description);
        assertEquals(TransactionCategory.OUTROS, result.category());
        assertEquals(CategorizationSource.UNKNOWN, result.source());
        assertTrue(result.requiresReview());
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPRA ASUS PETZ", "COMPRA PETZ ASUS",
            "COMPRA AMAZON MARKETPLACE AWS", "COMPRA AMZN MKTP AMAZON WEB SERVICES"})
    void shouldRequireReviewForUnresolvedOrExplicitlyExcludedConflicts(String description) {
        var decision = SERVICE.categorizeWithDetails(description);
        assertEquals(TransactionCategory.OUTROS, decision.result().category());
        assertEquals(CategorizationSource.UNKNOWN, decision.result().source());
        assertTrue(decision.result().requiresReview());
        assertNull(decision.ruleId());
    }

    @Test
    void shouldExposeInternalConflictRuleIdentifiersWithoutPersonalData() {
        var decision = SERVICE.categorizeWithDetails("COMPRA ASUS PETZ");
        assertEquals(Set.of("tecnologia.asus", "pets.petz"), Set.copyOf(decision.conflictingRuleIds()));
    }

    @Test
    void shouldLoadNonemptyUniqueNormalizedAliasesAndRules() {
        Set<String> ids = new HashSet<>();
        Set<String> aliases = new HashSet<>();
        for (var rule : CATALOG.rules()) {
            assertTrue(ids.add(rule.id()), rule.id());
            for (var alias : rule.aliases()) {
                assertTrue(aliases.add(alias.value()), rule.id() + ": " + alias.value());
                assertFalse(alias.value().isBlank());
            }
        }
        assertFalse(ids.isEmpty());
        assertFalse(aliases.isEmpty());
    }

    @Test
    void shouldCoverCommercialCategoriesAndReservePersonalNamesForTheServiceFallback() {
        assertEquals(EnumSet.complementOf(EnumSet.of(TransactionCategory.OUTROS, TransactionCategory.TRANSFERENCIAS_PESSOAIS)),
                CATALOG.rules().stream().map(CompiledCatalog.CompiledRule::category).collect(Collectors.toSet()));
    }

    @Test
    void shouldUseFiniteParkingWordsInsteadOfOpenPrefix() {
        var prefixes = CATALOG.rules().stream().filter(rule -> rule.aliases().stream()
                .anyMatch(alias -> alias.mode() == CategoryCatalog.MatchMode.PREFIX)).toList();
        assertTrue(prefixes.isEmpty());
        var parking = CATALOG.rules().stream().filter(rule -> rule.id().equals("transporte.estacionament"))
                .findFirst().orElseThrow();
        assertEquals(Set.of("ESTACIONAMENTO", "ESTACIONAMENTOS"), parking.aliases().stream()
                .map(CompiledCatalog.NormalizedPattern::value).collect(Collectors.toSet()));
        assertTrue(parking.aliases().stream().allMatch(alias -> alias.mode() == CategoryCatalog.MatchMode.WORD));
    }
}
