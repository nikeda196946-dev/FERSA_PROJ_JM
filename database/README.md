# FERSA Care — Banco de dados

Supabase (Postgres 15+) com a extensão `pgvector`, usada pelo RAG dos
manuais (`manuais.sql`).

## Como aplicar

No SQL Editor do seu projeto Supabase, nesta ordem:

1. `schema.sql` — tabelas, tipos, índices, trigger de protocolo do
   chamado e as políticas de RLS (Row Level Security).
2. `seed.sql` — as 5 peças fictícias do protótipo (FRS-1001 a FRS-1005),
   com aplicações, ferramentas, problemas, lubrificação, passos e vídeos.
3. `manuais.sql` — sistema de manuais com RAG: tabelas `categoria`,
   `manual` (com `status` processando/pronto/erro e `ativo`) e
   `manual_chunk` (texto + `embedding vector(768)` com índice HNSW) e
   `manual_peca` (qual manual trata de qual peça/Part Number), RLS ligado.
   Pode ser rodado de novo sem erro.
4. `cadastro.sql` — trigger que cria a linha em `profiles` (sempre como
   MECANICO) para quem se cadastra pela tela "Criar conta". Sem ele, a
   conta é criada no Supabase Auth mas não consegue entrar no FERSA Care.
   Pode ser rodado de novo sem erro.

> Para testar o cadastro sem precisar confirmar e-mail: Supabase →
> Authentication → Sign In / Providers → Email → desligue "Confirm email".
> Com a confirmação ligada, a tela avisa que o e-mail foi enviado.

## Usuários de teste

1. No Supabase, abra **Authentication → Users → Add user** e crie estas
   contas com **Auto Confirm User** marcado:

   | E-mail | Nome | Perfil |
   |---|---|---|
   | `mecanico@oficina.com` | Carlos Mendes | MECANICO |
   | `tecnico@fersa.com` | Renata Souza | TECNICO |
   | `admin@fersa.com` | Paulo Ferreira | ADMIN |

2. Escolha uma senha de teste para as contas (por exemplo,
   `FersaTeste2026!`; não reutilize em produção).
3. No **SQL Editor**, execute `test-users.sql`. Ele liga os perfis às
   contas Auth pelo e-mail e mostra o resultado da criação. As contas
   precisam existir antes de rodar o arquivo.
4. Entre pela tela do sistema com um desses e-mails e a senha definida.

O CMS de usuários ainda é demonstrativo e não cria contas reais. O login
é feito pelo Supabase Auth; `test-users.sql` só cria/atualiza os perfis
de domínio necessários para o backend reconhecer cada usuário.

## Validado contra um Postgres real

Todo o schema e o seed foram rodados e testados num Postgres 16 +
pgvector local antes de qualquer linha virar código do backend — joins,
o trigger que gera o protocolo (`FC-2026-123`), as políticas de RLS e uma
busca por similaridade vetorial usando o índice HNSW. Detalhes de cada
tabela estão comentados dentro do próprio `schema.sql`.

`manuais.sql` também foi rodado (duas vezes seguidas, para confirmar que é
idempotente) num Postgres 16 + pgvector local, e o backend fez upload,
ingestão e busca vetorial contra ele. As tabelas antigas `documentos` /
`documento_chunks` (vetor de 1536, pensadas para OpenAI) continuam no
`schema.sql`, mas o RAG não usa — o motivo está no topo do `manuais.sql`.
