/* =====================================================================
   FERSA Care — núcleo: sessão, roteador, toasts, modal, navegação
   ===================================================================== */

/* ---------------- Sessão (sobrevive ao F5 via sessionStorage) ---------------- */
const Sessao = {
  atual(){
    try{ return JSON.parse(sessionStorage.getItem("fersa_sessao") || "null"); }
    catch(e){ return null; }
  },
  entrar(usuario){
    try{ sessionStorage.setItem("fersa_sessao", JSON.stringify(usuario)); }catch(e){}
  },
  sair(){
    try{ sessionStorage.removeItem("fersa_sessao"); }catch(e){}
  }
};

/* ---------------- Roteador por hash ---------------- */
const ROTAS = []; // { padrao: RegExp, chaves: [...], perfis: [...] ou null, render: fn(params) }

// Abrem sem login: a área pública do sistema de manuais (o backend também
// libera /api/chat, /api/manuais e /api/categorias sem token).
const ROTAS_PUBLICAS = ["/login", "/cadastro", "/recuperar-senha", "/ia", "/manuais"];

function rota(padraoTexto, render, perfis){
  const chaves = [];
  const regexTexto = padraoTexto.replace(/:[a-zA-Z]+/g, (m) => {
    chaves.push(m.slice(1));
    return "([^/]+)";
  });
  ROTAS.push({ padrao: new RegExp("^" + regexTexto + "$"), chaves, render, perfis: perfis || null });
}

function irPara(hash){
  if(("#" + hash) === window.location.hash){ renderRota(); return; }
  window.location.hash = hash;
}

function renderRota(){
  const hash = window.location.hash.slice(1) || "/login";
  const semQuery = hash.split("?")[0];
  const sessao = Sessao.atual();

  if(!sessao && !ROTAS_PUBLICAS.includes(semQuery)){
    window.location.hash = "/login";
    return;
  }
  if(sessao && (semQuery === "/login" || semQuery === "/cadastro")){
    window.location.hash = sessao.perfil === "MECANICO" ? "/inicio" : "/admin";
    return;
  }

  for(const r of ROTAS){
    const m = semQuery.match(r.padrao);
    if(!m) continue;
    if(r.perfis && (!sessao || !r.perfis.includes(sessao.perfil))){
      montarConteudo(telaSemPermissao());
      return;
    }
    const params = {};
    r.chaves.forEach((chave, i) => { params[chave] = decodeURIComponent(m[i + 1]); });
    const app = document.getElementById("app");
    app.innerHTML = "";
    r.render(params);
    window.scrollTo(0, 0);
    return;
  }
  montarConteudo(telaNaoEncontrada());
}
window.addEventListener("hashchange", renderRota);

function telaSemPermissao(){
  return `<div class="conteudo"><div class="estado erro">${ICONES.alerta}<h3>Sem permissão</h3><p>Seu perfil não tem acesso a esta área.</p>
    <div class="estado-acoes"><a class="btn-principal" href="#/inicio">Voltar ao início</a></div></div></div>`;
}
function telaNaoEncontrada(){
  return `<div class="conteudo"><div class="estado">${ICONES.vazio}<h3>Página não encontrada</h3><p>O endereço acessado não existe.</p>
    <div class="estado-acoes"><a class="btn-principal" href="#/inicio">Voltar ao início</a></div></div></div>`;
}

/* ---------------- Layout: helpers para montar uma tela completa ---------------- */
function montarConteudo(htmlMiolo, opcoes){
  const app = document.getElementById("app");
  const semTopbar = opcoes && opcoes.semTopbar;
  const semRodape = semTopbar || (opcoes && opcoes.semRodape);
  document.body.classList.remove("menu-aberto");
  app.innerHTML = `
    ${semTopbar ? "" : topbar(opcoes)}
    <main id="conteudo" tabindex="-1">${htmlMiolo}</main>
    ${semRodape ? "" : rodape()}
    ${semTopbar ? "" : navInferior()}
  `;
  ligarTopbar();
}

/* Logotipo oficial (vetor extraído do Brand Manual): Fersa Origin em fundo
   claro, branco em fundo escuro. Nunca recolorir, distorcer ou compor. */
function logoFersa(variante, classe){
  const arquivo = variante === "branco" ? "img/fersa-logo-branco.svg" : "img/fersa-logo.svg";
  return `<img class="${classe || "logo"}" src="${arquivo}" alt="Fersa" width="516" height="83">`;
}
function simboloFersa(variante, classe){
  const arquivo = variante === "branco" ? "img/fersa-simbolo-branco.svg" : "img/fersa-simbolo.svg";
  return `<img class="${classe || "simbolo"}" src="${arquivo}" alt="" aria-hidden="true" width="584" height="658">`;
}

