package com.financepilot.backend.statement.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Conservative header-only extraction; never searches movement descriptions for names. */
final class HolderNameSupport {
    private static final Pattern MOVEMENTS = Pattern.compile(
            "(?iu)^(?:DETALHE DOS MOVIMENTOS|LAN[ÇC]AMENTOS|MOVIMENTA[ÇC][ÕO]ES|"
                    + "SALDO ANTERIOR|DATA\\h+(?:DESCRI[ÇC][ÃA]O|LAN[ÇC]AMENTO)|\\d{2}/\\d{2}/\\d{4}\\h+).*"
    );
    private static final Pattern NAME = Pattern.compile(
            "^[\\p{L}\\p{M}]+(?:['’\\-][\\p{L}\\p{M}]+)*"
                    + "(?: [\\p{L}\\p{M}]+(?:['’\\-][\\p{L}\\p{M}]+)*){1,9}$"
    );
    private static final Pattern NON_NAME = Pattern.compile(
            "(?iu)\\b(?:BANCO|ITA[ÚU]|UNIBANCO|SANTANDER|EXTRATO|CONTA|AG[ÊE]NCIA|"
                    + "SALDO|TITULAR|CLIENTE|NOME|CPF|CNPJ|RUA|AVENIDA|ENDERE[ÇC]O|"
                    + "PER[ÍI]ODO|LAN[ÇC]AMENTOS|MOVIMENTA[ÇC][ÕO]ES|LTDA|EIRELI)\\b"
    );

    private HolderNameSupport() { }

    static List<String> headerLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        int pageBreak = text.indexOf('\f');
        String firstPage = pageBreak >= 0 ? text.substring(0, pageBreak) : text;
        List<String> result = new ArrayList<>();
        for (String raw : firstPage.split("\\R")) {
            String line = raw.replace('\u00a0', ' ').strip();
            if (MOVEMENTS.matcher(line).matches() || result.size() >= 80) break;
            if (!line.isBlank()) result.add(line);
        }
        return result;
    }

    static Optional<String> uniqueName(List<String> candidates) {
        var names = new LinkedHashMap<String, String>();
        for (String candidate : candidates) {
            String name = candidate.replace('\u00a0', ' ')
                    .replaceAll("(?iu)\\h+(?:CPF|CNPJ|AG[ÊE]NCIA|CONTA|PER[ÍI]ODO)\\h*:?.*$", "")
                    .replaceAll("\\s+", " ").strip();
            if (name.length() <= 120 && NAME.matcher(name).matches()
                    && !NON_NAME.matcher(name).find()) {
                names.putIfAbsent(name.toUpperCase(Locale.ROOT), name);
            }
        }
        return names.size() == 1 ? Optional.of(names.values().iterator().next()) : Optional.empty();
    }
}
