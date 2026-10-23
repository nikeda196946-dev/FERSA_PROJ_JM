package com.fersacare.api.manuais.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide o texto de cada página em pedaços de ~1200 caracteres, com 200
 * de sobreposição entre um pedaço e o próximo — se a resposta estiver bem
 * na fronteira entre dois chunks, a sobreposição garante que pelo menos um
 * deles tem a frase inteira.
 *
 * O corte procura, nessa ordem: fim de parágrafo, fim de frase, espaço —
 * para não partir palavra nem frase no meio quando dá para evitar.
 * Chunks nunca atravessam páginas (ver PdfExtractor: cada chunk cita uma página).
 */
@Component
public class Chunker {

    /** Trecho com menos que isso (ex: página só com "12" do rodapé) não vira chunk. */
    static final int TAMANHO_MINIMO = 30;

    public record Pedaco(int ordem, int pagina, String texto) {}

    private final int tamanho;
    private final int sobreposicao;

    public Chunker(
        @Value("${fersa.rag.chunk-tamanho}") int tamanho,
        @Value("${fersa.rag.chunk-sobreposicao}") int sobreposicao
    ) {
        if (sobreposicao >= tamanho / 2) {
            throw new IllegalArgumentException("chunk-sobreposicao precisa ser menor que metade de chunk-tamanho");
        }
        this.tamanho = tamanho;
        this.sobreposicao = sobreposicao;
    }

    public List<Pedaco> dividir(List<PdfExtractor.Pagina> paginas) {
        List<Pedaco> pedacos = new ArrayList<>();
        for (PdfExtractor.Pagina pagina : paginas) {
            for (String texto : dividirTexto(pagina.texto())) {
                pedacos.add(new Pedaco(pedacos.size(), pagina.numero(), texto));
            }
        }
        return pedacos;
    }

    List<String> dividirTexto(String texto) {
        List<String> resultado = new ArrayList<>();
        if (texto == null) return resultado;
        String t = texto.strip();
        int inicio = 0;
        while (inicio < t.length()) {
            int fim = Math.min(inicio + tamanho, t.length());
            if (fim < t.length()) {
                fim = melhorCorte(t, inicio, fim);
            }
            String pedaco = t.substring(inicio, fim).strip();
            if (pedaco.length() >= TAMANHO_MINIMO) {
                resultado.add(pedaco);
            }
            if (fim >= t.length()) break;

            // Próximo começa "sobreposicao" caracteres antes do fim deste,
            // ajustado para o começo de uma palavra.
            int proximo = fim - sobreposicao;
            while (proximo < fim && !Character.isWhitespace(t.charAt(proximo - 1))) {
                proximo++;
            }
            inicio = proximo > inicio ? proximo : fim;
        }
        return resultado;
    }

    /** Procura um bom ponto de corte na segunda metade da janela [inicio, fim). */
    private int melhorCorte(String t, int inicio, int fim) {
        int limite = inicio + tamanho / 2;
        int corte = t.lastIndexOf("\n\n", fim);
        if (corte >= limite) return corte;
        for (int i = fim - 1; i >= limite; i--) {
            char c = t.charAt(i);
            if ((c == '.' || c == '!' || c == '?' || c == ';' || c == ':') && Character.isWhitespace(t.charAt(i + 1))) {
                return i + 1;
            }
        }
        corte = t.lastIndexOf(' ', fim);
        return corte >= limite ? corte : fim;
    }
}
