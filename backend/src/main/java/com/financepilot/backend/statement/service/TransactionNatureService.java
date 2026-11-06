
package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.domain.TransactionCategory;
import com.financepilot.backend.statement.domain.TransactionNature;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class TransactionNatureService {

    public TransactionNature identify(String description) {
        return identify(description, TransactionCategory.OUTROS);
    }

    public TransactionNature identify(
            String description,
            TransactionCategory category
    ) {

        if (description == null || description.isBlank()) {
            return TransactionNature.UNKNOWN;
        }

        String normalized = normalize(description);

        // 1. Devoluções e estornos
        if (normalized.startsWith("DEV PIX")
                || normalized.startsWith("PIX DEVOLVIDO")
                || normalized.startsWith("ESTORNO")
                || normalized.startsWith("DEVOLUCAO")) {

            return TransactionNature.REFUND;
        }

        // 2. Recebimentos de salário
        if (normalized.startsWith("CREDITO DE SALARIO")
                || normalized.startsWith("PAGAMENTO DE SALARIO")) {

            return TransactionNature.INCOME;
        }

        // 3. Compras e pagamentos explícitos
        if (normalized.startsWith("PIX QRS")
                || normalized.startsWith("RSCSS")
                || normalized.startsWith("COMPRA")
                || normalized.startsWith("PAGAMENTO CARTAO")) {

            return TransactionNature.EXPENSE;
        }

        // 4. Transferências explicitamente identificadas
        // como movimentações entre contas próprias
        if (normalized.contains("ENTRE MINHAS CONTAS")
                || normalized.contains("MESMA TITULARIDADE")
                || normalized.contains("TRANSFERENCIA ENTRE CONTAS PROPRIAS")) {

            return TransactionNature.TRANSFER;
        }

        // 5. Pix enviado para estabelecimento reconhecido
        if ((normalized.startsWith("PIX ENVIADO")
                || normalized.startsWith("PIX TRANSF"))
                && category != null
                && category != TransactionCategory.OUTROS) {

            return TransactionNature.EXPENSE;
        }

        // 6. Demais operações permanecem desconhecidas
        return TransactionNature.UNKNOWN;
    }

    private String normalize(String description) {

        return Normalizer.normalize(
                        description, Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
    }
}
