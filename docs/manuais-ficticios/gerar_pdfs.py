"""
Gera os manuais FICTÍCIOS do FERSA Care (uso acadêmico) em PDF com texto
selecionável, organizados em pastas por categoria — prontos para subir no
SharePoint de teste (ou pelo "Enviar PDF" do CMS).

Cada assunto fica numa página fixa (quebra de página explícita), para o
gabarito das perguntas de teste (gabarito.json) apontar a página certa.

Uso:  pip install reportlab   e depois   python gerar_pdfs.py
Saída: ./Manuais/<categoria>/<arquivo>.pdf
"""
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

AVISO = "DOCUMENTO FICTÍCIO — criado para o projeto acadêmico FERSA Care. Não use em manutenção real."

estilos = getSampleStyleSheet()
TITULO = ParagraphStyle("titulo", parent=estilos["Title"], fontSize=17, leading=21, spaceAfter=4)
SUB = ParagraphStyle("sub", parent=estilos["Normal"], fontSize=10, textColor=colors.HexColor("#5C6870"), spaceAfter=10)
H = ParagraphStyle("h", parent=estilos["Heading2"], fontSize=13, spaceBefore=6, spaceAfter=6, textColor=colors.HexColor("#0A2E52"))
P = ParagraphStyle("p", parent=estilos["Normal"], fontSize=10.5, leading=14.5, spaceAfter=6)
LI = ParagraphStyle("li", parent=P, leftIndent=12, bulletIndent=2, spaceAfter=3)


def rodape(doc_titulo):
    def desenhar(canvas, doc):
        canvas.saveState()
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(colors.HexColor("#5C6870"))
        canvas.drawString(20 * mm, 12 * mm, "FERSA Care — documento fictício (uso acadêmico)")
        canvas.drawRightString(190 * mm, 12 * mm, f"página {doc.page}")
        canvas.restoreState()
    return desenhar


