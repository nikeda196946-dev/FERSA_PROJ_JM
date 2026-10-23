package com.fersacare.api.manuais.service;

import com.fersacare.api.common.RequisicaoInvalidaException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extrai o texto do PDF página por página (PDFBox). Página por página, e
 * não o documento inteiro de uma vez, para cada chunk saber de qual
 * página veio — é isso que o chat mostra como fonte ("Manual X, p. 12").
 *
 * PDF escaneado (só imagem) sai com texto vazio: não fazemos OCR, e a
 * ingestão marca o manual como erro com uma mensagem explicando isso.
 */
@Component
public class PdfExtractor {

    public record Pagina(int numero, String texto) {}

    public record Resultado(int totalPaginas, List<Pagina> paginas) {}

    public static boolean pareceSerPdf(byte[] conteudo) {
        return conteudo != null && conteudo.length > 4
            && conteudo[0] == '%' && conteudo[1] == 'P' && conteudo[2] == 'D' && conteudo[3] == 'F';
    }

    public Resultado extrair(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            int total = documento.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // ajuda em manuais com colunas/tabelas

            List<Pagina> paginas = new ArrayList<>(total);
            for (int p = 1; p <= total; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                paginas.add(new Pagina(p, limpar(stripper.getText(documento))));
            }
            return new Resultado(total, paginas);
        } catch (InvalidPasswordException e) {
            throw new RequisicaoInvalidaException("O PDF está protegido por senha.");
        } catch (IOException e) {
            throw new RequisicaoInvalidaException("Não foi possível ler o PDF: " + e.getMessage());
        }
    }

    /**
     * Junta palavras quebradas com hífen no fim da linha ("rola-\nmento"),
     * transforma quebras de linha simples em espaço (o PDF quebra linha
     * onde a página acaba, não onde a frase acaba) e mantém linhas em
     * branco como separador de parágrafo — que o Chunker usa como ponto
     * preferido de corte.
     */
    static String limpar(String texto) {
        if (texto == null) return "";
        String t = texto
            .replace("\r\n", "\n").replace('\r', '\n')
            .replace('\u00A0', ' ')
            .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")
            .replaceAll("[ \t]+", " ")
            .replaceAll(" *\n *", "\n")
            .replaceAll("(\\p{L})-\n(\\p{Ll})", "$1$2")
            .replaceAll("\n{2,}", "\u0000")   // marca parágrafo
            .replace('\n', ' ')
            .replace("\u0000", "\n\n");
        return t.strip();
    }
}
