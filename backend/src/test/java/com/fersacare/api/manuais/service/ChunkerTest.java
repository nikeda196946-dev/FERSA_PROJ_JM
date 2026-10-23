package com.fersacare.api.manuais.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkerTest {

    private final Chunker chunker = new Chunker(200, 40);

    @Test
    void textoCurtoViraUmChunkSo() {
        assertThat(chunker.dividirTexto("Aplique 450 Nm na porca do cubo e recue um quarto de volta."))
            .containsExactly("Aplique 450 Nm na porca do cubo e recue um quarto de volta.");
    }

    @Test
    void ignoraTrechosMinusculosComoNumeroDePagina() {
        assertThat(chunker.dividirTexto("12")).isEmpty();
        assertThat(chunker.dividirTexto("   ")).isEmpty();
        assertThat(chunker.dividirTexto(null)).isEmpty();
    }

    @Test
    void textoLongoRespeitaTamanhoMaximoESobrepoe() {
        String frase = "O rolamento deve ser montado com prensa, nunca com martelo. ";
        String texto = frase.repeat(20).strip(); // ~1200 caracteres

        List<String> pedacos = chunker.dividirTexto(texto);

        assertThat(pedacos).hasSizeGreaterThan(5);
        assertThat(pedacos).allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(200));
        // corta em fim de frase quando dá
        assertThat(pedacos.get(0)).endsWith(".");
        // sobreposição: o começo do 2º chunk aparece no fim do 1º
        String inicioDoSegundo = pedacos.get(1).substring(0, 20);
        assertThat(pedacos.get(0)).contains(inicioDoSegundo);
    }

    @Test
    void naoPartePalavraNoMeio() {
        String texto = "palavra ".repeat(100).strip();
        for (String p : chunker.dividirTexto(texto)) {
            assertThat(p).matches("palavra( palavra)*");
        }
    }

    @Test
    void textoSemEspacoNenhumAindaTermina() {
        List<String> pedacos = chunker.dividirTexto("x".repeat(1000));
        assertThat(String.join("", pedacos).length()).isGreaterThanOrEqualTo(1000);
        assertThat(pedacos).allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(200));
    }

    @Test
    void numeraEmOrdemEGuardaAPagina() {
        List<Chunker.Pedaco> pedacos = chunker.dividir(List.of(
            new PdfExtractor.Pagina(1, "Capítulo 1 — montagem do rolamento de roda dianteira."),
            new PdfExtractor.Pagina(2, "7"),
            new PdfExtractor.Pagina(3, "Capítulo 2 — lubrificação com graxa NLGI 2 a cada 100 mil km.")
        ));

        assertThat(pedacos).extracting(Chunker.Pedaco::ordem).containsExactly(0, 1);
        assertThat(pedacos).extracting(Chunker.Pedaco::pagina).containsExactly(1, 3);
    }

    @Test
    void recusaSobreposicaoGrandeDemais() {
        assertThatThrownBy(() -> new Chunker(100, 60)).isInstanceOf(IllegalArgumentException.class);
    }
}
