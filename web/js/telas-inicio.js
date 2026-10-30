/* =====================================================================
   FERSA Care — telas: login, recuperar senha, início, busca
   ===================================================================== */

/* ---------------- Login ---------------- */
rota("/login", () => {
  montarConteudo(null, { semTopbar: true });
  document.getElementById("app").innerHTML = telaAcesso(`
        <div class="marca-login">Entrar</div>
        <p class="slogan">Use o e-mail e a senha da sua conta FERSA Care.</p>
        <form id="form-login" novalidate>
          <div class="campo" id="campo-email">
            <label for="email">E-mail</label>
            <input id="email" type="email" placeholder="seu.email@oficina.com" autocomplete="username" required>
          </div>
          <div class="campo" id="campo-senha">
            <label for="senha">Senha</label>
            <div class="senha-wrap">
              <input id="senha" type="password" placeholder="••••••••" autocomplete="current-password" required>
              <button type="button" id="btn-ver-senha">Mostrar</button>
            </div>
          </div>
          <button class="btn-principal btn-bloco" type="submit" id="btn-entrar">Entrar</button>
        </form>
        <div class="linha-entre">
          <a class="link-simples" href="#/recuperar-senha">Esqueci minha senha</a>
          <a class="link-simples" href="#/cadastro"><strong>Criar conta</strong></a>
        </div>
        <div class="acesso-publico">
          <a class="link-simples" href="#/ia">${ICONES.ia} Perguntar à IA sem entrar</a>
          <a class="link-simples" href="#/manuais">${ICONES.manual} Consultar manuais</a>
        </div>`);
  ligarLogin();
}, null);

function ligarLogin(){
  const form = document.getElementById("form-login");
  const campoEmail = document.getElementById("email");
  const campoSenha = document.getElementById("senha");

  document.getElementById("btn-ver-senha").addEventListener("click", (e) => {
    const mostrar = campoSenha.type === "password";
    campoSenha.type = mostrar ? "text" : "password";
    e.target.textContent = mostrar ? "Ocultar" : "Mostrar";
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    document.getElementById("campo-email").classList.remove("invalido");
    document.getElementById("campo-senha").classList.remove("invalido");
    const btn = document.getElementById("btn-entrar");
    btn.disabled = true; btn.textContent = "Entrando…";

    api.login(campoEmail.value, campoSenha.value).then(usuario => {
      Sessao.entrar(usuario);
      toast(`Bem-vindo, ${usuario.nome.split(" ")[0]}!`, "ok");
      irPara(usuario.perfil === "MECANICO" ? "/inicio" : "/admin");
    }).catch(err => {
      btn.disabled = false; btn.textContent = "Entrar";
      document.getElementById("campo-senha").classList.add("invalido");
      toast(err.message, "erro");
    });
  });
}

/* ---------------- Cadastro (pública) ----------------
   Conta nova no Supabase Auth; o perfil MECANICO é criado pelo trigger de
   database/cadastro.sql. Técnico/Admin só por promoção no CMS (Usuários). */
rota("/cadastro", () => {
  montarConteudo(null, { semTopbar: true });
  document.getElementById("app").innerHTML = telaAcesso(`
        <a class="voltar" href="#/login">${ICONES.setaEsq} Voltar ao login</a>
        <div class="marca-login">Criar conta</div>
        <p class="slogan" id="texto-cadastro">Para mecânicos e oficinas. Com a conta você abre chamados e acompanha as soluções.</p>
        <form id="form-cadastro" novalidate>
          <div class="campo" id="campo-cad-nome">
            <label for="cad-nome">Nome</label>
            <input id="cad-nome" type="text" placeholder="Seu nome" autocomplete="name" maxlength="120" required>
          </div>
          <div class="campo" id="campo-cad-email">
            <label for="cad-email">E-mail</label>
            <input id="cad-email" type="email" placeholder="seu.email@oficina.com" autocomplete="email" required>
          </div>
          <div class="campo" id="campo-cad-senha">
            <label for="cad-senha">Senha <span class="opcional">(mínimo 6 caracteres)</span></label>
            <div class="senha-wrap">
              <input id="cad-senha" type="password" autocomplete="new-password" required>
              <button type="button" id="btn-ver-senha-cad">Mostrar</button>
            </div>
          </div>
          <div class="campo" id="campo-cad-senha2">
            <label for="cad-senha2">Confirmar senha</label>
            <input id="cad-senha2" type="password" autocomplete="new-password" required>
          </div>
          <button class="btn-principal btn-bloco" type="submit" id="btn-cadastrar">Criar conta</button>
        </form>
        <p class="nota-login">Já tem conta? <a class="link-simples" href="#/login">Entrar</a></p>`);
  ligarCadastro();
}, null);

