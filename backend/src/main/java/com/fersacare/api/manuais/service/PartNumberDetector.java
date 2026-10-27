package com.fersacare.api.manuais.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acha, num texto, os Part Numbers das peças cadastradas — é assim que um
 * manual é ligado sozinho à(s) peça(s) de que ele trata.
 *
 * Manual escreve o código de vários jeitos: "FRS-1001", "FRS 1001",
 * "frs1001", "FRS.1001". Por isso o PN é quebrado em blocos de letras e
 * números, e entre um bloco e outro vale qualquer separador (ou nenhum). A
 * fronteira nas pontas impede falso positivo: "FRS-10010" NÃO é "FRS-1001".
 */
@Component
public class PartNumberDetector {

    public record Peca(UUID id, String partNumber) {}

    private static final Pattern BLOCO = Pattern.compile("[A-Za-z]+|[0-9]+");

    /** pecaId → quantas vezes o Part Number aparece no texto (só as que aparecem). */
    public Map<UUID, Integer> detectar(String texto, List<Peca> pecas) {
        Map<UUID, Integer> achadas = new LinkedHashMap<>();
        if (texto == null || texto.isBlank()) return achadas;
        for (Peca peca : pecas) {
            Pattern padrao = padrao(peca.partNumber());
            if (padrao == null) continue;
            Matcher m = padrao.matcher(texto);
            int n = 0;
            while (m.find()) n++;
            if (n > 0) achadas.put(peca.id(), n);
        }
        return achadas;
    }

    static Pattern padrao(String partNumber) {
        if (partNumber == null) return null;
        List<String> blocos = new ArrayList<>();
        Matcher m = BLOCO.matcher(partNumber);
        while (m.find()) blocos.add(Pattern.quote(m.group()));
        // PN curto ("A1", "10") casaria com qualquer coisa no texto — não arrisca
        if (blocos.isEmpty() || partNumber.replaceAll("[^A-Za-z0-9]", "").length() < 4) return null;
        String meio = String.join("[\\s\\-_./]?", blocos);
        return Pattern.compile("(?<![A-Za-z0-9])" + meio + "(?![A-Za-z0-9])", Pattern.CASE_INSENSITIVE);
    }
}
