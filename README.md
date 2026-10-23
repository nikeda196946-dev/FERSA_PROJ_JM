# FERSA Care

Plataforma de suporte técnico e pós-vendas para mecânicos que trabalham
com rolamentos — projeto acadêmico. Busca tradicional (Part Number,
aplicação, veículo, modelo, categoria, texto), ficha da peça, abertura e
atendimento de chamados já conectados ponta a ponta. O sistema de manuais
com RAG (upload de PDF → embeddings do Gemini → pgvector → `/api/chat`
com fontes) e o CMS de manuais/categorias já existem no backend; as telas
de IA e admin do Web ainda são demonstrativas até a semana 3 do
cronograma.

## Estrutura do repositório

```
fersa-care/
├── web/        → HTML/CSS/JS puro (sem build step)
├── backend/    → API REST, Java 21 + Spring Boot 3
├── database/   → schema.sql e seed.sql (Supabase/Postgres + pgvector)
└── docs/       → relatório de evolução do projeto
```

Cada pasta tem o próprio README com detalhes; este arquivo é só o mapa.

## Arquitetura

```
Web (navegador)
   │  login/senha direto no Supabase Auth
   │  demais chamadas: Bearer <access_token> do Supabase
   ▼
Backend Spring Boot  ──valida o JWT via JWKS──▶  Supabase Auth
   │        └──embeddings + resposta do chat──▶  Gemini (Google AI)
   ▼
Supabase Postgres (+ pgvector: chunks dos manuais)
```

O backend nunca emite os próprios tokens — quem autentica é o Supabase
Auth; a API só valida o token recebido e decide o que cada perfil
(mecânico, técnico, admin, cliente) pode fazer.

## Para rodar o projeto do zero

1. **Banco** — crie um projeto no [supabase.com](https://supabase.com),
   rode `database/schema.sql`, `database/seed.sql` e `database/manuais.sql`
   no SQL Editor.
   Crie os usuários de teste em Authentication → Add user (ver
   `database/seed.sql`, bloco final).
2. **Backend** — siga `backend/README.md` (`.env` a partir do
   `.env.example`, depois `./run.sh` ou `run.cmd`).
3. **Web** — siga `web/README.md` (`config.js` a partir do
   `config.example.js`, depois `npm run dev` ou Live Server).

## Stack

| Camada | Tecnologia |
|---|---|
| Web | HTML, CSS, JavaScript (sem framework) |
| Backend | Java 21, Spring Boot 3, Spring Security (Resource Server), JdbcTemplate |
| Banco | Supabase (Postgres + pgvector) |
| IA / RAG | Gemini (`gemini-embedding-001` + `gemini-2.5-flash`), PDFBox para ler os PDFs |
| Auth | Supabase Auth (JWT assinado, validado pelo backend via JWKS) |
| Android | planejado — consumirá a mesma API do backend |

## O que falta (nesta ordem)

1. Ligar o Web às rotas de manuais: busca + chat com fontes na área
   pública, e o CMS (tabela de manuais, categorias, upload) no admin
   (semana 3 do cronograma).
2. Importar do SharePoint (semana 3; se travar, fica só o upload manual).
3. CMS de escrita do catálogo de peças no backend.
4. Aplicativo Android, consumindo a mesma API.

## Segurança — resumo

- Segredos (senha do banco, chaves) só em `.env` / `web/js/config.js`,
  ambos fora do Git (ver `.gitignore`).
- Autorização por perfil feita no backend (`@PreAuthorize`), nunca só
  escondendo botão na interface. `/api/admin/**` só para ADMIN;
  `/api/chat`, `/api/manuais` e `/api/categorias` são públicos e só
  mostram manuais ativos e já processados.
- Mecânico só vê os próprios chamados; um chamado de outra pessoa
  devolve 404 (não 403), para não revelar nem que ele existe.