def tabela(linhas):
    t = Table(linhas, hAlign="LEFT", colWidths=[70 * mm, 95 * mm])
    t.setStyle(TableStyle([
        ("FONT", (0, 0), (-1, -1), "Helvetica", 9.5),
        ("FONT", (0, 0), (-1, 0), "Helvetica-Bold", 9.5),
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EAF2F8")),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#B3BCC3")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return t


def montar(destino, titulo, subtitulo, paginas):
    """paginas: lista de páginas; cada página é uma lista de blocos (tipo, conteúdo)."""
    destino.parent.mkdir(parents=True, exist_ok=True)
    doc = SimpleDocTemplate(str(destino), pagesize=A4, title=titulo, author="FERSA Care (fictício)",
                            leftMargin=20 * mm, rightMargin=20 * mm, topMargin=18 * mm, bottomMargin=20 * mm)
    historia = [Paragraph(titulo, TITULO), Paragraph(subtitulo, SUB), Paragraph(f"<i>{AVISO}</i>", SUB)]
    for i, pagina in enumerate(paginas):
        if i:
            historia.append(PageBreak())
        for tipo, conteudo in pagina:
            if tipo == "h":
                historia.append(Paragraph(conteudo, H))
            elif tipo == "p":
                historia.append(Paragraph(conteudo, P))
            elif tipo == "lista":
                historia += [Paragraph(item, LI, bulletText="•") for item in conteudo]
            elif tipo == "num":
                historia += [Paragraph(item, LI, bulletText=f"{n}.") for n, item in enumerate(conteudo, 1)]
            elif tipo == "tabela":
                historia += [tabela(conteudo), Spacer(1, 6)]
    doc.build(historia, onFirstPage=rodape(titulo), onLaterPages=rodape(titulo))


# ---------------------------------------------------------------------------
# Conteúdo. Coerente com o catálogo fictício (database/seed.sql): mesmos
# veículos, lubrificantes e ferramentas de cada peça. Valores de torque e
# folga DIFERENTES por peça, para testar se a IA mistura peças.
# ---------------------------------------------------------------------------
DOCUMENTOS = [
    {
        "pasta": "Rolamento de Cubo",
        "arquivo": "FRS-1001 - Manual Técnico - Rolamento de Roda Dianteira.pdf",
        "titulo": "Manual Técnico FRS-1001 — Rolamento de Roda Dianteira",
        "sub": "Revisão 3 · Rolamento cônico de duas carreiras · Aplicação: Volvo FH 540 6x2 e Scania R450 Highline",
        "paginas": [
            [("h", "1. Visão geral"),
             ("p", "O rolamento FRS-1001 é um rolamento cônico de duas carreiras, fornecido pré-lubrificado, para o cubo da "
                   "roda dianteira de caminhões pesados. Este manual cobre a inspeção, a montagem, o ajuste da porca do cubo e a "
                   "lubrificação do FRS-1001."),
             ("tabela", [["Especificação", "Valor"],
                         ["Tipo", "Cônico de duas carreiras"],
                         ["Aplicação", "Volvo FH 540 6x2 e Scania R450 Highline"],
                         ["Temperatura máxima de trabalho", "80 °C no cubo"],
                         ["Vida útil estimada", "300.000 km em uso rodoviário"]]),
             ("p", "Ferramentas necessárias: extrator de rolamento, prensa hidráulica e torquímetro.")],
            [("h", "2. Montagem e torque da porca do cubo"),
             ("num", ["Limpe o alojamento do cubo e remova resíduos de graxa antiga e partículas metálicas.",
                      "Aqueça o rolamento em aquecedor por indução até no máximo 110 °C. Nunca use maçarico.",
                      "Monte o rolamento com a prensa hidráulica, apoiando somente a pista interna, até o batente.",
                      "Aperte a porca do cubo com 450 Nm girando a roda, solte e reaperte com 150 Nm.",
                      "Meça a folga axial com relógio comparador: o valor correto fica entre 0,02 mm e 0,05 mm.",
                      "Trave a porca com arruela de travamento nova. Arruelas usadas não podem ser reaproveitadas."])],
            [("h", "3. Lubrificação"),
             ("p", "Use graxa à base de lítio EP2. Quantidade: 18 g por rolamento, preenchendo cerca de 2/3 do espaço interno "
                   "das carreiras. Intervalo de relubrificação: a cada 40.000 km. Em operação fora de estrada, reduza o "
                   "intervalo para 20.000 km."),
             ("lista", ["Não misture graxas de bases químicas diferentes.",
                        "Excesso de graxa causa superaquecimento do cubo.",
                        "Evite contaminação com água durante a lavagem do veículo."])],
            [("h", "4. Diagnóstico de problemas"),
             ("lista", ["Aquecimento acima de 80 °C após rodagem: excesso de graxa ou pré-carga incorreta no aperto do cubo.",
                        "Ruído tipo zumbido que aumenta com a velocidade: desgaste das pistas — substitua o rolamento.",
                        "Folga axial acima de 0,15 mm: substitua o rolamento e o retentor e abra um chamado técnico.",
                        "Manchas azuladas nas pistas indicam superaquecimento: o rolamento não pode ser reaproveitado."])],
        ],
    },
    {
        "pasta": "Rolamento de Transmissão",
        "arquivo": "FRS-1002 - Manual Técnico - Rolamento de Câmbio.pdf",
        "titulo": "Manual Técnico FRS-1002 — Rolamento de Câmbio",
        "sub": "Revisão 2 · Rolamento de esferas com anel de vedação · Aplicação: Mercedes-Benz Actros 2651 e DAF XF 480",
        "paginas": [
            [("h", "1. Visão geral"),
             ("p", "O FRS-1002 é um rolamento de esferas com anel de vedação, montado no eixo de entrada da caixa de câmbio. "
                   "Trabalha imerso no óleo da transmissão."),
             ("tabela", [["Especificação", "Valor"],
                         ["Aplicação", "Mercedes-Benz Actros 2651 e DAF XF 480"],
                         ["Lubrificação", "Óleo de transmissão SAE 75W-90 (imerso)"],
                         ["Folga radial máxima", "0,03 mm"],
                         ["Inspeção recomendada", "A cada 120.000 km"]])],
            [("h", "2. Montagem"),
             ("num", ["Verifique o alinhamento do eixo de entrada antes da montagem.",
                      "Se necessário, aqueça o rolamento até no máximo 90 °C para facilitar o encaixe.",
                      "Posicione com martelo de nylon, batendo de forma uniforme ao redor do anel — nunca num ponto só.",
                      "Aperte os parafusos da tampa do rolamento com 45 Nm, em sequência cruzada.",
                      "Confira a vedação: não pode haver folga entre o anel e o alojamento.",
                      "Substitua sempre o retentor junto com o rolamento."])],
            [("h", "3. Lubrificação e diagnóstico"),
             ("p", "O FRS-1002 não recebe graxa: é lubrificado pelo óleo SAE 75W-90 da transmissão e deve ser verificado a cada "
                   "troca de óleo do câmbio."),
             ("lista", ["Vibração no câmbio: desalinhamento do eixo ou folga excessiva de montagem.",
                        "Desgaste irregular: óleo contaminado ou nível abaixo do recomendado.",
                        "Ruído ao trocar de marcha com folga radial acima de 0,03 mm: substitua o rolamento."])],
        ],
    },
    {
        "pasta": "Rolamento de Eixo",
        "arquivo": "FRS-1003 - Manual Técnico - Rolamento de Diferencial.pdf",
        "titulo": "Manual Técnico FRS-1003 — Rolamento de Diferencial",
        "sub": "Revisão 4 · Rolamento cônico reforçado com porca de fixação · Aplicação: Iveco S-Way 460 e Scania R450",
        "paginas": [
            [("h", "1. Visão geral"),
             ("p", "O FRS-1003 é um rolamento cônico reforçado do pinhão do diferencial. A pré-carga é ajustada pela porca castelo."),
             ("tabela", [["Especificação", "Valor"],
                         ["Aplicação", "Iveco S-Way 460 e Scania R450"],
                         ["Lubrificação", "Óleo para diferencial SAE 85W-140"],
                         ["Troca de óleo", "A cada 80.000 km"],
                         ["Folga entre coroa e pinhão", "0,15 mm a 0,25 mm"]])],
            [("h", "2. Ajuste de pré-carga e torque da porca castelo"),
             ("num", ["Limpe o alojamento do diferencial e remova resíduos de graxa antiga.",
                      "Posicione o rolamento com a prensa, sem forçar lateralmente.",
                      "Aperte a porca castelo até 280 Nm girando o pinhão.",
                      "O torque de giro do pinhão deve ficar entre 1,5 Nm e 2,5 Nm. Acima de 2,5 Nm, alivie a porca e repita.",
                      "Trave a porca com contrapino novo.",
                      "Verifique a folga entre coroa e pinhão com relógio comparador (0,15 mm a 0,25 mm)."])],
            [("h", "3. Diagnóstico de problemas"),
             ("lista", ["Ruído (uivo) no diferencial em carga: folga incorreta entre coroa e pinhão.",
                        "Aquecimento da carcaça: nível de óleo abaixo do especificado ou óleo errado.",
                        "Nunca misture óleo mineral com sintético no mesmo diferencial."])],
        ],
    },
    {
        "pasta": "Rolamento de Suspensão",
        "arquivo": "FRS-1004 - Manual Técnico - Rolamento de Suspensão.pdf",
        "titulo": "Manual Técnico FRS-1004 — Rolamento de Suspensão",
        "sub": "Revisão 1 · Rolamento de agulhas com anel de retenção · Aplicação: Volvo FH 540 e DAF XF 480",
        "paginas": [
            [("h", "1. Visão geral"),
             ("p", "O FRS-1004 é um rolamento de agulhas com anel de retenção, usado na articulação do braço da suspensão."),
             ("tabela", [["Especificação", "Valor"],
                         ["Aplicação", "Volvo FH 540 e DAF XF 480"],
                         ["Lubrificação", "Graxa à base de lítio EP2"],
                         ["Quantidade de graxa", "12 g por rolamento"],
                         ["Intervalo de lubrificação", "A cada 30.000 km"]])],
            [("h", "2. Troca do rolamento"),
             ("num", ["Retire o anel de retenção antigo com o alicate de anel de retenção.",
                      "Remova o rolamento com o extrator.",
                      "Posicione o novo rolamento alinhado ao braço de suspensão.",
                      "Instale um anel de retenção NOVO — o anel antigo nunca deve ser reaproveitado.",
                      "Aperte o parafuso do braço da suspensão com 210 Nm.",
                      "Movimente o braço manualmente para checar folga residual."])],
            [("h", "3. Inspeção e diagnóstico"),
             ("p", "Verifique a folga radial a cada 60.000 km. Folga acima de 0,30 mm exige a troca do rolamento e do anel de retenção."),
             ("lista", ["Desgaste irregular dos pneus: alinhamento incorreto da suspensão.",
                        "Dificuldade de instalação: anel de retenção deformado ou alojamento com rebarbas."])],
        ],
    },
    {
        "pasta": "Rolamento de Motor",
        "arquivo": "FRS-1005 - Manual Técnico - Rolamento de Eixo Comando.pdf",
        "titulo": "Manual Técnico FRS-1005 — Rolamento de Eixo Comando",
        "sub": "Revisão 2 · Rolamento de esferas de precisão · Aplicação: Mercedes-Benz Actros 2651 e Iveco S-Way 460",
        "paginas": [
            [("h", "1. Visão geral"),
             ("p", "O FRS-1005 é um rolamento de esferas de precisão e baixo atrito do eixo comando de válvulas. É lubrificado "
                   "pela circulação do óleo do motor e não recebe graxa."),
             ("tabela", [["Especificação", "Valor"],
                         ["Aplicação", "Mercedes-Benz Actros 2651 e Iveco S-Way 460"],
                         ["Lubrificação", "Óleo do motor (circulação)"],
                         ["Folga axial do eixo comando", "0,10 mm a 0,20 mm"],
                         ["Força máxima de prensagem", "15 kN"]])],
            [("h", "2. Montagem"),
             ("num", ["Inspecione o eixo comando quanto a riscos ou desgaste antes de instalar o rolamento.",
                      "Monte com a prensa manual de bancada, apoiando SOMENTE o anel interno, com força máxima de 15 kN.",
                      "Instale o anel de trava e confirme o assentamento completo.",
                      "Aperte os parafusos do mancal com 25 Nm.",
                      "Gire o eixo manualmente: não pode haver travamento nem ruído."]),
             ("p", "O apoio incorreto durante a prensagem é a principal causa de dano ao FRS-1005.")],
        ],
    },
    {
        "pasta": "Relatórios Técnicos",
        "arquivo": "RT-2026-014 - Relatório de Falhas em Rolamentos de Cubo e Suspensão.pdf",
        "titulo": "Relatório Técnico RT-2026-014 — Falhas em Rolamentos de Cubo e Suspensão",
        "sub": "Período analisado: janeiro a junho de 2026 · 37 ocorrências em frotas parceiras (dados fictícios)",
        "paginas": [
            [("h", "1. Resumo"),
             ("p", "Este relatório analisa 37 ocorrências de falha prematura registradas entre janeiro e junho de 2026: "
                   "23 com o rolamento de roda dianteira FRS-1001 e 14 com o rolamento de suspensão FRS-1004."),
             ("tabela", [["Causa principal", "Ocorrências"],
                         ["Excesso de graxa no cubo (FRS-1001)", "11"],
                         ["Pré-carga incorreta na porca do cubo (FRS-1001)", "8"],
                         ["Contaminação na montagem (FRS-1001)", "4"],
                         ["Anel de retenção reaproveitado (FRS-1004)", "9"],
                         ["Suspensão desalinhada (FRS-1004)", "5"]])],
            [("h", "2. Caso típico: cubo aquecendo após a troca do FRS-1001"),
             ("p", "Sintoma relatado: o cubo esquenta muito nos primeiros 50 km após a troca do rolamento. Em 19 dos 23 casos, a "
                   "causa foi excesso de graxa ou pré-carga incorreta. Solução aplicada: retirar o excesso de graxa (deixar 18 g), "
                   "refazer o aperto da porca do cubo conforme o manual e medir a temperatura depois de 20 km."),
             ("h", "3. Caso típico: falha precoce do FRS-1004"),
             ("p", "Em 9 dos 14 casos, o anel de retenção antigo tinha sido reaproveitado e se soltou. Solução: sempre instalar "
                   "anel de retenção novo e conferir o encaixe completo no rasgo.")],
            [("h", "4. Recomendações"),
             ("num", ["Treinar as oficinas parceiras na dosagem de graxa (18 g no FRS-1001, 12 g no FRS-1004).",
                      "Incluir o torquímetro como item obrigatório no kit de montagem do FRS-1001.",
                      "Fornecer o anel de retenção junto com cada FRS-1004 vendido."])],
        ],
    },
    {
        "pasta": "Guias Gerais",
        "arquivo": "Guia Geral de Lubrificação e Armazenagem de Rolamentos.pdf",
        "titulo": "Guia Geral de Lubrificação e Armazenagem de Rolamentos",
        "sub": "Boas práticas válidas para todos os rolamentos da linha (não substitui o manual de cada peça)",
        "paginas": [
            [("h", "1. Armazenagem"),
             ("lista", ["Guarde os rolamentos na embalagem original, deitados, em local seco.",
                        "Umidade relativa do ar abaixo de 60% e temperatura entre 10 °C e 30 °C.",
                        "Prazo máximo de armazenagem: 3 anos a partir da data de fabricação.",
                        "Não lave rolamentos novos com solvente: isso remove a proteção de fábrica."])],
            [("h", "2. Boas práticas de lubrificação"),
             ("lista", ["Nunca misture graxas de bases químicas diferentes (por exemplo, lítio com cálcio).",
                        "Limpe a graxeira antes de aplicar graxa nova, para não empurrar sujeira para dentro.",
                        "Excesso de graxa aquece o rolamento tanto quanto a falta.",
                        "Registre a data e a quilometragem de cada lubrificação."]),
             ("h", "3. Descarte"),
             ("p", "Graxa e óleo usados devem ser entregues a coletor licenciado. Nunca descarte no esgoto ou no lixo comum.")],
        ],
    },
]


if __name__ == "__main__":
    base = Path(__file__).parent / "Manuais"
    for d in DOCUMENTOS:
        destino = base / d["pasta"] / d["arquivo"]
        montar(destino, d["titulo"], d["sub"], d["paginas"])
        print(f"{destino.relative_to(base.parent)}  ({len(d['paginas'])} páginas)")
