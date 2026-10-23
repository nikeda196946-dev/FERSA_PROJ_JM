package com.fersacare.api.manuais.service;

import com.fersacare.api.common.RequisicaoInvalidaException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfExtractorTest {

    private final PdfExtractor extractor = new PdfExtractor();

    @Test
    void extraiTextoPorPagina() throws IOException {
        byte[] pdf = gerarPdf(new String[][] {
            {"Torque da porca do cubo: 450 Nm."},
            {},                                         // página só com imagem / em branco
            {"Graxa NLGI 2, intervalo de 100 mil km."},
        });

        PdfExtractor.Resultado r = extractor.extrair(pdf);

        assertThat(r.totalPaginas()).isEqualTo(3);
        assertThat(r.paginas()).extracting(PdfExtractor.Pagina::numero).containsExactly(1, 2, 3);
        assertThat(r.paginas().get(0).texto()).isEqualTo("Torque da porca do cubo: 450 Nm.");
        assertThat(r.paginas().get(1).texto()).isEmpty();
        assertThat(r.paginas().get(2).texto()).contains("NLGI 2");
    }

    @Test
    void juntaLinhasDaMesmaFraseEPalavraHifenizada() {
        String bruto = "O rola-\nmento deve ser montado\ncom prensa.\n\nNunca use martelo.  \n";
        assertThat(PdfExtractor.limpar(bruto))
            .isEqualTo("O rolamento deve ser montado com prensa.\n\nNunca use martelo.");
    }

    @Test
    void reconheceCabecalhoDePdf() throws IOException {
        assertThat(PdfExtractor.pareceSerPdf(gerarPdf(new String[][] {{"x"}}))).isTrue();
        assertThat(PdfExtractor.pareceSerPdf("não é pdf".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(PdfExtractor.pareceSerPdf(null)).isFalse();
    }

    @Test
    void pdfCorrompidoViraErroDeRequisicao() {
        byte[] lixo = "%PDF-1.7 isto não é um pdf de verdade".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> extractor.extrair(lixo)).isInstanceOf(RequisicaoInvalidaException.class);
    }

    private static byte[] gerarPdf(String[][] paginas) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String[] linhas : paginas) {
                PDPage pagina = new PDPage();
                doc.addPage(pagina);
                try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                    float y = 700;
                    for (String linha : linhas) {
                        cs.beginText();
                        cs.setFont(fonte, 12);
                        cs.newLineAtOffset(50, y);
                        cs.showText(linha);
                        cs.endText();
                        y -= 16;
                    }
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
