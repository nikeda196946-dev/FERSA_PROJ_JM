# FERSA Care — Web

HTML + CSS + JavaScript puro, sem build step. Fala com o backend (pasta
`../backend`) e com o Supabase Auth direto do navegador para login.

## O que já é real e o que ainda é demonstrativo

| Área | Rota | Login | Dados |
|---|---|---|---|
| Perguntar à IA (RAG) | `#/ia` | **não precisa** | reais — `POST /api/chat`, fontes com link para o PDF na página citada |
| Manuais | `#/manuais` | **não precisa** | reais — busca por título, categoria ou texto do manual |
| Busca e ficha de peça, chamados | `#/inicio`, `#/busca/…`, `#/peca/…`, `#/chamados` | sim | reais |
| CMS → Manuais | `#/admin/manuais` | ADMIN | reais — enviar PDF (vários de uma vez), status ao vivo, trechos, editar, ativar/desativar, reprocessar, excluir |
| CMS → Categorias | `#/admin/categorias` | ADMIN | reais |
| CMS → Base de conhecimento | `#/admin/base-conhecimento` | ADMIN | reais — resumo da indexação e teste da busca |
| CMS → Dashboard | `#/admin` | ADMIN/TÉCNICO | números dos manuais reais; o resto é demo |
| CMS → Peças, Aplicações, Tutoriais, Vídeos, Ferramentas, Problemas, Soluções, Usuários | `#/admin/…` | ADMIN/TÉCNICO | **demo** (selo no menu, nada é salvo) |

Sem login, o topo mostra só "Perguntar à IA", "Manuais" e o botão "Entrar".
Quando o token do Supabase expira (1 hora), o front renova sozinho com o
refresh token; se não der, volta para o login.

## Configurar

```bash
cp js/config.example.js js/config.js
```

Edite `js/config.js`:

```js
const FERSA_CONFIG = {
  apiUrl: "http://localhost:8080",        // onde o backend Spring está rodando
  supabaseUrl: "https://SEU-PROJETO.supabase.co",
  supabaseAnonKey: "a chave anon/public do projeto" // NUNCA a service_role
};
```

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
├── css/app.css
└── js/
    ├── config.js          → local, fora do Git (criado a partir do .example)
    ├── config.example.js  → template committado
    ├── data.js             → dados fictícios das seções "demo" do CMS
    ├── icons.js            → ícones SVG inline
    ├── core.js              → sessão, roteador por hash, toasts, modal
    ├── api.js                → fala com o backend (login, peças, chamados, chat,
    │                           manuais, categorias); só as seções demo usam data.js
    ├── telas-inicio.js        → login, recuperar senha, início, busca
    ├── telas-peca.js           → ficha da peça e sub-telas
    ├── telas-ia.js              → Busca por IA (RAG real) e chamados
    ├── telas-manuais.js         → busca pública de manuais
    └── telas-admin.js            → CMS: manuais, categorias e base de conhecimento reais; demais seções demo
```
