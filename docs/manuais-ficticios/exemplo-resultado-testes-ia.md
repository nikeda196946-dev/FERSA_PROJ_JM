# Resultado dos testes da IA — 27/10/2026, 13:49:09

**Acertos (conferência automática): 10/10** · tempo médio de resposta: 4.5 s · API: http://localhost:8080

A conferência automática verifica se os termos esperados aparecem na resposta e se a fonte certa foi citada. Revisem cada linha e anotem a nota final na coluna Revisão.

| # | Pergunta | Peça | Esperado | Resposta da IA | Fontes citadas | Auto | Tempo | Revisão |
|---|---|---|---|---|---|---|---|---|
| 1 | Qual o torque de aperto da porca? | FRS-1001 | 450 Nm girando a roda; solta e reaperta com 150 Nm. | Aperte a porca do cubo com 450 Nm girando a roda, solte e reaperte com 150 Nm. | FRS-1001 - Manual Técnico - Rolamento de Roda Dianteira p.2 | ✅ | 1.4 s |  |
| 2 | Qual o torque de aperto da porca? | FRS-1003 | Porca castelo com 280 Nm girando o pinhão. | Aperte a porca castelo até 280 Nm girando o pinhão. | FRS-1003 - Manual Técnico - Rolamento de Diferencial p.2 | ✅ | 17.8 s |  |
| 3 | Quanta graxa vai no rolamento de suspensão FRS-1004 e de quanto em quanto tempo? | — | 12 g por rolamento, a cada 30.000 km. | Quantidade de graxa: 12 g por rolamento. Intervalo de lubrificação: a cada 30.000 km. | FRS-1004 - Manual Técnico - Rolamento de Suspensão p.1 | ✅ | 1.0 s |  |
| 4 | O cubo da roda está esquentando depois que troquei o rolamento, o que pode ser? | — | Excesso de graxa ou pré-carga incorreta no aperto da porca do cubo. | O aquecimento do cubo após a troca pode ser causado por excesso de graxa ou pré-carga incorreta na porca do cubo. | RT-2026-014 - Relatório de Falhas em Rolamentos de Cubo e Suspensão p.2; FRS-1001 - Manual Técnico - Rolamento de Roda Dianteira p.4 | ✅ | 1.1 s |  |
| 5 | Qual óleo devo usar no rolamento do câmbio? | FRS-1002 | Óleo de transmissão SAE 75W-90 (o rolamento trabalha imerso; não recebe graxa). | Use o óleo de transmissão SAE 75W-90. | FRS-1002 - Manual Técnico - Rolamento de Câmbio p.3; FRS-1002 - Manual Técnico - Rolamento de Câmbio p.1 | ✅ | 9.1 s |  |
| 6 | Qual a força máxima para prensar o rolamento do eixo comando? | FRS-1005 | 15 kN, apoiando somente o anel interno. | A força máxima para prensar o rolamento FRS-1005 é de 15 kN. | FRS-1005 - Manual Técnico - Rolamento de Eixo Comando p.2; FRS-1005 - Manual Técnico - Rolamento de Eixo Comando p.1 | ✅ | 1.7 s |  |
| 7 | Posso reaproveitar o anel de retenção antigo na troca do rolamento de suspensão? | FRS-1004 | Não. Sempre instalar anel de retenção novo. | Não. O anel antigo nunca deve ser reaproveitado. Instale sempre um anel de retenção novo. | FRS-1004 - Manual Técnico - Rolamento de Suspensão p.2; RT-2026-014 - Relatório de Falhas em Rolamentos de Cubo e Suspensão p.2 | ✅ | 1.0 s |  |
| 8 | No relatório de falhas de 2026, quantas ocorrências foram do FRS-1001 e qual foi a causa mais comum? | — | 23 ocorrências; causa mais comum: excesso de graxa no cubo (11). | Foram 23 ocorrências com o rolamento FRS-1001. A causa principal foi excesso de graxa no cubo. | RT-2026-014 - Relatório de Falhas em Rolamentos de Cubo e Suspensão p.1 | ✅ | 8.5 s |  |
| 9 | Por quanto tempo posso deixar um rolamento guardado no estoque? | — | No máximo 3 anos a partir da data de fabricação. | Prazo máximo de armazenagem: 3 anos a partir da data de fabricação. Guarde na embalagem original, deitados, em local seco (umidade abaixo de 60% e temperatura entre 10 °C e 30 °C). | Guia Geral de Lubrificação e Armazenagem de Rolamentos p.1 | ✅ | 2.5 s |  |
| 10 | Qual o preço do rolamento FRS-1001? | — | Não encontrei informação suficiente nos materiais disponíveis. Consulte um técnico ou abra um chamado de suporte. | Não encontrei informação suficiente nos materiais disponíveis. Consulte um técnico ou abra um chamado de suporte. | — | ✅ | 1.0 s |  |

## O que cada pergunta testa

1. Mesma pergunta da nº 2, outra peça: a IA não pode misturar os valores.
2. Busca restrita aos manuais da peça escolhida.
3. Part Number citado na pergunta, sem escolher a peça.
4. Busca por significado: 'esquentando' x 'aquecimento' no texto.
5. Informação de tabela (página 1).
6. Valor técnico específico de uma peça.
7. Pergunta de sim/não com regra de segurança.
8. Pergunta sobre relatório (não manual) e dado de tabela.
9. Documento geral, sem Part Number.
10. Informação que não está em nenhum manual: a IA não pode inventar.
