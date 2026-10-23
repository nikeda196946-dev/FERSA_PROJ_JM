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
   `manual_chunk` (texto + `embedding vector(768)` com índice HNSW), RLS
   ligado. Pode ser rodado de novo sem erro.

O final do `seed.sql` tem um bloco comentado para ligar os usuários de
teste (mecânico/técnico/admin) depois de criá-los em
**Authentication → Add user** no painel do Supabase.

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
