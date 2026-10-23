package com.fersacare.api.rag.service;

import com.fersacare.api.ia.GeminiClient;
import com.fersacare.api.manuais.repository.ManualChunkRepository;
import com.fersacare.api.manuais.repository.ManualChunkRepository.TrechoEncontrado;
import com.fersacare.api.rag.dto.ChatResponse;
import com.fersacare.api.rag.dto.FonteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * O RAG em si:
 *   1. embedding da pergunta (Gemini, taskType RETRIEVAL_QUERY)
 *   2. top-K trechos mais parecidos no pgvector (só manuais ativos e prontos)
 *   3. prompt = regras + trechos numerados [1]..[K] + pergunta → Gemini Flash
 *   4. resposta + as fontes que ela citou
 *
 * Se a busca não acha nada, nem chamamos o modelo de texto: devolvemos
 * direto a frase de "não encontrei". Se acha trechos mas eles não
 * respondem a pergunta, o prompt manda o modelo usar exatamente a mesma
 * frase — é por ela que sabemos preencher encontrou = false.
 */
@Service
public class ChatService {

    public static final String NAO_ENCONTREI = "Não encontrei essa informação nos manuais.";

    static final String INSTRUCAO_SISTEMA = """
        Você é o assistente técnico do FERSA Care. Responde dúvidas de mecânicos sobre rolamentos \
        usando SOMENTE os trechos de manuais fornecidos na mensagem.

        Regras:
        1. Use apenas as informações dos trechos. Não use conhecimento próprio e nunca invente \
        valores (torque, folga, medidas, intervalos, quantidades de graxa).
        2. Se os trechos não trazem a resposta, responda exatamente: "%s" — e nada mais.
        3. Cite a origem de cada informação com o número do trecho entre colchetes, ex.: [1] ou [2][3].
        4. Responda em português do Brasil, de forma direta e prática, em passos numerados quando \
        for um procedimento.
        """.formatted(NAO_ENCONTREI);

    private static final Pattern CITACAO = Pattern.compile("\\[(\\d+)]");
    private static final int TAMANHO_TRECHO_FONTE = 300;

    private final GeminiClient gemini;
    private final ManualChunkRepository chunkRepository;
    private final int topK;
    private final double scoreMinimo;

    public ChatService(
        GeminiClient gemini,
        ManualChunkRepository chunkRepository,
        @Value("${fersa.rag.top-k}") int topK,
        @Value("${fersa.rag.score-minimo}") double scoreMinimo
    ) {
        this.gemini = gemini;
        this.chunkRepository = chunkRepository;
        this.topK = topK;
        this.scoreMinimo = scoreMinimo;
    }

    public ChatResponse perguntar(String pergunta) {
        String p = pergunta.strip();
        float[] vetor = gemini.embedPergunta(p);
        List<TrechoEncontrado> trechos = chunkRepository.buscarSimilares(vetor, topK, scoreMinimo);
        if (trechos.isEmpty()) {
            return new ChatResponse(NAO_ENCONTREI, false, List.of(), gemini.modeloChat());
        }

        String resposta = gemini.gerar(INSTRUCAO_SISTEMA, montarMensagem(p, trechos));
        if (resposta.isBlank() || resposta.replace("\"", "").strip().startsWith(NAO_ENCONTREI)) {
            return new ChatResponse(NAO_ENCONTREI, false, List.of(), gemini.modeloChat());
        }
        return new ChatResponse(resposta, true, fontesCitadas(resposta, trechos), gemini.modeloChat());
    }

    static String montarMensagem(String pergunta, List<TrechoEncontrado> trechos) {
        StringBuilder sb = new StringBuilder("Trechos dos manuais:\n\n");
        for (int i = 0; i < trechos.size(); i++) {
            TrechoEncontrado t = trechos.get(i);
            sb.append('[').append(i + 1).append("] Manual: ").append(t.tituloManual());
            if (t.pagina() != null) sb.append(" — página ").append(t.pagina());
            sb.append('\n').append(t.conteudo()).append("\n\n");
        }
        return sb.append("Pergunta: ").append(pergunta).toString();
    }

    /**
     * Fontes = os trechos que a resposta citou ([1], [3]...). Se o modelo
     * não citou nenhum (acontece), devolvemos todos os trechos usados no
     * prompt — melhor mostrar fonte demais do que nenhuma.
     */
    static List<FonteDTO> fontesCitadas(String resposta, List<TrechoEncontrado> trechos) {
        Set<Integer> citados = new TreeSet<>();
        Matcher m = CITACAO.matcher(resposta);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= trechos.size()) citados.add(n);
        }
        if (citados.isEmpty()) {
            for (int i = 1; i <= trechos.size(); i++) citados.add(i);
        }

        List<FonteDTO> fontes = new ArrayList<>(citados.size());
        for (int n : citados) {
            TrechoEncontrado t = trechos.get(n - 1);
            fontes.add(new FonteDTO(n, t.manualId(), t.tituloManual(), t.pagina(), resumir(t.conteudo()),
                Math.round(t.score() * 1000) / 1000.0));
        }
        return fontes;
    }

    private static String resumir(String texto) {
        return texto.length() <= TAMANHO_TRECHO_FONTE ? texto : texto.substring(0, TAMANHO_TRECHO_FONTE) + "…";
    }
}
