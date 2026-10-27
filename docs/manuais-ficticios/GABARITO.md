# Gabarito — 10 perguntas de teste da IA

Perguntas sobre os manuais fictícios de `Manuais/`. A coluna **Peça** indica
o Part Number escolhido no chat. Onde aparece "—", a pergunta é feita sem
escolher peça.

| # | Pergunta | Peça | Resposta esperada | Onde está |
|---|---|---|---|---|
| 1 | Qual o torque de aperto da porca? | FRS-1001 | 450 Nm girando a roda; depois solta e reaperta com 150 Nm | FRS-1001, p. 2 |
| 2 | Qual o torque de aperto da porca? | FRS-1003 | Porca castelo com 280 Nm girando o pinhão | FRS-1003, p. 2 |
| 3 | Quanta graxa vai no rolamento de suspensão FRS-1004 e de quanto em quanto tempo? | — | 12 g por rolamento, a cada 30.000 km | FRS-1004, p. 1 |
| 4 | O cubo da roda está esquentando depois que troquei o rolamento, o que pode ser? | — | Excesso de graxa ou pré-carga incorreta no aperto da porca do cubo | FRS-1001, p. 4 / RT-2026-014, p. 2 |
| 5 | Qual óleo devo usar no rolamento do câmbio? | FRS-1002 | Óleo de transmissão SAE 75W-90 (trabalha imerso, não recebe graxa) | FRS-1002, p. 1 e 3 |
| 6 | Qual a força máxima para prensar o rolamento do eixo comando? | FRS-1005 | 15 kN, apoiando só o anel interno | FRS-1005, p. 1–2 |
| 7 | Posso reaproveitar o anel de retenção antigo na troca do rolamento de suspensão? | FRS-1004 | Não. Sempre instalar um anel de retenção novo | FRS-1004, p. 2 |
| 8 | No relatório de falhas de 2026, quantas ocorrências foram do FRS-1001 e qual foi a causa mais comum? | — | 23 ocorrências; causa mais comum: excesso de graxa no cubo (11) | RT-2026-014, p. 1 |
| 9 | Por quanto tempo posso deixar um rolamento guardado no estoque? | — | No máximo 3 anos a partir da data de fabricação | Guia Geral, p. 1 |
| 10 | Qual o preço do rolamento FRS-1001? | — | "Não encontrei informação suficiente nos materiais disponíveis…" | nenhum manual: a IA não pode inventar |

## O que cada pergunta mostra na demo

1. **1 e 2: a mesma pergunta para peças diferentes dá respostas diferentes.**
   Com a peça escolhida, a busca vai primeiro nos manuais ligados a ela, então
   o torque de uma peça não se mistura com o de outra.
2. **3: Part Number escrito na pergunta.** A busca acha o manual certo mesmo
   sem escolher a peça.
3. **4: busca por significado.** A pergunta diz "esquentando" e o texto diz
   "aquecimento". A resposta junta o manual e o relatório.
4. **5 e 8: dado de tabela.** O 75W-90 está na tabela da página 1 do FRS-1002.
   As 23 ocorrências estão na tabela do relatório.
5. **6 e 7: valor técnico e regra de segurança**, cada um de uma peça
   específica.
6. **8: relatório, não manual.** Mostra que a base aceita qualquer documento
   técnico.
7. **9: documento geral, sem peça.** O guia não está ligado a nenhum Part
   Number e mesmo assim é encontrado.
8. **10: pergunta fora dos manuais.** A IA devolve a mensagem fixa de "não
   encontrei" e não inventa um preço.

## Outros valores para perguntas extras na demo

| Peça | Valores que estão nos PDFs |
|---|---|
| FRS-1001 | graxa de lítio EP2, 18 g, a cada 40.000 km; folga axial 0,02–0,05 mm |
| FRS-1002 | inspeção a cada 120.000 km; folga 0,03 mm; parafusos da tampa 45 Nm |
| FRS-1003 | óleo 85W-140 a cada 80.000 km; folga coroa-pinhão 0,15–0,25 mm; torque de giro 1,5–2,5 Nm |
| FRS-1004 | parafuso 210 Nm; troca obrigatória com folga radial acima de 0,30 mm |
| FRS-1005 | parafusos do mancal 25 Nm; folga 0,10–0,20 mm |
| Relatório | 37 ocorrências no total (23 FRS-1001 e 14 FRS-1004) |

Resultado de uma rodada real (Gemini, backend local): **10/10, tempo médio
de 4,5 s**. A tabela completa está em `exemplo-resultado-testes-ia.md`.