/* Rodapé: logo + separador (símbolo) + claim oficial, como no manual. */
function rodape(){
  return `
    <footer class="rodape">
      <div class="assinatura">${logoFersa("origin", "logo")}${simboloFersa("origin")}<span>Excellence beyond bearings</span></div>
      <span>FERSA Care · suporte técnico de rolamentos</span>
    </footer>`;
}

/* Telas de acesso (login, cadastro, recuperar senha): marca à esquerda,
   formulário à direita. */
function telaAcesso(htmlPainel){
  return `
    <div class="tela-acesso">
      <section class="acesso-marca" aria-label="Fersa">
        ${logoFersa("branco")}
        <div>
          <span class="eyebrow">FERSA Care</span>
          <h1 class="titulo-acesso">Suporte técnico de rolamentos, direto na oficina.</h1>
          <p class="descricao">Busca por Part Number, manuais técnicos e respostas da IA com a fonte no manual, para quem trabalha com rolamentos todos os dias.</p>
        </div>
        <div class="assinatura">${simboloFersa("branco")}<span>Excellence beyond bearings</span></div>
      </section>
      <section class="acesso-form">
        <div class="painel-login">${htmlPainel}</div>
      </section>
    </div>`;
}

function rotaAtualBase(){
  return (window.location.hash.slice(1) || "").split("/")[1] || "";
}

function topbar(){
  const sessao = Sessao.atual();
  const base = rotaAtualBase();
  if(!sessao) return topbarPublica(base);
  const itens = [
    { href: "#/inicio", rot: "inicio", label: "Início" },
    { href: "#/busca/geral", rot: "busca", label: "Buscar peça" },
    { href: "#/ia", rot: "ia", label: "Perguntar à IA" },
    { href: "#/manuais", rot: "manuais", label: "Manuais" },
    { href: "#/chamados", rot: "chamados", label: "Meus chamados" }
  ];
  const ehAdmin = sessao && sessao.perfil !== "MECANICO";
  const noCms = base === "admin";
  return `
    <header class="topbar">
      ${noCms ? `<button class="btn-menu" type="button" data-acao="menu" aria-label="Abrir menu" aria-expanded="false" aria-controls="menu-admin">${ICONES.menu}</button>` : ""}
      <a class="marca" href="${ehAdmin ? "#/admin" : "#/inicio"}" aria-label="FERSA Care — início">
        ${logoFersa("branco")}<span class="produto">Care</span>
      </a>
      ${ehAdmin ? "" : `<nav class="nav-principal" aria-label="Navegação principal">${itens.map(i => `<a href="${i.href}" class="${base === i.rot ? "ativo" : ""}"${base === i.rot ? ' aria-current="page"' : ""}>${esc(i.label)}</a>`).join("")}</nav>`}
      <div class="acoes">
        ${ehAdmin ? `<a class="btn-texto ver-mecanico" href="#/inicio">${ICONES.olho}<span class="rotulo-curto">Ver como mecânico</span></a><span class="separador-v" aria-hidden="true"></span>` : ""}
        <span class="usuario">${esc(sessao.nome)}<small>${esc(rotuloPerfil(sessao.perfil))}</small></span>
        <button class="btn-texto" data-acao="sair" aria-label="Sair">${ICONES.sair}<span class="rotulo-curto">Sair</span></button>
      </div>
    </header>`;
}
/* Sem login: só a área pública (chat e manuais) e o botão de entrar. */
function topbarPublica(base){
  const itens = [
    { href: "#/ia", rot: "ia", label: "Perguntar à IA" },
    { href: "#/manuais", rot: "manuais", label: "Manuais" }
  ];
  return `
    <header class="topbar topbar-publica">
      <a class="marca" href="#/ia" aria-label="FERSA Care">${logoFersa("branco")}<span class="produto">Care</span></a>
      <nav class="nav-principal" aria-label="Navegação principal">${itens.map(i => `<a href="${i.href}" class="${base === i.rot ? "ativo" : ""}"${base === i.rot ? ' aria-current="page"' : ""}>${esc(i.label)}</a>`).join("")}</nav>
      <div class="acoes"><a class="btn-secundario btn-pequeno" href="#/login">${ICONES.usuario} Entrar</a></div>
    </header>`;
}