function ligarCadastro(){
  const form = document.getElementById("form-cadastro");
  const senha = document.getElementById("cad-senha");
  const senha2 = document.getElementById("cad-senha2");
  document.getElementById("btn-ver-senha-cad").addEventListener("click", (e) => {
    const mostrar = senha.type === "password";
    senha.type = senha2.type = mostrar ? "text" : "password";
    e.target.textContent = mostrar ? "Ocultar" : "Mostrar";
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const nome = document.getElementById("cad-nome").value.trim();
    const email = document.getElementById("cad-email").value.trim();
    const erros = [
      ["campo-cad-nome", !nome, "Informe seu nome."],
      ["campo-cad-email", !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email), "Informe um e-mail válido."],
      ["campo-cad-senha", senha.value.length < 6, "A senha precisa ter pelo menos 6 caracteres."],
      ["campo-cad-senha2", senha.value !== senha2.value, "As senhas não são iguais."]
    ];
    erros.forEach(([id, invalido]) => document.getElementById(id).classList.toggle("invalido", invalido));
    const primeiro = erros.find(([, invalido]) => invalido);
    if(primeiro){ toast(primeiro[2], "erro"); return; }

    const btn = document.getElementById("btn-cadastrar");
    btn.disabled = true; btn.textContent = "Criando conta…";
    api.cadastrar(nome, email, senha.value).then(r => {
      if(r.precisaConfirmar){
        form.remove();
        const texto = document.getElementById("texto-cadastro");
        texto.innerHTML = `${ICONES.check} Conta criada! Enviamos um e-mail de confirmação para <strong>${esc(email)}</strong>. Depois de confirmar, entre com seu e-mail e senha.`;
        texto.style.color = "var(--success)";
        return;
      }
      Sessao.entrar(r.usuario);
      toast(`Conta criada. Bem-vindo, ${r.usuario.nome.split(" ")[0]}!`, "ok");
      irPara("/inicio");
    }).catch(err => {
      btn.disabled = false; btn.textContent = "Criar conta";
      toast(err.message, "erro");
    });
  });
}

/* ---------------- Recuperar senha ---------------- */
rota("/recuperar-senha", () => {
  montarConteudo(null, { semTopbar: true });
  document.getElementById("app").innerHTML = telaAcesso(`
        <a class="voltar" href="#/login">${ICONES.setaEsq} Voltar ao login</a>
        <div class="marca-login">Recuperar senha</div>
        <p class="slogan" id="texto-recuperar">Informe seu e-mail de acesso. Enviaremos um link para você criar uma nova senha.</p>
        <form id="form-recuperar" novalidate>
          <div class="campo">
            <label for="email-rec">E-mail</label>
            <input id="email-rec" type="email" placeholder="seu.email@oficina.com" required>
          </div>
          <button class="btn-principal btn-bloco" type="submit" id="btn-recuperar">Enviar link de recuperação</button>
        </form>
        <p class="nota-login">Se o endereço estiver cadastrado, o Supabase enviará as instruções de recuperação.</p>`);
  document.getElementById("form-recuperar").addEventListener("submit", (e) => {
    e.preventDefault();
    const email = document.getElementById("email-rec").value;
    const btn = document.getElementById("btn-recuperar");
    btn.disabled = true; btn.textContent = "Enviando…";
    api.recuperarSenha(email).then(() => {
      document.getElementById("form-recuperar").innerHTML = "";
      document.getElementById("texto-recuperar").innerHTML =
        `${ICONES.check} Se este e-mail estiver cadastrado, um link de recuperação foi enviado. Verifique também a caixa de spam.`;
      document.getElementById("texto-recuperar").style.color = "var(--success)";
    }).catch(err => { btn.disabled = false; btn.textContent = "Enviar link de recuperação"; toast(err.message, "erro"); });
  });
}, null);

