# FERSA Care — Backend (Fase 3)

API REST em Java + Spring Boot, falando com o Supabase (Postgres). Este
pacote cobre **Segurança + Catálogo (busca e ficha da peça) + Chamados** e,
do cronograma de 4 semanas do sistema de manuais, **ingestão de PDF,
RAG (`/api/chat`) e o CMS de manuais/categorias** — ver a seção
[Manuais + RAG](#manuais--rag) abaixo.

## Por que JdbcTemplate e não JPA/Hibernate?

Decisão desta etapa, registrada aqui porque muda a cara do projeto: em vez
de `@Entity`/Hibernate, os repositórios usam `NamedParameterJdbcTemplate`
com SQL explícito. Dois motivos:

1. **Controle**: o schema tem coisas que o ORM deixa menos óbvias — uma
   chave gerada por trigger (`protocolo`), colunas `text[]` do Postgres,
   enums nativos (`prioridade_chamado`). Em SQL direto, cada uma dessas
   partes fica visível e comentada no código.
2. Cada query deste pacote foi **testada contra um Postgres 16 + pgvector
   real** (o mesmo schema.sql da Fase 2) antes de virar código Java —
   incluindo um detalhe que só aparece em runtime: bind de `String` contra
   coluna `enum` do Postgres falha sem `::prioridade_chamado` explícito.
   Isso é o tipo de erro que o Hibernate esconde atrás de anotação, e que
   eu não conseguiria pegar sem rodar de verdade.

Se a equipe preferir JPA mais pra frente, dá para trocar só a camada de
repositório — Service e Controller não mudam.

## Status da compilação

Confirmado: o projeto compila (`mvn compile` já rodou com sucesso neste
ambiente). Depois dessa confirmação, os pacotes foram reorganizados para
`controller` / `service` / `repository` / `model` (ver "Estrutura" abaixo)
e um bean de CORS foi extraído para `config/CorsConfig.java` — mudança só
de organização de arquivo, nenhuma lógica foi tocada: cada classe movida
foi comparada byte a byte com a versão anterior (revertendo o caminho do
pacote) antes de eu considerar a migração concluída. Ainda assim, rode
`mvn clean compile` de novo depois de puxar esta versão, por garantia.

## Pré-requisitos

- Java 21
- Maven (ou o wrapper `./mvnw`, se vierem a adicionar um)
- Projeto Supabase já criado, com `schema.sql`, `seed.sql` e `manuais.sql` rodados
- Uma chave do Gemini (https://aistudio.google.com/apikey) para a parte de manuais/RAG

## Variáveis de ambiente

Nenhum segredo vai pro repositório — `.env` está no `.gitignore` da raiz
do projeto, só `.env.example` (sem valor real) é commitado.

```bash
cp .env.example .env
```

| Variável | Onde encontrar no Supabase | Exemplo |
|---|---|---|
| `SUPABASE_DB_URL` | Project Settings → Database → Connection string (JDBC) | `jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres` |
| `SUPABASE_DB_USER` | idem | `postgres` |
| `SUPABASE_DB_PASSWORD` | a senha que você definiu ao criar o projeto | — |
| `SUPABASE_JWKS_URL` | é sempre a mesma URL, só troca o `<project-ref>` | `https://<project-ref>.supabase.co/auth/v1/jwks` |
| `SUPABASE_JWT_ISSUER` | idem | `https://<project-ref>.supabase.co/auth/v1` |
| `FERSA_CORS_ORIGINS` | origem do Web em dev | `http://127.0.0.1:5500,http://localhost:5500` |
| `PORT` | opcional — porta da API | `8080` |
| `GEMINI_API_KEY` | não é do Supabase: https://aistudio.google.com/apikey | — |
| `GEMINI_CHAT_MODEL` | opcional — modelo que escreve a resposta do chat | `gemini-flash-latest` |
| `GEMINI_CHAT_MODEL_RESERVA` | opcional — usado se o principal falhar (503, 429, 404, timeout); vazio = sem reserva | `gemini-flash-lite-latest` |
| `GEMINI_NIVEL_RACIOCINIO` | opcional — `thinkingLevel` dos modelos Gemini 3+ (`low`/`high`); vazio = não manda | `low` |
| `GEMINI_EMBEDDING_MODEL` | opcional — modelo de embeddings | `gemini-embedding-001` |
| `FERSA_UPLOAD_MAX` | opcional — tamanho máximo do PDF | `25MB` |
| `FERSA_RAG_SCORE_MINIMO` | opcional — similaridade mínima de um trecho (0 a 1); 0 = desligado | `0` |

## Como rodar

```bash
mvn clean compile   # confirma que compila no seu ambiente
./run.sh            # Linux/macOS — carrega .env e sobe em http://localhost:8080
run.cmd              # Windows — mesma coisa
```

`run.sh`/`run.cmd` só leem o `.env` e chamam `mvn spring-boot:run` — se
preferir, exporte as variáveis você mesmo e rode `mvn spring-boot:run`
direto, dá no mesmo.

Teste rápido (não exige token):

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Como testar os endpoints autenticados

Pegue um **access token** de um dos usuários de teste (os que você criou
no Supabase Auth na Fase 2) — mais simples é pelo Web: faça login no
protótipo, abra o DevTools → Application → Local Storage, ou chame direto
a API do Supabase:

```bash
curl -X POST "https://xxxxx.supabase.co/auth/v1/token?grant_type=password" \
  -H "apikey: <sua-anon-key>" \
  -H "Content-Type: application/json" \
  -d '{"email":"mecanico@oficina.com","password":"<a-senha-que-voce-definiu>"}'
# devolve { "access_token": "...", ... }
```

Com o token em mãos:

```bash
TOKEN="cole-aqui-o-access_token"

# quem eu sou
curl http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"

# busca tradicional — modo: partNumber | aplicacao | veiculo | modelo | categoria | geral
curl "http://localhost:8080/api/v1/pecas?termo=scania&modo=veiculo" -H "Authorization: Bearer $TOKEN"

# ficha completa da peça
curl "http://localhost:8080/api/v1/pecas/<id-da-peca>" -H "Authorization: Bearer $TOKEN"

# abrir chamado (precisa de um usuário MECANICO ou CLIENTE)
curl -X POST http://localhost:8080/api/v1/chamados \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"assunto":"Teste","descricao":"Descrição de teste","prioridade":"ALTA"}'

# listar meus chamados
curl http://localhost:8080/api/v1/chamados -H "Authorization: Bearer $TOKEN"

# técnico assume (precisa de um usuário TECNICO ou ADMIN)
curl -X POST http://localhost:8080/api/v1/chamados/<id>/assumir -H "Authorization: Bearer $TOKEN_TECNICO"

# técnico registra solução
curl -X POST http://localhost:8080/api/v1/chamados/<id>/solucao \
  -H "Authorization: Bearer $TOKEN_TECNICO" -H "Content-Type: application/json" \
  -d '{"solucao":"Pré-carga corrigida conforme torque do manual."}'
```

Esperado: sem token → `401`. Token de mecânico tentando `/assumir` → `403`
(corpo `ApiError` com `"erro": "Acesso negado"`). Mecânico tentando ver o
chamado de outro mecânico pelo id → `404` (de propósito — ver comentário
em `ChamadoService.buscarDetalhe`).

## Endpoints desta entrega

| Método | Rota | Quem acessa |
|---|---|---|
| GET | `/actuator/health` | público |
| GET | `/api/v1/auth/me` | qualquer autenticado |
| GET | `/api/v1/pecas?termo=&modo=` | qualquer autenticado |
| GET | `/api/v1/pecas/{id}` | qualquer autenticado |
| POST | `/api/v1/chamados` | MECANICO, CLIENTE |
| GET | `/api/v1/chamados` | qualquer autenticado (mecânico/cliente vê só os seus; técnico/admin vê todos) |
| GET | `/api/v1/chamados/{id}` | dono do chamado, ou TECNICO/ADMIN |
| POST | `/api/v1/chamados/{id}/assumir` | TECNICO, ADMIN |
| POST | `/api/v1/chamados/{id}/solucao` | TECNICO, ADMIN |
| POST | `/api/chat` | **público** — pergunta → resposta + fontes |
| GET | `/api/manuais?termo=&categoriaId=` | **público** — só manuais ativos e prontos |
| GET | `/api/manuais/{id}/pdf` | **público** — o PDF, para abrir a fonte citada |
| GET | `/api/categorias` | **público** — só categorias ativas |
| POST | `/api/admin/manuais` (multipart: `arquivo`, `titulo?`, `categoriaId?`) | ADMIN — responde 202, processa em segundo plano |
| GET | `/api/admin/manuais` · `/api/admin/manuais/{id}` | ADMIN |
| GET | `/api/admin/manuais/{id}/chunks` | ADMIN — conferir a ingestão |
| PUT | `/api/admin/manuais/{id}` (`titulo`, `categoriaId`) | ADMIN |
| PATCH | `/api/admin/manuais/{id}/ativo` (`ativo`) | ADMIN |
| POST | `/api/admin/manuais/{id}/reprocessar` | ADMIN — 202 |
| DELETE | `/api/admin/manuais/{id}` | ADMIN |
| GET · POST | `/api/admin/categorias` | ADMIN |
| GET · PUT · DELETE | `/api/admin/categorias/{id}` (`nome`, `descricao?`, `ativo?`) | ADMIN |

## Estrutura

```
src/main/java/com/fersacare/api/
├── FersaCareApplication.java
├── config/              → CorsConfig (quem pode chamar a API pelo navegador)
├── security/            → SecurityConfig, validação do JWT do Supabase, authorities por perfil
├── common/              → ApiError, exceções, GlobalExceptionHandler
├── usuarios/            → perfil do usuário logado (GET /auth/me)
│   ├── model/             → PerfilUsuario (enum), Profile
│   ├── repository/        → ProfileRepository
│   └── controller/        → AuthController
├── catalogo/            → peças, aplicações, ferramentas, problemas, lubrificação, passos, vídeos
│   ├── dto/                → contratos da API (também fazem o papel de "model" de leitura aqui —
│   │                         não há Entity JPA porque o módulo é só leitura, ver decisão acima)
│   ├── repository/         → SQL (PecaRepository, FerramentaRepository, …)
│   ├── service/            → PecaService (monta a ficha completa)
│   └── controller/         → PecaController
├── chamados/            → abrir, listar, assumir, registrar solução
│   ├── model/              → StatusChamado, PrioridadeChamado (enums)
│   ├── dto/                → contratos da API
│   ├── repository/         → ChamadoRepository, ChamadoHistoricoRepository
│   ├── service/            → ChamadoService (regras: quem vê o quê, transições de status)
│   └── controller/         → ChamadoController
├── ia/                  → GeminiClient (embeddings + geração), GeminiConfig
├── manuais/             → manuais em PDF, chunks e categorias
│   ├── model/              → StatusManual (PROCESSANDO, PRONTO, ERRO)
│   ├── dto/                → contratos da API
│   ├── repository/         → ManualRepository, ManualChunkRepository (busca vetorial), CategoriaRepository
│   ├── service/            → PdfExtractor (PDFBox), Chunker, IngestaoService (@Async),
│   │                         ManualService e CategoriaService (regras do CMS)
│   └── controller/         → AdminManualController, AdminCategoriaController, ManualPublicoController
└── rag/                 → ChatService (prompt + fontes), ChatController (/api/chat)
```

Nomenclatura em linha com o que foi combinado no início do projeto:
Controller / Service / Repository / Model / DTO / Config / Security.

## Manuais + RAG

Implementa as semanas 1 e 2 do cronograma (e a parte de backend do CMS da
semana 3). Antes de tudo, rode `database/manuais.sql` no SQL Editor do
Supabase e coloque `GEMINI_API_KEY` no `.env`.

### Como funciona

```
Admin sobe PDF ─▶ POST /api/admin/manuais ─▶ manual (status PROCESSANDO), responde 202
                                               │  em segundo plano (IngestaoService):
                                               ▼
              PDFBox (texto por página) ─▶ Chunker (~1200 caracteres, 200 de sobreposição)
                 ─▶ Gemini gemini-embedding-001 (768 dimensões, RETRIEVAL_DOCUMENT)
                 ─▶ manual_chunk (pgvector) ─▶ status PRONTO  (ou ERRO + mensagem)

Pergunta ─▶ POST /api/chat ─▶ embedding da pergunta (RETRIEVAL_QUERY)
   ─▶ top 5 por similaridade de cosseno, só manuais ativo = true e status = 'pronto'
   ─▶ prompt (regras + trechos [1]..[5] + pergunta) ─▶ Gemini Flash
   ─▶ { resposta, encontrou, fontes: [manual, página, trecho, score], modelo }
```

Decisões que valem saber:

- **"Não encontrei nos manuais"**: se a busca não acha nada, o modelo nem é
  chamado. Se acha trechos que não respondem, o prompt manda o modelo
  responder exatamente `Não encontrei essa informação nos manuais.` — e aí
  `encontrou` vem `false` e `fontes` vazio.
- **Fontes**: a resposta cita os trechos como `[1]`, `[2]`…; `fontes` traz
  só os citados (se o modelo não citar nenhum, traz todos os usados).
- **PDF escaneado** (só imagem) não tem texto: o manual vai para `ERRO` com
  uma mensagem explicando. Não há OCR — separe manuais com texto selecionável.
- **Mesmo PDF duas vezes** é recusado (hash SHA-256).
- **Reprocessar** que falha não apaga os chunks antigos — o manual só fica
  em `ERRO` (e fora da busca) até reprocessar com sucesso.
- **API reiniciou no meio de uma ingestão**: na subida, o que estava
  `PROCESSANDO` vira `ERRO` com "clique em reprocessar".
- **Login de admin** é o mesmo login do Supabase (não há senha própria no
  backend): o usuário precisa ter `profiles.perfil = 'ADMIN'`. `/api/admin/**`
  é barrado no `SecurityConfig` e de novo com `@PreAuthorize` nos controllers.
- **Cota do Gemini**: nos embeddings (ingestão), HTTP 429/5xx/timeout são
  repetidos até 4 vezes; erro definitivo (chave inválida etc.) vira `ERRO`
  no manual.
- **Modelo do chat e reserva**: o padrão é `gemini-flash-latest`, alias que
  o Google mantém apontando para o Flash mais novo (o `gemini-2.5-flash` já
  dá 404 para contas novas). Se ele responder 503 ("high demand"), 429, 404
  ou passar de 25 s, o chat usa o reserva `gemini-flash-lite-latest`. Se os
  dois falharem, o chat devolve `503`. O campo `modelo` da resposta diz
  qual modelo respondeu de fato (ex.: `gemini-3.8-flash`).
- **`thinkingLevel: low`**: os modelos Gemini 3+ "pensam" antes de
  responder; `low` deixa a resposta 2 a 3x mais rápida. Ao trocar para um
  modelo 2.5, deixe `GEMINI_NIVEL_RACIOCINIO` vazio (eles recusam o campo).

### Entrega da semana 1 — subir um PDF e ver os chunks

```bash
TOKEN_ADMIN="access_token de um usuário com perfil ADMIN"

# (opcional) criar uma categoria
curl -X POST http://localhost:8080/api/admin/categorias \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"nome":"Rolamento de Cubo"}'

# subir o PDF — responde 202 com status PROCESSANDO
curl -X POST http://localhost:8080/api/admin/manuais \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -F "arquivo=@/caminho/manual.pdf" -F "titulo=Manual FRS-1001"

# acompanhar até PRONTO (ou ERRO, com mensagemErro)
curl http://localhost:8080/api/admin/manuais/<id> -H "Authorization: Bearer $TOKEN_ADMIN"

# ver os chunks (texto, página e dimensoesEmbedding = 768)
curl http://localhost:8080/api/admin/manuais/<id>/chunks -H "Authorization: Bearer $TOKEN_ADMIN"
```

No Supabase dá para conferir direto: `select manual_id, ordem, pagina,
vector_dims(embedding) from manual_chunk;`

### Entrega da semana 2 — perguntar pelo Postman

Importe `postman/FERSA-Manuais-RAG.postman_collection.json` no Postman,
preencha as variáveis `baseUrl` e `tokenAdmin` e use a requisição
**Chat — perguntar**. Ou por curl (não precisa de token):

```bash
curl -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" \
  -d '{"pergunta":"Qual o torque da porca do cubo do FRS-1001?"}'
```

### Testado como

- `mvn test`: testes de unidade do Chunker, PdfExtractor (PDF gerado no
  teste), GeminiClient (JSON enviado/recebido, lotes de 100, erros) e
  ChatService (prompt, "não encontrei", fontes citadas).
- Ponta a ponta contra Postgres 16 + pgvector local com `schema.sql`,
  `seed.sql` e `manuais.sql`, com um servidor falso no lugar do Gemini e do
  JWKS do Supabase: upload, PDF escaneado, PDF duplicado, PDF de 40 páginas
  (257 chunks, 3 lotes de embedding), chat, ativar/desativar manual e
  categoria, reprocessar, excluir, 401/403 nas rotas admin.
- Com o **Gemini real** (23/10/2026): manual de teste de 4 páginas
  ingerido (4 chunks de 768 dimensões) e 7 perguntas — torque, graxa,
  aquecimento do cubo, montagem, folga axial e troca do rolamento do
  câmbio respondidas certo e com a página certa como fonte; "preço do
  rolamento" e "capital da França" voltaram "não encontrei". Nesse dia o
  `gemini-flash-latest` estava instável (503 de alta demanda e timeouts) e
  quase todas as respostas vieram do reserva, em 1 a 9 s. Se continuar
  assim perto da apresentação, use `GEMINI_CHAT_MODEL=gemini-flash-lite-latest`.

## Próximos passos

1. Rodar `database/manuais.sql` no Supabase, pôr a `GEMINI_API_KEY` no
   `.env` e subir os 5 a 10 manuais reais (semana 1).
2. Front da área pública (busca + chat com fontes) e do CMS consumindo as
   rotas acima (semana 3).
3. Importar do SharePoint: listar/baixar os PDFs de uma pasta e chamar
   `ManualService.enviar(nome, bytes, titulo, categoriaId)` — o mesmo
   caminho do upload (semana 3).
4. CMS de escrita do catálogo de peças (cadastrar/editar peça, ferramenta,
   problema) continua pendente.
