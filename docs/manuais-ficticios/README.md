# Manuais fictícios para o SharePoint de teste

Pacote de documentos **inventados** (uso acadêmico) para montar o SharePoint de
teste e rodar as 10 perguntas da semana 4. Os valores são diferentes por peça
de propósito: assim dá para mostrar que a IA não mistura um manual com outro.

```
Manuais/
├── Rolamento de Cubo/         FRS-1001 - Manual Técnico - Rolamento de Roda Dianteira.pdf   (4 págs)
├── Rolamento de Transmissão/  FRS-1002 - Manual Técnico - Rolamento de Câmbio.pdf           (3 págs)
├── Rolamento de Eixo/         FRS-1003 - Manual Técnico - Rolamento de Diferencial.pdf      (3 págs)
├── Rolamento de Suspensão/    FRS-1004 - Manual Técnico - Rolamento de Suspensão.pdf        (3 págs)
├── Rolamento de Motor/        FRS-1005 - Manual Técnico - Rolamento de Eixo Comando.pdf     (2 págs)
├── Relatórios Técnicos/       RT-2026-014 - Relatório de Falhas em Rolamentos de Cubo e Suspensão.pdf (3 págs)
└── Guias Gerais/              Guia Geral de Lubrificação e Armazenagem de Rolamentos.pdf    (2 págs)
```

Os Part Numbers FRS-1001 a FRS-1005 são os do `database/seed.sql`. Por isso,
ao importar, cada manual se liga sozinho à sua peça. O relatório cita
FRS-1001 e FRS-1004 e fica ligado às duas. O guia geral não cita nenhuma peça.

- `GABARITO.md`: as 10 perguntas, a resposta esperada e a página de cada uma.
  Serve para os slides.
- `gabarito.json`: o mesmo gabarito, lido pelo script de teste.
- `Manuais.zip`: a pasta `Manuais/` compactada, para baixar de uma vez.
- `exemplo-resultado-testes-ia.md`: uma rodada real do script (10/10).
- `gerar_pdfs.py`: gera os PDFs de novo, caso queiram mudar algum valor.

## 1. Subir no SharePoint de teste

1. No site de teste, abra a biblioteca **Documentos** e crie a pasta `Manuais`.
2. Arraste as 7 subpastas de `Manuais/` (ou descompacte o `Manuais.zip`) para
   dentro dela.
3. No `.env` do backend, configure:
   ```
   SHAREPOINT_PASTA=Manuais
   SHAREPOINT_CATEGORIA_PELA_PASTA=true
   ```
   Preencha também as outras variáveis `SHAREPOINT_*`, conforme
   `backend/README.md`, seção "Conectar ao SharePoint".
4. Reinicie o backend. No CMS, vá em **Manuais → Importar do SharePoint** e
   importe todos.
5. Espere os 7 ficarem PRONTOS (cerca de 30 s). Cada subpasta vira uma
   categoria, e a coluna de peças mostra os Part Numbers ligados.

Sem SharePoint? Também funciona subir os PDFs pelo CMS (**Manuais → Enviar
PDF**). Nesse caso a categoria é escolhida na tela.

## 2. Rodar as 10 perguntas

Com o backend no ar e os manuais prontos (Node 18 ou mais novo, sem instalar
nada), rode na raiz do repositório:

```
node scripts/testar-ia.mjs
```

O script faz as 10 perguntas do `gabarito.json` em `POST /api/chat`. As que
têm Part Number vão com a peça escolhida, como no chat. Para cada resposta,
ele confere se os termos esperados aparecem e se a fonte certa foi citada.
No fim, gera dois arquivos:

- `resultado-testes-ia.md`: tabela pronta para o README e os slides;
- `resultado-testes-ia.csv`: abre no Excel.

A conferência automática é só uma ajuda. Revisem cada resposta e anotem a
nota na coluna **Revisão**.

Opções:
- `--api https://sua-api` aponta para outra API;
- `--pausa 3000` espera mais entre as perguntas, caso a cota do Gemini
  reclame.

## 3. Mudar os PDFs

```
pip install reportlab
python gerar_pdfs.py        # dentro desta pasta; recria Manuais/
```

Se mudarem algum valor, atualizem também o `gabarito.json` e o `GABARITO.md`.
Depois, no SharePoint, basta substituir o arquivo: a próxima importação
percebe que ele mudou e reprocessa.