/* ---------------- Início (Home do mecânico) ---------------- */
rota("/inicio", () => {
  montarConteudo(estadoCarregando("Carregando seu painel…"));
  api.consultasRecentes().then(recentes => {
    const sessao = Sessao.atual();
    const opcoes = [
      { icone: "instalar", label: "Instalar", destino: "/busca/instalacao" },
      { icone: "lubrificar", label: "Lubrificar", destino: "/busca/lubrificacao" },
      { icone: "ferramentas", label: "Ferramentas", destino: "/busca/ferramentas" },
      { icone: "problema", label: "Estou com um problema", destino: "/busca/problemas", alerta: true },
      { icone: "manual", label: "Consultar manual", destino: "/manuais" },
      { icone: "tecnico", label: "Falar com técnico", destino: "/chamado/novo" }
    ];
    montarConteudo(`
      <div class="conteudo">
        <div class="cabecalho-inicio">
          <span class="eyebrow">Painel do mecânico</span>
          <h1>Olá, ${esc((sessao.nome || "").split(" ")[0])}</h1>
          <p>O que você precisa? Digite o Part Number, ou escolha uma opção abaixo.</p>
        </div>

        <form id="form-busca-home">
          <div class="busca-home">
            <input id="campo-busca-home" type="text" placeholder="Ex: FRS-1001, Scania R450, rolamento de roda…" autocomplete="off">
            <button type="submit">${ICONES.busca} Buscar</button>
          </div>
        </form>
        <p class="dica-busca">Não sabe o Part Number? <a href="#/busca/geral">Buscar por aplicação, veículo ou descrição</a> · ou <a href="#/ia">pergunte em linguagem natural</a>.</p>

        <a class="card-ia" href="#/ia">
          ${ICONES.ia}
          <div><strong>Perguntar à IA</strong><p>Descreva o problema com suas palavras e receba uma resposta com a fonte no manual.</p></div>
          ${ICONES.seta}
        </a>

        <div class="grade-opcoes">
          ${opcoes.map(o => `
            <a class="opcao ${o.alerta ? "opcao-alerta" : ""}" href="#${o.destino}">
              ${ICONES[o.icone]}<span>${esc(o.label)}</span>
            </a>`).join("")}
        </div>

        <div class="retomar">
          <div>
            <h3>Consultadas recentemente</h3>
            ${recentes.length ? `<ul class="lista-simples">${recentes.map(p => `
              <li><a href="#/peca/${p.id}">
                <span><span class="pn">${esc(p.partNumber)}</span> <span class="sub txt">${esc(p.nome)}</span></span>
                ${ICONES.seta}
              </a></li>`).join("")}</ul>`
              : `<p style="font-size:0.88rem;">Nenhuma peça consultada ainda nesta sessão.</p>`}
          </div>
          <div>
            <h3>Atalhos úteis</h3>
            <ul class="lista-simples">
              <li><a href="#/chamados">Acompanhar meus chamados ${ICONES.seta}</a></li>
              <li><a href="#/busca/geral">Ver todas as peças ${ICONES.seta}</a></li>
            </ul>
          </div>
        </div>
      </div>`);

    document.getElementById("form-busca-home").addEventListener("submit", (e) => {
      e.preventDefault();
      const termo = document.getElementById("campo-busca-home").value.trim();
      irPara("/busca/geral?q=" + encodeURIComponent(termo));
    });
  });
}, ["MECANICO"]);

/* ---------------- Busca de peças ---------------- */
const MODOS = [
  { id: "geral", label: "Tudo" },
  { id: "partNumber", label: "Part Number" },
  { id: "aplicacao", label: "Aplicação" },
  { id: "veiculo", label: "Veículo" },
  { id: "modelo", label: "Modelo" },
  { id: "categoria", label: "Categoria" }
];
const CONTEXTO_FLUXO = {
  instalacao: "Mostrando o atalho de Instalação na ficha da peça.",
  lubrificacao: "Mostrando o atalho de Lubrificação na ficha da peça.",
  ferramentas: "Mostrando o atalho de Ferramentas na ficha da peça.",
  problemas: "Mostrando o atalho de Problemas comuns na ficha da peça.",
  manual: "Mostrando o atalho do Manual na ficha da peça."
};

