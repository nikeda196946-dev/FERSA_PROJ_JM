# FERSA Care — Web

HTML + CSS + JavaScript puro, sem build step. Fala com o backend (pasta
`../backend`) e com o Supabase Auth direto do navegador para login.

## O que já é real e o que ainda é demonstrativo

| Área | Rota | Login | Dados |
|---|---|---|---|
| Criar conta | `#/cadastro` | **não precisa** | reais — Supabase Auth; entra sempre como Mecânico (precisa de `database/cadastro.sql`) |
| Perguntar à IA (RAG) | `#/ia` | **não precisa** | reais — `POST /api/chat`, fontes com link para o PDF na página citada; logado, escolher a peça faz a busca começar pelos manuais dela |
| Ficha da peça → Manual | `#/peca/:id/manual` | sim | reais — manuais ligados ao Part Number da peça |
| Manuais | `#/manuais` | **não precisa** | reais — busca por título, categoria ou texto do manual |
| Busca e ficha de peça, chamados | `#/inicio`, `#/busca/…`, `#/peca/…`, `#/chamados` | sim | reais |
| CMS → Manuais | `#/admin/manuais` | ADMIN | reais — **importar do SharePoint**, enviar PDF (vários de uma vez), status ao vivo, trechos, editar, ativar/desativar, reprocessar, excluir |
| CMS → Categorias | `#/admin/categorias` | ADMIN | reais |
| CMS → Base de conhecimento | `#/admin/base-conhecimento` | ADMIN | reais — resumo da indexação e teste da busca |
| CMS → Testar IA | `#/admin/teste-ia` | ADMIN | reais — Gemini **livre** (responde qualquer coisa, sem manuais), com os manuais (RAG) ou os dois **lado a lado** |
| CMS → Usuários | `#/admin/usuarios` | ADMIN | reais — quem se cadastrou e troca de perfil (Mecânico/Técnico/Admin/Cliente) |
| CMS → Dashboard | `#/admin` | ADMIN/TÉCNICO | números dos manuais reais; o resto é demo |
| CMS → Peças, Aplicações, Tutoriais, Vídeos, Ferramentas, Problemas, Soluções | `#/admin/…` | ADMIN/TÉCNICO | **demo** (selo no menu, nada é salvo) |

Sem login, o topo mostra só "Perguntar à IA", "Manuais" e o botão "Entrar".
Quando o token do Supabase expira (1 hora), o front renova sozinho com o
refresh token; se não der, volta para o login.

## Configurar

```bash
cp js/config.example.js js/config.js
```

Edite `js/config.js` apenas se a API estiver em outro endereço. A URL do
Supabase e a chave pública são carregadas da API, que lê `SUPABASE_URL` e
`SUPABASE_ANON_KEY` no `.env` do backend:

```js
const FERSA_CONFIG = {
  apiUrl: "http://localhost:8080" // onde o backend Spring está rodando
};
```

Preencha `SUPABASE_URL` e `SUPABASE_ANON_KEY` em `backend/.env`. A chave
anon/publishable é pública e pode ser entregue ao navegador; nunca use
`service_role` ou uma chave `secret`.

`js/config.js` é ignorado pelo Git (ver `.gitignore` na raiz) — cada
desenvolvedor mantém o seu local, apontando para o próprio ambiente.

> A chave `anon` do Supabase é feita para ficar exposta no navegador (é
> assim que o Supabase funciona); quem protege os dados é o RLS do banco,
> não o sigilo dessa chave. Mesmo assim ela fica de fora do Git, porque
> cada ambiente (dev de cada um, homologação, produção) tem a sua.

## Rodar

Qualquer servidor estático funciona — este projeto não tem build step.

```bash
npm run dev        # sobe em http://localhost:5500
```

Ou, sem instalar nada, a extensão **Live Server** do VS Code, ou:

```bash
python3 -m http.server 5500
```

Em qualquer um dos casos, confirme que `FERSA_CORS_ORIGINS` no `.env` do
backend inclui a origem que você está usando (ex: `http://127.0.0.1:5500`).

## Estrutura

```
web/
├── index.html
├── css/app.css            → design system Fersa (tokens + componentes)
├── img/                   → logotipo e símbolo oficiais (vetores do Brand Manual) e favicon
└── js/
    ├── config.js          → local, fora do Git (criado a partir do .example)
    ├── config.example.js  → template committado
    ├── data.js             → dados fictícios das seções "demo" do CMS
    ├── icons.js            → ícones SVG inline
    ├── core.js              → sessão, roteador por hash, header, rodapé, tela de acesso, toasts, modal
    ├── api.js                → fala com o backend (login, peças, chamados, chat,
    │                           manuais, categorias); só as seções demo usam data.js
    ├── telas-inicio.js        → login, recuperar senha, início, busca
    ├── telas-peca.js           → ficha da peça e sub-telas
    ├── telas-ia.js              → Busca por IA (RAG real) e chamados
    ├── telas-manuais.js         → busca pública de manuais
    └── telas-admin.js            → CMS: manuais, categorias e base de conhecimento reais; demais seções demo
```

## Identidade visual

A interface segue o *Brand Manual Fersa 2025 (short version)*. Tudo fica
centralizado em `css/app.css`, na seção **1. Tokens**. Não use cores soltas
no CSS ou no JS: use as variáveis.

- **Cores:**
  - Fersa Origin `#003256` é a base institucional (header, botão primário, títulos).
  - Fersa Titanium `#F6F6F6` é o fundo das páginas.
  - Fersa Steel `#465055` é o texto.
  - Fersa Chrome `#85959C` é usado em ícones, bordas e textos auxiliares.
  - Fersa Electric `#00A9FF` é só destaque: item ativo, foco, indicadores e gráficos.
  - Para texto pequeno sobre fundo branco há variações com contraste AA:
    `--electric-ink` e `--text-secondary`.
- **Gradiente corporativo** (Origin → Electric): aparece só em pontos de destaque.
  São eles o painel do login, o card "Perguntar à IA", o KPI principal do
  dashboard, o player de vídeo e a linha superior dos modais.
- **Tipografia:** a fonte corporativa é a TT Hoves Pro, que tem licença e não está
  no projeto. A alternativa web é a **Inter**, pelo Google Fonts, com hierarquia
  única (`--fs-display`, `--fs-h1`… `--fs-caption`).
- **Logotipo** (`img/fersa-logo*.svg`) e **símbolo** (`img/fersa-simbolo*.svg`):
  - foram extraídos em vetor do próprio manual, sem redesenho;
  - use a versão Origin em fundo claro e a branca em fundo escuro;
  - nunca recolorir nem distorcer;
  - "Care" fica fora do logotipo, separado por respiro e um fio vertical.
- **Separador** (logo ✳ claim): usado no rodapé e no login com o slogan oficial
  "Excellence beyond bearings".
- **Ícones:** um único conjunto linear, em `js/icons.js`.
- **Padrões:**
  - status sempre com texto e ponto (`.badge` + `b-ok`, `b-info`, `b-alerta`, `b-erro`);
  - botões: primário, secundário, fantasma e perigo;
  - campos com estados de hover, foco (Electric), desabilitado, somente leitura, erro (`.invalido`) e sucesso (`.valido`).
- **Responsivo:**
  - 960, 900, 720, 560 e 420 px;
  - o menu do CMS vira gaveta abaixo de 900 px;
  - o mecânico ganha barra inferior abaixo de 720 px;
  - as tabelas viram cartões e os modais viram folha inferior no celular.
