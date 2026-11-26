package com.financepilot.backend.statement.domain;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionCategoryTest {
    @Test
    void shouldExposeTwentyThreeIdentifiersPreservingExistingOrder() {
        assertEquals(List.of(
                "ALIMENTACAO", "TRANSPORTE", "SAUDE", "ASSINATURAS", "MORADIA",
                "EDUCACAO", "LAZER", "COMPRAS", "TRANSFERENCIAS", "OUTROS",
                "SUPERMERCADO", "CONTAS_ESSENCIAIS", "INTERNET_TELEFONIA", "GAMES",
                "TECNOLOGIA", "VESTUARIO", "ESPORTE_BEM_ESTAR", "BELEZA_CUIDADOS_PESSOAIS",
                "PETS", "VIAGENS", "INVESTIMENTOS", "TARIFAS_BANCARIAS", "IMPOSTOS_TRIBUTOS"
        ), Arrays.stream(TransactionCategory.values()).map(Enum::name).toList());
    }

    @Test
    void shouldSerializeAndReadCategoriesUsingStableIdentifiers() {
        ObjectMapper mapper = new ObjectMapper();
        for (TransactionCategory category : TransactionCategory.values()) {
            String json = "\"" + category.name() + "\"";
            assertEquals(json, mapper.writeValueAsString(category));
            assertEquals(category, mapper.readValue(json, TransactionCategory.class));
        }
    }
}
