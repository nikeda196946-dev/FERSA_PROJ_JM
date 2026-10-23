-- =====================================================================
-- FERSA Care — Manuais + RAG (cronograma de 4 semanas, semana 1)
-- Rode DEPOIS de schema.sql. Pode ser rodado mais de uma vez sem erro
-- (tudo com "if not exists" / "drop ... if exists").
--
-- Fluxo que estas tabelas suportam:
--   admin sobe um PDF → linha em `manual` com status 'processando'
--   → backend extrai o texto (PDFBox), divide em chunks, gera os
--     embeddings (Gemini) e grava em `manual_chunk`
--   → status vira 'pronto' (ou 'erro', com a mensagem em mensagem_erro)
--
-- A busca do chat só enxerga chunks de manuais com ativo = true e
-- status = 'pronto' (e cuja categoria, se houver, também esteja ativa).
--
-- Por que tabelas novas e não `documentos`/`documento_chunks` do
-- schema.sql? Aquelas foram desenhadas para um modelo de 1536 dimensões
-- (OpenAI) e para um fluxo de rascunho/publicação do CMS de peças. O RAG
-- do cronograma usa o Gemini e só precisa de manual → chunks, então
-- ficou mais simples começar enxuto. As tabelas antigas continuam lá,
-- sem uso pelo RAG.
-- =====================================================================

create extension if not exists pgcrypto;
create extension if not exists vector;

-- ---------------------------------------------------------------------
-- Categorias de manual (ex: "Rolamento de Cubo", "Transmissão")
-- ---------------------------------------------------------------------
create table if not exists categoria (
  id uuid primary key default gen_random_uuid(),
  nome text not null unique,
  descricao text,
  ativo boolean not null default true,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- Manuais (um PDF = um manual)
-- O PDF original fica em arquivo_pdf para permitir "reprocessar" sem
-- pedir o upload de novo. Com 5 a 10 manuais isso cabe tranquilo no
-- plano gratuito do Supabase; se crescer, dá para mover para o Storage.
-- ---------------------------------------------------------------------
create table if not exists manual (
  id uuid primary key default gen_random_uuid(),
  titulo text not null,
  categoria_id uuid references categoria(id) on delete set null,
  nome_arquivo text not null,
  tamanho_bytes bigint not null,
  hash_sha256 text not null unique,            -- impede subir o mesmo PDF duas vezes
  arquivo_pdf bytea not null,
  total_paginas int,
  total_chunks int not null default 0,
  status text not null default 'processando'
    check (status in ('processando', 'pronto', 'erro')),
  mensagem_erro text,
  ativo boolean not null default true,
  origem text not null default 'upload'
    check (origem in ('upload', 'sharepoint')),
  sharepoint_item_id text,                     -- só quando origem = 'sharepoint' (semana 3)
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);
create index if not exists idx_manual_categoria on manual (categoria_id);
create index if not exists idx_manual_busca on manual (ativo, status);

-- ---------------------------------------------------------------------
-- Chunks com embedding
-- Dimensão 768 = gemini-embedding-001 com outputDimensionality = 768
-- (o modelo aceita 768, 1536 ou 3072; o índice HNSW do pgvector só vai
-- até 2000, e 768 já dá ótima qualidade com 4x menos espaço que 3072).
-- Se trocar o modelo ou a dimensão (fersa.gemini.embedding-dimensoes),
-- ajuste aqui e reprocesse todos os manuais — embeddings de modelos ou
-- dimensões diferentes não são comparáveis entre si.
-- ---------------------------------------------------------------------
create table if not exists manual_chunk (
  id uuid primary key default gen_random_uuid(),
  manual_id uuid not null references manual(id) on delete cascade,
  ordem int not null,
  pagina int,                                  -- página do PDF de onde veio o trecho (para citar a fonte)
  conteudo text not null,
  embedding vector(768) not null,
  criado_em timestamptz not null default now(),
  unique (manual_id, ordem)
);

-- Busca por similaridade de cosseno (operador <=> no SQL do backend)
create index if not exists idx_manual_chunk_embedding on manual_chunk
  using hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------
-- RLS ligado
-- O backend conecta com o usuário do banco (postgres), que ignora RLS —
-- então quem decide o que é público é a API (rotas /api/admin/** só
-- para ADMIN). Aqui o RLS é a segunda barreira: quem tentar ler essas
-- tabelas direto pela API REST do Supabase com a chave anon não vê
-- nada, nem o PDF nem os embeddings. Só ADMIN autenticado no Supabase
-- tem acesso direto.
-- ---------------------------------------------------------------------
alter table categoria enable row level security;
alter table manual enable row level security;
alter table manual_chunk enable row level security;

drop policy if exists "categoria_admin" on categoria;
create policy "categoria_admin" on categoria for all using (meu_perfil() = 'ADMIN');

drop policy if exists "manual_admin" on manual;
create policy "manual_admin" on manual for all using (meu_perfil() = 'ADMIN');

drop policy if exists "manual_chunk_admin" on manual_chunk;
create policy "manual_chunk_admin" on manual_chunk for all using (meu_perfil() = 'ADMIN');