function navInferior(){
  const sessao = Sessao.atual();
  if(!sessao || sessao.perfil !== "MECANICO") return "";
  const base = rotaAtualBase();
  const itens = [
    { href: "#/inicio", rot: "inicio", label: "Início", ico: ICONES.dashboard },
    { href: "#/busca/geral", rot: "busca", label: "Buscar", ico: ICONES.busca },
    { href: "#/ia", rot: "ia", label: "IA", ico: ICONES.ia },
    { href: "#/manuais", rot: "manuais", label: "Manuais", ico: ICONES.manual },
    { href: "#/chamados", rot: "chamados", label: "Chamados", ico: ICONES.chamado }
  ];
  return `<nav class="nav-inferior" aria-label="Navegação">${itens.map(i => `<a href="${i.href}" class="${base === i.rot ? "ativo" : ""}"${base === i.rot ? ' aria-current="page"' : ""}>${i.ico}${esc(i.label)}</a>`).join("")}</nav>`;
}
function rotuloPerfil(p){
  return { MECANICO: "Mecânico", TECNICO: "Técnico", ADMIN: "Administrador", CLIENTE: "Cliente" }[p] || p;
}
function ligarTopbar(){
  const btn = document.querySelector('[data-acao="sair"]');
  if(btn) btn.addEventListener("click", () => { Sessao.sair(); irPara("/login"); });
  // CMS no celular/tablet: o menu lateral vira gaveta
  const menu = document.querySelector('[data-acao="menu"]');
  if(menu) menu.addEventListener("click", () => alternarMenuAdmin());
}
function alternarMenuAdmin(abrir){
  const aberto = typeof abrir === "boolean" ? abrir : !document.body.classList.contains("menu-aberto");
  document.body.classList.toggle("menu-aberto", aberto);
  const btn = document.querySelector('[data-acao="menu"]');
  if(btn){ btn.setAttribute("aria-expanded", String(aberto)); btn.setAttribute("aria-label", aberto ? "Fechar menu" : "Abrir menu"); }
  if(aberto){ const primeiro = document.querySelector("#menu-admin a"); if(primeiro) primeiro.focus(); }
}

/* ---------------- Toasts ---------------- */
function toast(msg, tipo){
  let cont = document.getElementById("toasts");
  if(!cont){
    cont = document.createElement("div");
    cont.id = "toasts";
    document.body.appendChild(cont);
  }
  const el = document.createElement("div");
  el.className = "toast" + (tipo === "erro" ? " toast-erro" : tipo === "ok" ? " toast-ok" : "");
  el.textContent = msg;
  cont.appendChild(el);
  setTimeout(() => { el.classList.add("sai"); setTimeout(() => el.remove(), 320); }, 3600);
}

/* ---------------- Modal de confirmação ---------------- */
function abrirModalConfirmacao({ titulo, corpo, textoConfirmar, perigo, aoConfirmar }){
  const fundo = document.createElement("div");
  fundo.className = "modal-fundo";
  fundo.innerHTML = `
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-titulo">
      <h3 id="modal-titulo">${esc(titulo)}</h3>
      <p>${esc(corpo)}</p>
      <div class="modal-acoes">
        <button class="btn-fantasma" data-fechar>Cancelar</button>
        <button class="${perigo ? "btn-perigo-cheio" : "btn-principal"}" data-confirmar>${esc(textoConfirmar || "Confirmar")}</button>
      </div>
    </div>`;
  document.body.appendChild(fundo);
  const fechar = () => fundo.remove();
  fundo.addEventListener("click", (e) => { if(e.target === fundo) fechar(); });
  fundo.querySelector("[data-fechar]").addEventListener("click", fechar);
  fundo.querySelector("[data-confirmar]").addEventListener("click", () => { fechar(); aoConfirmar(); });
  fundo.querySelector("[data-confirmar]").focus();
}

/* Esc fecha o modal aberto (o de cima) e a gaveta do menu do CMS. */
document.addEventListener("keydown", (e) => {
  if(e.key !== "Escape") return;
  const fundos = document.querySelectorAll(".modal-fundo");
  const topo = fundos[fundos.length - 1];
  if(topo){
    const fechar = topo.querySelector("[data-fechar]");
    if(fechar) fechar.click(); else topo.remove();
    return;
  }
  if(document.body.classList.contains("menu-aberto")) alternarMenuAdmin(false);
});

/* ---------------- Delegação global de cliques em [data-destino] ---------------- */
document.addEventListener("click", (e) => {
  const el = e.target.closest("[data-destino]");
  if(el){ irPara(el.getAttribute("data-destino")); }
});

/* ---------------- Query string do hash (ex: #/ia?peca=3) ---------------- */
function obterQuery(){
  const h = window.location.hash.slice(1);
  const i = h.indexOf("?");
  return new URLSearchParams(i >= 0 ? h.slice(i + 1) : "");
}

/* ---------------- Utilitários de UI ---------------- */
function estadoCarregando(msg){
  return `<div class="estado"><div class="spinner"></div><p>${esc(msg || "Carregando…")}</p></div>`;
}
function estadoVazio({ titulo, texto, acaoHtml }){
  return `<div class="estado">${ICONES.vazio}<h3>${esc(titulo)}</h3><p>${esc(texto)}</p>${acaoHtml ? `<div class="estado-acoes">${acaoHtml}</div>` : ""}</div>`;
}
function estadoErro({ titulo, texto, acaoHtml }){
  return `<div class="estado erro">${ICONES.alerta}<h3>${esc(titulo || "Não foi possível carregar")}</h3><p>${esc(texto)}</p>${acaoHtml ? `<div class="estado-acoes">${acaoHtml}</div>` : ""}</div>`;
}
