package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.dto.PixRecipientResponse;
import com.financepilot.backend.statement.dto.RecipientSummaryResponse;
import com.financepilot.backend.statement.dto.TransactionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecipientSummaryService {
    private final PixRecipientExtractionService extraction;

    public RecipientSummaryService(PixRecipientExtractionService extraction) {
        this.extraction = extraction;
    }

    public RecipientSummaryResponse calculate(List<TransactionResponse> transactions) {
        Map<String, Group> groups = new LinkedHashMap<>();
        for (int index = 0; index < transactions.size(); index++) {
            TransactionResponse transaction = transactions.get(index);
            var name = extraction.extract(transaction);
            if (name.isEmpty()) continue;
            Group group = groups.computeIfAbsent(extraction.normalizedName(name.get()), key -> new Group(name.get()));
            group.indexes.add(index);
            group.total = group.total.add(transaction.amount().abs());
        }
        List<PixRecipientResponse> recipients = groups.values().stream()
                .map(group -> new PixRecipientResponse(group.name, group.total, group.indexes.size(), group.indexes))
                .toList();
        BigDecimal total = recipients.stream().map(PixRecipientResponse::totalSent).reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = recipients.stream().mapToInt(PixRecipientResponse::transferCount).sum();
        return new RecipientSummaryResponse(total, recipients.size(), count, recipients);
    }

    private static final class Group {
        private final String name;
        private final List<Integer> indexes = new ArrayList<>();
        private BigDecimal total = BigDecimal.ZERO;

        private Group(String name) { this.name = name; }
    }
}