rota("/busca/:fluxo", (params) => {
  const fluxo = params.fluxo === "geral" ? "" : params.fluxo;
  const qInicial = obterQuery().get("q") || "";
  montarConteudo(`
    <div class="conteudo">
      <a class="voltar" href="#/inicio">${ICONES.setaEsq} Voltar ao início</a>
      <h2>Buscar peça</h2>
      <p style="margin:6px 0 16px;">Use o Part Number, se tiver, ou busque por aplicação, veículo, modelo, categoria ou texto livre.</p>
      ${CONTEXTO_FLUXO[fluxo] ? `<div class="contexto-fluxo">${ICONES.info}${esc(CONTEXTO_FLUXO[fluxo])}</div>` : ""}

      <form id="form-busca">
        <div class="barra-busca">
          <input id="campo-busca" type="text" placeholder="Ex: FRS-1001, Scania R450, rolamento de roda…" autocomplete="off" value="${esc(qInicial)}">
          <button type="submit">Buscar</button>
        </div>
      </form>
      <div class="modos-busca" id="modos-busca" role="tablist">
        ${MODOS.map((m, i) => `<button class="modo ${i === 0 ? "ativo" : ""}" data-modo="${m.id}" type="button">${esc(m.label)}</button>`).join("")}
      </div>
      <p class="total-resultados" id="total-resultados"></p>
      <div class="lista-resultados" id="lista-resultados"></div>
    </div>`);
  ligarBusca(fluxo);
}, ["MECANICO"]);

function ligarBusca(fluxo){
  let modoAtivo = "geral";
  const campo = document.getElementById("campo-busca");
  const lista = document.getElementById("lista-resultados");
  const total = document.getElementById("total-resultados");

  function executarBusca(){
    lista.innerHTML = `<div class="esqueleto"></div><div class="esqueleto"></div><div class="esqueleto"></div>`;
    total.textContent = "";
    api.buscarPecas(campo.value, modoAtivo).then(renderResultados).catch(() => {
      lista.innerHTML = estadoErro({ titulo: "Não foi possível buscar", texto: "Tente novamente em instantes." });
    });
  }
  function renderResultados(pecas){
    if(pecas.length === 0){
      total.textContent = "";
      lista.innerHTML = estadoVazio({
        titulo: "Nenhuma peça encontrada",
        texto: "Tente outro termo, ou troque o modo de busca acima.",
        acaoHtml: `<a class="btn-secundario" href="#/ia">Perguntar à IA</a>`
      });
      return;
    }
    total.textContent = `${pecas.length} peça${pecas.length > 1 ? "s" : ""} encontrada${pecas.length > 1 ? "s" : ""}`;
    lista.innerHTML = pecas.map(p => `
      <div class="resultado-peca">
        <a class="info-peca" href="#/peca/${p.id}${fluxo ? "/" + fluxo : ""}" style="text-decoration:none;color:inherit;">
          <span class="pn">${esc(p.partNumber)}</span>
          <span class="nome">${esc(p.nome)}</span>
          <span class="meta">${esc(p.categoria)} · ${esc(p.aplicacoes.map(a => a.veiculo).join(", "))}</span>
        </a>
        <div class="acoes-card">
          <a class="btn-secundario btn-pequeno" href="#/peca/${p.id}${fluxo ? "/" + fluxo : ""}">Ver detalhes</a>
        </div>
      </div>`).join("");
  }

  document.getElementById("form-busca").addEventListener("submit", (e) => { e.preventDefault(); executarBusca(); });
  document.getElementById("modos-busca").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-modo]");
    if(!btn) return;
    document.querySelectorAll(".modo").forEach(m => m.classList.remove("ativo"));
    btn.classList.add("ativo");
    modoAtivo = btn.getAttribute("data-modo");
    executarBusca();
  });
  executarBusca();
}
