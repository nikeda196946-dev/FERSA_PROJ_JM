/* =====================================================================
   FERSA Care — CMS (área administrativa)
   Reais (backend): Dashboard (números dos manuais), Manuais, Categorias,
   Base de conhecimento, Testar IA e Usuários — só ADMIN, igual ao backend
   (/api/admin/**).
   Demonstrativas (mock, nada é salvo): as seções marcadas com demo: true.
   ===================================================================== */

const MENU_ADMIN = [
  { id: "", label: "Dashboard", ico: "dashboard" },
  { id: "manuais", label: "Manuais", ico: "manual", perfis: ["ADMIN"] },
  { id: "categorias", label: "Categorias", ico: "base", perfis: ["ADMIN"] },
  { id: "base-conhecimento", label: "Base de conhecimento", ico: "base", perfis: ["ADMIN"] },
  { id: "teste-ia", label: "Testar IA", ico: "ia", perfis: ["ADMIN"] },
  { id: "usuarios", label: "Usuários", ico: "usuario", perfis: ["ADMIN"] },
  { id: "pecas", label: "Peças", ico: "peca", demo: true },
  { id: "aplicacoes", label: "Aplicações", ico: "peca", demo: true },
  { id: "tutoriais", label: "Tutoriais", ico: "instalar", demo: true },
  { id: "videos", label: "Vídeos", ico: "video", demo: true },
  { id: "ferramentas", label: "Ferramentas", ico: "ferramentas", demo: true },
  { id: "problemas", label: "Problemas", ico: "problema", demo: true },
  { id: "solucoes", label: "Soluções", ico: "check", demo: true },
  { id: "chamados", label: "Chamados", ico: "chamado", href: "#/chamados" }
];

function podeVerItemMenu(item, sessao){
  return !item.perfis || item.perfis.includes(sessao.perfil);
}

function montarAdmin(secaoAtiva, htmlDireita){
  const sessao = Sessao.atual();
  const item = MENU_ADMIN.find(i => i.id === secaoAtiva);
  const demo = item && item.demo;
  montarConteudo(`
    <div class="admin-grade">
      <aside class="admin-menu">
        <nav>
          ${MENU_ADMIN.filter(i => podeVerItemMenu(i, sessao)).map(i => `
            <a href="${i.href || "#/admin" + (i.id ? "/" + i.id : "")}" class="${secaoAtiva === i.id ? "ativo" : ""}">${ICONES[i.ico]}${esc(i.label)}${i.demo ? `<span class="selo-demo">demo</span>` : ""}</a>`).join("")}
        </nav>
      </aside>
      <div class="admin-conteudo">
        <span class="tag-cms" style="margin-bottom:12px;display:inline-block;">CMS · ${esc(rotuloPerfil(sessao.perfil))}</span>
        ${demo ? `<div class="aviso-prototipo" style="margin:0 0 16px;">${ICONES.info}<span>Seção demonstrativa: usa dados fictícios e nada do que for alterado aqui é salvo. As seções reais são Manuais, Categorias, Base de conhecimento, Testar IA e Usuários.</span></div>` : ""}
        ${htmlDireita}
      </div>
    </div>`);
}

function cabAdmin(titulo, subtitulo, acaoHtml){
  return `<div class="cab-tela"><div><h1>${esc(titulo)}</h1>${subtitulo ? `<p>${esc(subtitulo)}</p>` : ""}</div>${acaoHtml || ""}</div>`;
}

/* Resumo dos manuais reais (dashboard e base de conhecimento). */
function resumoManuais(manuais){
  return {
    total: manuais.length,
    prontos: manuais.filter(m => m.status === "PRONTO").length,
    disponiveis: manuais.filter(m => m.status === "PRONTO" && m.ativo).length,
    processando: manuais.filter(m => m.status === "PROCESSANDO").length,
    erro: manuais.filter(m => m.status === "ERRO").length,
    trechos: manuais.reduce((n, m) => n + (m.status === "PRONTO" ? m.totalChunks : 0), 0)
  };
}

/* ---------------- Dashboard ---------------- */
rota("/admin", () => {
  const sessao = Sessao.atual();
  montarAdmin("", estadoCarregando("Carregando dashboard…"));
  const manuaisReais = sessao.perfil === "ADMIN" ? api.cmsListarManuais().catch(() => null) : Promise.resolve(null);
  Promise.all([api.cmsEstatisticas(), manuaisReais]).then(([s, manuais]) => {
    const r = manuais ? resumoManuais(manuais) : null;
    const maiorCategoria = Math.max(1, ...s.porCategoria.map(([, n]) => n));
    montarAdmin("", `
      ${cabAdmin("Dashboard", "Visão geral do conteúdo disponível para o mecânico e para a IA.")}
      ${r ? `
      <h3 class="titulo-dash">Manuais e Busca por IA</h3>
      <div class="stats stats-4">
        <a class="stat destaque" href="#/admin/manuais"><div class="num">${r.disponiveis}/${r.total}</div><div class="rot">Manuais disponíveis para a IA</div><div class="sub">ativos e processados</div></a>
        <a class="stat" href="#/admin/base-conhecimento"><div class="num">${r.trechos}</div><div class="rot">Trechos indexados</div></a>
        <a class="stat" href="#/admin/manuais?status=PROCESSANDO"><div class="num">${r.processando}</div><div class="rot">Processando agora</div></a>
        <a class="stat ${r.erro ? "stat-erro" : ""}" href="#/admin/manuais?status=ERRO"><div class="num">${r.erro}</div><div class="rot">Com erro</div></a>
      </div>` : ""}

      <h3 class="titulo-dash">Catálogo e chamados <span class="selo-demo">demo</span></h3>
      <div class="stats">
        <a class="stat" href="#/admin/pecas"><div class="num">${s.totalPecas}</div><div class="rot">Peças cadastradas</div></a>
        <a class="stat" href="#/admin/tutoriais"><div class="num">${s.totalTutoriais}</div><div class="rot">Tutoriais</div></a>
        <a class="stat" href="#/admin/videos"><div class="num">${s.totalVideos}</div><div class="rot">Vídeos</div></a>
        <a class="stat" href="#/admin/ferramentas"><div class="num">${s.totalFerramentas}</div><div class="rot">Ferramentas</div></a>
        <a class="stat" href="#/admin/problemas"><div class="num">${s.totalProblemas}</div><div class="rot">Problemas cadastrados</div></a>
        <a class="stat" href="#/chamados"><div class="num">${s.chamadosAbertos}</div><div class="rot">Chamados em aberto</div></a>
      </div>

      <div class="dash-grade">
        <div class="painel">
          <h3>Peças por categoria <span class="selo-demo">demo</span></h3>
          <div class="barras">
            ${s.porCategoria.map(([cat, n]) => `
              <div class="barra-linha"><span>${esc(cat)}</span><div class="trilho"><i style="width:${(n / maiorCategoria) * 100}%"></i></div><span class="val">${n}</span></div>`).join("")}
          </div>
        </div>
        <div class="painel">
          <h3>Perguntas sem resposta suficiente <span class="selo-demo">demo</span></h3>
          <p style="margin-bottom:10px;">Sinal de conteúdo que ainda falta cadastrar (o histórico real de perguntas é um próximo passo).</p>
          <ul class="lista-lacunas">
            ${s.lacunasIA.map(l => `<li>${esc(l.pergunta)}<small>${esc(l.quando)}</small></li>`).join("")}
          </ul>
        </div>
      </div>`);
  });
}, ["ADMIN", "TECNICO"]);

/* ---------------- Peças (CRUD) ---------------- */
rota("/admin/pecas", () => {
  montarAdmin("pecas", estadoCarregando("Carregando peças…"));
  api.cmsListarPecas().then(pecas => renderListaPecasAdmin(pecas));
}, ["ADMIN", "TECNICO"]);

function renderListaPecasAdmin(pecas, termoAtual){
  montarAdmin("pecas", `
    ${cabAdmin("Peças", "Cadastro de peças e Part Numbers.", `<button class="btn-principal" id="btn-nova-peca">${ICONES.mais} Nova peça</button>`)}
    <div class="barra-ferramentas">
      <div class="campo-busca">${ICONES.busca}<input id="busca-pecas-admin" type="text" placeholder="Buscar por Part Number, nome ou categoria…" value="${esc(termoAtual || "")}"></div>
    </div>
    <div class="tabela-wrap">
      <table class="tabela">
        <thead><tr><th>Part Number</th><th>Nome</th><th>Categoria</th><th>Aplicações</th><th class="col-acoes">Ações</th></tr></thead>
        <tbody>
          ${pecas.map(p => `
            <tr>
              <td data-rot="Part Number" class="mono">${esc(p.partNumber)}</td>
              <td data-rot="Nome">${esc(p.nome)}</td>
              <td data-rot="Categoria">${esc(p.categoria)}</td>
              <td data-rot="Aplicações">${esc(p.aplicacoes.map(a => a.veiculo).join(", "))}</td>
              <td data-rot="Ações" class="col-acoes">
                <a class="btn-fantasma" href="#/peca/${p.id}" title="Visualizar como mecânico">${ICONES.olho}</a>
                <button class="btn-fantasma" data-editar="${p.id}" title="Editar">${ICONES.lapis}</button>
                <button class="btn-fantasma" data-excluir="${p.id}" data-nome="${esc(p.partNumber)}" title="Excluir">${ICONES.lixeira}</button>
              </td>
            </tr>`).join("")}
        </tbody>
      </table>
    </div>
    ${pecas.length === 0 ? estadoVazio({ titulo: "Nenhuma peça encontrada", texto: "Ajuste o termo buscado ou cadastre uma nova peça." }) : ""}
  `);

  document.getElementById("busca-pecas-admin").addEventListener("input", (e) => {
    const t = e.target.value.trim().toLowerCase();
    api.cmsListarPecas().then(todas => {
      const filtradas = todas.filter(p => !t || (p.partNumber + p.nome + p.categoria).toLowerCase().includes(t));
      renderListaPecasAdmin(filtradas, e.target.value);
      document.getElementById("busca-pecas-admin").focus();
    });
  });
  document.getElementById("btn-nova-peca").addEventListener("click", () => abrirFormularioPeca(null));
  document.querySelectorAll("[data-editar]").forEach(b => b.addEventListener("click", () => {
    const peca = pecas.find(p => p.id === Number(b.getAttribute("data-editar")));
    abrirFormularioPeca(peca);
  }));
  document.querySelectorAll("[data-excluir]").forEach(b => b.addEventListener("click", () => {
    abrirModalConfirmacao({
      titulo: "Excluir peça",
      corpo: `Tem certeza que deseja excluir a peça ${b.getAttribute("data-nome")}? Esta ação não pode ser desfeita.`,
      textoConfirmar: "Excluir", perigo: true,
      aoConfirmar: () => api.cmsExcluirPeca(b.getAttribute("data-excluir")).then(() => {
        toast("Peça excluída.", "ok");
        api.cmsListarPecas().then(renderListaPecasAdmin);
      })
    });
  }));
}

function abrirFormularioPeca(peca){
  const editando = !!peca;
  const fundo = document.createElement("div");
  fundo.className = "modal-fundo";
  fundo.innerHTML = `
    <div class="modal modal-larga" role="dialog" aria-modal="true">
      <div class="modal-cab">
        <h3>${editando ? "Editar peça" : "Nova peça"}</h3>
        <button class="btn-fantasma" data-fechar aria-label="Fechar">✕</button>
      </div>
      <form id="form-peca-admin" class="form-cms">
        <div class="linha-campos">
          <div class="campo"><label for="pf-pn">Part Number</label><input id="pf-pn" type="text" required value="${editando ? esc(peca.partNumber) : ""}"></div>
          <div class="campo"><label for="pf-categoria">Categoria</label><input id="pf-categoria" type="text" required value="${editando ? esc(peca.categoria) : ""}"></div>
        </div>
        <div class="campo"><label for="pf-nome">Nome / descrição</label><input id="pf-nome" type="text" required value="${editando ? esc(peca.nome) : ""}"></div>
        <div class="campo"><label for="pf-veiculo">Veículo / modelo principal <span class="opcional">(demais aplicações continuam no cadastro atual)</span></label><input id="pf-veiculo" type="text" placeholder="Ex: Volvo FH 540" value="${editando ? esc(peca.aplicacoes[0]?.veiculo || "") : ""}"></div>
        <div class="modal-acoes">
          <button type="button" class="btn-fantasma" data-fechar>Cancelar</button>
          <button type="submit" class="btn-principal">${editando ? "Salvar alterações" : "Cadastrar peça"}</button>
        </div>
      </form>
    </div>`;
  document.body.appendChild(fundo);
  const fechar = () => fundo.remove();
  fundo.addEventListener("click", (e) => { if(e.target === fundo) fechar(); });
  fundo.querySelectorAll("[data-fechar]").forEach(b => b.addEventListener("click", fechar));

  fundo.querySelector("#form-peca-admin").addEventListener("submit", (e) => {
    e.preventDefault();
    const dados = {
      id: editando ? peca.id : undefined,
      partNumber: fundo.querySelector("#pf-pn").value.trim(),
      categoria: fundo.querySelector("#pf-categoria").value.trim(),
      nome: fundo.querySelector("#pf-nome").value.trim()
    };
    const veiculo = fundo.querySelector("#pf-veiculo").value.trim();
    if(veiculo) dados.aplicacoes = [{ veiculo, modelo: veiculo }, ...(editando ? peca.aplicacoes.slice(1) : [])];
    api.cmsSalvarPeca(dados).then(() => {
      toast(editando ? "Peça atualizada." : "Peça cadastrada.", "ok");
      fechar();
      api.cmsListarPecas().then(renderListaPecasAdmin);
    });
  });
}

/* ---------------- Aplicações (derivado das peças) ---------------- */
rota("/admin/aplicacoes", () => {
  montarAdmin("aplicacoes", estadoCarregando());
  api.cmsListarPecas().then(pecas => {
    const linhas = pecas.flatMap(p => p.aplicacoes.map(a => ({ ...a, pn: p.partNumber, nome: p.nome, pecaId: p.id })));
    montarAdmin("aplicacoes", `
      ${cabAdmin("Aplicações", "Veículos e modelos vinculados a cada peça.")}
      <div class="tabela-wrap"><table class="tabela">
        <thead><tr><th>Veículo</th><th>Modelo</th><th>Peça vinculada</th><th class="col-acoes">Ações</th></tr></thead>
        <tbody>${linhas.map(l => `<tr>
          <td data-rot="Veículo">${esc(l.veiculo)}</td>
          <td data-rot="Modelo">${esc(l.modelo)}</td>
          <td data-rot="Peça">${esc(l.pn)} — ${esc(l.nome)}</td>
          <td data-rot="Ações" class="col-acoes"><a class="btn-fantasma" href="#/admin/pecas" title="Editar na peça">${ICONES.lapis}</a></td>
        </tr>`).join("")}</tbody>
      </table></div>`);
  });
}, ["ADMIN", "TECNICO"]);

/* ---------------- Manuais (real: /api/admin/manuais) ---------------- */
const ROTULO_STATUS_MANUAL = { PROCESSANDO: "Processando", PRONTO: "Pronto", ERRO: "Erro" };
const CLASSE_STATUS_MANUAL = { PROCESSANDO: "b-info", PRONTO: "b-ok", ERRO: "b-erro" };
let timerManuais = null; // atualização automática enquanto há manual processando

rota("/admin/manuais", () => {
  clearTimeout(timerManuais);
  montarAdmin("manuais", estadoCarregando("Carregando manuais…"));
  const estado = { manuais: [], categorias: [], termo: "", status: obterQuery().get("status") || "" };
  Promise.all([api.cmsListarManuais(), api.cmsListarCategorias()]).then(([manuais, categorias]) => {
    estado.manuais = manuais;
    estado.categorias = categorias;
    telaManuaisAdmin(estado);
  }).catch(err => montarAdmin("manuais", estadoErro({ titulo: "Não foi possível carregar os manuais", texto: err.message })));
}, ["ADMIN"]);

function telaManuaisAdmin(estado){
  montarAdmin("manuais", `
    ${cabAdmin("Manuais", "PDFs que alimentam a Busca por IA. Ao importar, o texto é extraído, dividido em trechos e indexado em segundo plano.",
      `<div class="grupo-botoes">
        <button class="btn-principal" id="btn-importar-sp">${ICONES.atualizar} Importar do SharePoint</button>
        <button class="btn-secundario" id="btn-enviar-pdf">${ICONES.mais} Enviar PDF</button>
      </div>`)}
    <div class="barra-ferramentas">
      <div class="campo-busca">${ICONES.busca}<input id="busca-manuais-admin" type="text" placeholder="Buscar por título, arquivo, categoria ou Part Number…"></div>
      <button class="btn-secundario" id="btn-detectar-pecas" title="Procura os Part Numbers do catálogo no texto de todos os manuais">${ICONES.peca} Detectar peças nos manuais</button>
      <select id="filtro-status-manual" aria-label="Filtrar por status">
        <option value="">Todos os status</option>
        ${Object.entries(ROTULO_STATUS_MANUAL).map(([v, r]) => `<option value="${v}" ${estado.status === v ? "selected" : ""}>${r}</option>`).join("")}
      </select>
    </div>
    <div id="tabela-manuais"></div>`);

  document.getElementById("btn-enviar-pdf").addEventListener("click", () => abrirEnvioManuais(estado));
  document.getElementById("btn-importar-sp").addEventListener("click", () => abrirImportacaoSharePoint(estado));
  document.getElementById("btn-detectar-pecas").addEventListener("click", (e) => {
    const btn = e.currentTarget;
    btn.disabled = true;
    api.cmsDetectarPecas().then(r => {
      toast(`${r.vinculos} ligação(ões) peça ↔ manual encontradas em ${r.manuais} manual(is).`, "ok");
      return api.cmsListarManuais().then(m => { estado.manuais = m; pintarTabelaManuais(estado); });
    }).catch(err => toast(err.message, "erro")).finally(() => { btn.disabled = false; });
  });
  document.getElementById("busca-manuais-admin").addEventListener("input", (e) => { estado.termo = e.target.value; pintarTabelaManuais(estado); });
  document.getElementById("filtro-status-manual").addEventListener("change", (e) => { estado.status = e.target.value; pintarTabelaManuais(estado); });
  document.getElementById("tabela-manuais").addEventListener("click", (e) => acaoManual(e, estado));
  document.getElementById("tabela-manuais").addEventListener("change", (e) => {
    const chk = e.target.closest("[data-ativo]");
    if(!chk) return;
    chk.disabled = true;
    api.cmsAlterarAtivoManual(chk.getAttribute("data-ativo"), chk.checked).then(m => {
      substituirManual(estado, m);
      toast(m.ativo ? "Manual ativado: volta a aparecer na busca e no chat." : "Manual desativado: saiu da busca e do chat.", "ok");
    }).catch(err => { chk.checked = !chk.checked; toast(err.message, "erro"); })
      .finally(() => pintarTabelaManuais(estado));
  });
  pintarTabelaManuais(estado);
}

function pintarTabelaManuais(estado){
  const cont = document.getElementById("tabela-manuais");
  if(!cont) return;
  const t = estado.termo.trim().toLowerCase();
  const lista = estado.manuais.filter(m =>
    (!estado.status || m.status === estado.status) &&
    (!t || [m.titulo, m.nomeArquivo, m.categoriaNome, ...(m.pecas || []).map(p => p.partNumber)].filter(Boolean).join(" ").toLowerCase().includes(t)));

  if(!estado.manuais.length){
    cont.innerHTML = estadoVazio({ titulo: "Nenhum manual ainda", texto: "Envie o primeiro PDF para a IA começar a responder com base nele.",
      acaoHtml: `<button class="btn-principal" data-acao-vazio="enviar">${ICONES.mais} Enviar PDF</button>` });
    cont.querySelector("[data-acao-vazio]").addEventListener("click", () => abrirEnvioManuais(estado));
    return;
  }
  if(!lista.length){
    cont.innerHTML = estadoVazio({ titulo: "Nenhum manual com esse filtro", texto: "Ajuste a busca ou o status." });
    return;
  }
  cont.innerHTML = `
    <div class="tabela-wrap"><table class="tabela">
      <thead><tr><th>Manual</th><th>Peças</th><th>Categoria</th><th>Páginas</th><th>Trechos</th><th>Status</th><th>Ativo</th><th class="col-acoes">Ações</th></tr></thead>
      <tbody>${lista.map(linhaManualAdmin).join("")}</tbody>
    </table></div>`;
  agendarAtualizacaoManuais(estado);
}

function linhaManualAdmin(m){
  const processando = m.status === "PROCESSANDO";
  return `<tr>
    <td data-rot="Manual"><strong>${esc(m.titulo)}</strong><br><small class="texto-suave">${m.origem === "sharepoint"
        ? `<a class="selo-origem" href="${esc(m.sharepointUrl || "#")}" target="_blank" rel="noopener" title="Abrir no SharePoint">SharePoint</a> ${m.sharepointCaminho ? esc(m.sharepointCaminho) + "/" : ""}`
        : ""}${esc(m.nomeArquivo)} · ${formatarTamanho(m.tamanhoBytes)}</small>
      ${m.status === "ERRO" && m.mensagemErro ? `<div class="erro-doc">${esc(m.mensagemErro)}</div>` : ""}</td>
    <td data-rot="Peças">${(m.pecas || []).length
      ? `<span class="chips-pn">${m.pecas.map(p => `<span class="chip-pn ${p.origem === "manual" ? "chip-manual" : ""}" title="${esc(p.nome)} — ${p.origem === "manual" ? "ligada pelo admin" : `achada no texto (${p.ocorrencias}x)`}">${esc(p.partNumber)}</span>`).join("")}</span>`
      : `<span class="texto-suave">nenhuma</span>`}</td>
    <td data-rot="Categoria">${m.categoriaNome ? esc(m.categoriaNome) : `<span class="texto-suave">Sem categoria</span>`}</td>
    <td data-rot="Páginas">${m.totalPaginas ?? "—"}</td>
    <td data-rot="Trechos">${m.status === "PRONTO" ? m.totalChunks : "—"}</td>
    <td data-rot="Status"><span class="badge ${CLASSE_STATUS_MANUAL[m.status]}">${processando ? `<span class="spinner-mini"></span>` : ""}${esc(ROTULO_STATUS_MANUAL[m.status])}</span></td>
    <td data-rot="Ativo"><label class="interruptor" title="${m.ativo ? "Aparece na busca e no chat" : "Fora da busca e do chat"}"><input type="checkbox" data-ativo="${m.id}" ${m.ativo ? "checked" : ""}><i></i></label></td>
    <td data-rot="Ações" class="col-acoes">
      <button class="btn-fantasma" data-pdf="${m.id}" title="Abrir PDF">${ICONES.documento}</button>
      <button class="btn-fantasma" data-trechos="${m.id}" title="Ver trechos indexados" ${m.status !== "PRONTO" ? "disabled" : ""}>${ICONES.olho}</button>
      <button class="btn-fantasma" data-editar-manual="${m.id}" title="Editar título e categoria">${ICONES.lapis}</button>
      <button class="btn-fantasma" data-reprocessar="${m.id}" title="Reprocessar" ${processando ? "disabled" : ""}>${ICONES.atualizar}</button>
      <button class="btn-fantasma" data-excluir-manual="${m.id}" title="Excluir">${ICONES.lixeira}</button>
    </td>
  </tr>`;
}

function formatarTamanho(bytes){
  if(bytes < 1024 * 1024) return Math.max(1, Math.round(bytes / 1024)) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1).replace(".", ",") + " MB";
}

function substituirManual(estado, m){
  const i = estado.manuais.findIndex(x => x.id === m.id);
  if(i >= 0) estado.manuais[i] = m; else estado.manuais.unshift(m);
}

/* Enquanto houver manual em PROCESSANDO, recarrega a lista a cada 3 s — para quando sai da tela. */
function agendarAtualizacaoManuais(estado){
  clearTimeout(timerManuais);
  if(!estado.manuais.some(m => m.status === "PROCESSANDO")) return;
  timerManuais = setTimeout(() => {
    if(!window.location.hash.startsWith("#/admin/manuais")) return;
    api.cmsListarManuais().then(manuais => {
      const antes = new Map(estado.manuais.map(m => [m.id, m.status]));
      estado.manuais = manuais;
      manuais.forEach(m => {
        if(antes.get(m.id) === "PROCESSANDO" && m.status === "PRONTO") toast(`"${m.titulo}" pronto: ${m.totalChunks} trechos indexados.`, "ok");
        if(antes.get(m.id) === "PROCESSANDO" && m.status === "ERRO") toast(`"${m.titulo}" falhou: ${m.mensagemErro || "erro no processamento"}`, "erro");
      });
      pintarTabelaManuais(estado);
    }).catch(() => agendarAtualizacaoManuais(estado));
  }, 3000);
}

function acaoManual(e, estado){
  const alvo = e.target.closest("button");
  if(!alvo || alvo.disabled) return;
  const id = alvo.getAttribute("data-pdf") || alvo.getAttribute("data-trechos") || alvo.getAttribute("data-editar-manual")
    || alvo.getAttribute("data-reprocessar") || alvo.getAttribute("data-excluir-manual");
  if(!id) return;
  const m = estado.manuais.find(x => x.id === id);

  if(alvo.hasAttribute("data-pdf")){
    api.cmsAbrirPdfManual(id).catch(err => toast(err.message, "erro"));
  }else if(alvo.hasAttribute("data-trechos")){
    const modal = abrirModalVisualizar(`Trechos — ${m.titulo}`, estadoCarregando("Carregando trechos…"));
    api.cmsChunksManual(id).then(chunks => {
      const corpo = modal.querySelector(".modal-corpo");
      corpo.classList.add("modal-rolavel");
      corpo.innerHTML = `<p style="margin-bottom:10px;">${chunks.length} trechos, cada um com um vetor de ${chunks[0] ? chunks[0].dimensoesEmbedding : 0} posições. É isto que a busca compara com a pergunta.</p>`
        + chunks.map(c => `<div class="chunk-item"><strong>Trecho ${c.ordem + 1}${c.pagina ? ` · página ${c.pagina}` : ""} · ${c.conteudo.length} caracteres</strong><p>${esc(c.conteudo)}</p></div>`).join("");
    }).catch(err => toast(err.message, "erro"));
  }else if(alvo.hasAttribute("data-editar-manual")){
    abrirEdicaoManual(m, estado);
  }else if(alvo.hasAttribute("data-reprocessar")){
    abrirModalConfirmacao({
      titulo: "Reprocessar manual",
      corpo: `Extrair o texto e gerar os embeddings de "${m.titulo}" de novo? Os trechos atuais só são trocados se o novo processamento der certo.`,
      textoConfirmar: "Reprocessar",
      aoConfirmar: () => api.cmsReprocessarManual(id).then(atual => {
        substituirManual(estado, atual); pintarTabelaManuais(estado); toast("Reprocessamento iniciado.", "ok");
      }).catch(err => toast(err.message, "erro"))
    });
  }else if(alvo.hasAttribute("data-excluir-manual")){
    abrirModalConfirmacao({
      titulo: "Excluir manual",
      corpo: `Excluir "${m.titulo}"? O PDF e todos os trechos indexados serão apagados, e a IA deixa de usar este manual. Não dá para desfazer.`,
      textoConfirmar: "Excluir", perigo: true,
      aoConfirmar: () => api.cmsExcluirManual(id).then(() => {
        estado.manuais = estado.manuais.filter(x => x.id !== id); pintarTabelaManuais(estado); toast("Manual excluído.", "ok");
      }).catch(err => toast(err.message, "erro"))
    });
  }
}

function opcoesCategoria(categorias, selecionada){
  return `<option value="">Sem categoria</option>` + categorias.map(c =>
    `<option value="${esc(c.id)}" ${c.id === selecionada ? "selected" : ""}>${esc(c.nome)}${c.ativo ? "" : " (inativa)"}</option>`).join("");
}

function abrirModalFormulario(titulo, corpoHtml, textoSalvar){
  const fundo = document.createElement("div");
  fundo.className = "modal-fundo";
  fundo.innerHTML = `
    <div class="modal modal-larga" role="dialog" aria-modal="true">
      <div class="modal-cab"><h3>${esc(titulo)}</h3><button class="btn-fantasma" data-fechar aria-label="Fechar">✕</button></div>
      <form class="form-cms" novalidate>
        ${corpoHtml}
        <div class="modal-acoes">
          <button type="button" class="btn-fantasma" data-fechar>Cancelar</button>
          <button type="submit" class="btn-principal">${esc(textoSalvar)}</button>
        </div>
      </form>
    </div>`;
  document.body.appendChild(fundo);
  const fechar = () => fundo.remove();
  fundo.addEventListener("click", (e) => { if(e.target === fundo) fechar(); });
  fundo.querySelectorAll("[data-fechar]").forEach(b => b.addEventListener("click", fechar));
  const primeiro = fundo.querySelector("input, select, textarea");
  if(primeiro) primeiro.focus();
  return { fundo, form: fundo.querySelector("form"), fechar };
}

function abrirEdicaoManual(m, estado){
  const ligadas = new Set((m.pecas || []).map(p => p.id));
  const { fundo, form, fechar } = abrirModalFormulario("Editar manual", `
    <div class="campo" id="campo-em-titulo"><label for="em-titulo">Título</label><input id="em-titulo" type="text" maxlength="200" value="${esc(m.titulo)}"></div>
    <div class="campo"><label for="em-categoria">Categoria</label><select id="em-categoria">${opcoesCategoria(estado.categorias, m.categoriaId)}</select></div>
    <div class="campo">
      <label for="em-busca-peca">Peças deste manual (Part Number)</label>
      <small class="texto-suave">As marcadas como "achada no texto" foram ligadas sozinhas. Desmarcar uma tira a ligação — e ela não volta ao reprocessar.</small>
      <input id="em-busca-peca" type="text" placeholder="Filtrar por Part Number ou nome…" style="margin-top:8px;">
      <div class="lista-pecas-manual" id="em-pecas">${estadoCarregando("Carregando peças…")}</div>
    </div>
    <p class="texto-suave">Mudar o título não reprocessa o PDF; o título novo já aparece nas fontes do chat.</p>`, "Salvar");

  api.listarTodasPecas().then(pecas => {
    const auto = new Map((m.pecas || []).filter(p => p.origem === "automatico").map(p => [p.id, p.ocorrencias]));
    fundo.querySelector("#em-pecas").innerHTML = pecas.length ? pecas.map(p => `
      <label class="item-peca-manual" data-busca="${esc((p.partNumber + " " + p.nome).toLowerCase())}">
        <input type="checkbox" value="${esc(p.id)}" ${ligadas.has(p.id) ? "checked" : ""}>
        <span class="chip-pn">${esc(p.partNumber)}</span> <span>${esc(p.nome)}</span>
        ${auto.has(p.id) ? `<small class="texto-suave">achada no texto (${auto.get(p.id)}x)</small>` : ""}
      </label>`).join("") : `<p class="texto-suave">Nenhuma peça no catálogo.</p>`;
  }).catch(err => { fundo.querySelector("#em-pecas").innerHTML = `<p class="erro-doc">${esc(err.message)}</p>`; });
  fundo.querySelector("#em-busca-peca").addEventListener("input", (e) => {
    const t = e.target.value.trim().toLowerCase();
    fundo.querySelectorAll(".item-peca-manual").forEach(l => { l.style.display = !t || l.dataset.busca.includes(t) ? "" : "none"; });
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const titulo = fundo.querySelector("#em-titulo").value.trim();
    fundo.querySelector("#campo-em-titulo").classList.toggle("invalido", !titulo);
    if(!titulo){ toast("Informe o título.", "erro"); return; }
    const caixas = fundo.querySelectorAll("#em-pecas input[type=checkbox]");
    const pecaIds = Array.from(caixas).filter(c => c.checked).map(c => c.value);
    const mudouPecas = caixas.length && (pecaIds.length !== ligadas.size || pecaIds.some(id => !ligadas.has(id)));
    api.cmsAtualizarManual(m.id, titulo, fundo.querySelector("#em-categoria").value)
      .then(atual => mudouPecas ? api.cmsDefinirPecasManual(m.id, pecaIds) : atual)
      .then(atual => { substituirManual(estado, atual); pintarTabelaManuais(estado); fechar(); toast("Manual atualizado.", "ok"); })
      .catch(err => toast(err.message, "erro"));
  });
}

/* Envio de um ou vários PDFs, um de cada vez, mostrando o resultado de cada arquivo. */
function abrirEnvioManuais(estado){
  const { fundo, form, fechar } = abrirModalFormulario("Enviar manuais em PDF", `
    <div class="campo" id="campo-env-arquivo">
      <label for="env-arquivo">Arquivos PDF</label>
      <input id="env-arquivo" type="file" accept="application/pdf,.pdf" multiple>
      <small class="texto-suave">Use PDFs com texto selecionável — PDF escaneado (só imagem) não tem texto para a IA ler. Até 25 MB cada.</small>
    </div>
    <div class="campo" id="campo-env-titulo"><label for="env-titulo">Título <span class="opcional">(opcional — padrão: nome do arquivo)</span></label><input id="env-titulo" type="text" maxlength="200"></div>
    <div class="campo"><label for="env-categoria">Categoria</label><select id="env-categoria">${opcoesCategoria(estado.categorias.filter(c => c.ativo), null)}</select></div>
    <ul class="lista-upload" id="lista-upload"></ul>`, "Enviar");
  const inputArquivo = fundo.querySelector("#env-arquivo");
  const btnEnviar = fundo.querySelector('button[type="submit"]');

  inputArquivo.addEventListener("change", () => {
    const n = inputArquivo.files.length;
    fundo.querySelector("#campo-env-titulo").style.display = n > 1 ? "none" : "";
    fundo.querySelector("#lista-upload").innerHTML = Array.from(inputArquivo.files).map((f, i) =>
      `<li id="up-${i}"><span>${esc(f.name)}</span><small>${formatarTamanho(f.size)} · aguardando</small></li>`).join("");
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const arquivos = Array.from(inputArquivo.files);
    fundo.querySelector("#campo-env-arquivo").classList.toggle("invalido", !arquivos.length);
    if(!arquivos.length){ toast("Escolha pelo menos um PDF.", "erro"); return; }
    btnEnviar.disabled = true; inputArquivo.disabled = true;
    const titulo = arquivos.length === 1 ? fundo.querySelector("#env-titulo").value.trim() : "";
    const categoriaId = fundo.querySelector("#env-categoria").value;
    let enviados = 0;
    for(let i = 0; i < arquivos.length; i++){
      const li = fundo.querySelector(`#up-${i}`);
      li.querySelector("small").textContent = "enviando…";
      try{
        const m = await api.cmsEnviarManual(arquivos[i], titulo, categoriaId);
        substituirManual(estado, m);
        enviados++;
        li.classList.add("ok");
        li.querySelector("small").textContent = "enviado — processando em segundo plano";
      }catch(err){
        li.classList.add("erro");
        li.querySelector("small").textContent = err.message;
      }
    }
    pintarTabelaManuais(estado);
    btnEnviar.textContent = "Concluído";
    if(enviados) toast(`${enviados} manua${enviados > 1 ? "is enviados" : "l enviado"}. A lista atualiza sozinha quando o processamento terminar.`, "ok");
    if(enviados === arquivos.length) setTimeout(fechar, 900);
  });
}

/* ---------------- Importar do SharePoint (real: /api/admin/sharepoint) ---------------- */
const ROTULO_SITUACAO_SP = { NOVO: "Novo", IMPORTADO: "Já importado", ALTERADO: "Alterado no SharePoint", GRANDE_DEMAIS: "Grande demais" };
const CLASSE_SITUACAO_SP = { NOVO: "b-info", IMPORTADO: "b-ok", ALTERADO: "b-alerta", GRANDE_DEMAIS: "b-erro" };
const ROTULO_RESULTADO_SP = { IMPORTADO: "importado", ATUALIZADO: "atualizado", VINCULADO: "ligado ao existente", SEM_ALTERACAO: "sem alteração", IGNORADO: "ignorado", ERRO: "erro" };

function abrirImportacaoSharePoint(estado){
  const fundo = document.createElement("div");
  fundo.className = "modal-fundo";
  fundo.innerHTML = `
    <div class="modal modal-larga modal-sp" role="dialog" aria-modal="true">
      <div class="modal-cab"><h3>Importar do SharePoint</h3><button class="btn-fantasma" data-fechar aria-label="Fechar">✕</button></div>
      <div class="modal-corpo" id="sp-corpo">${estadoCarregando("Verificando a conexão com o SharePoint…")}</div>
    </div>`;
  document.body.appendChild(fundo);
  const fechar = () => fundo.remove();
  fundo.addEventListener("click", (e) => { if(e.target === fundo) fechar(); });
  fundo.querySelector("[data-fechar]").addEventListener("click", fechar);
  const corpo = fundo.querySelector("#sp-corpo");

  api.cmsSharePointStatus().then(({ status, categoriaPelaPastaPadrao }) => {
    if(!status.configurado){
      corpo.innerHTML = `
        <div class="aviso aviso-cuidado">
          <h4>${ICONES.alerta} SharePoint ainda não configurado</h4>
          <p>Falta no <code>.env</code> do backend: <strong>${esc(status.faltando)}</strong>.</p>
          <p style="margin-top:6px;">O passo a passo para registrar o app no Entra ID está no <code>backend/README.md</code>, seção "Conectar ao SharePoint". Enquanto isso, dá para usar o "Enviar PDF".</p>
        </div>`;
      return;
    }
    corpo.innerHTML = `
      <dl class="chave-valor sp-origem">
        <dt>Site</dt><dd><a class="link-simples" href="${esc(status.siteUrl)}" target="_blank" rel="noopener">${esc(status.siteUrl)}</a></dd>
        <dt>Pasta</dt><dd>${status.pasta ? esc(status.pasta) : "raiz da biblioteca Documentos"} <span class="texto-suave">(e subpastas)</span></dd>
        <dt>Automático</dt><dd>${status.sincronizacaoMinutos ? `a cada ${status.sincronizacaoMinutos} min` : "desligado — só por este botão"}${status.ultimaSincronizacao
          ? ` · última: ${esc(new Date(status.ultimaSincronizacao).toLocaleString("pt-BR"))} — ${esc(status.resumoUltimaSincronizacao || "")}` : ""}</dd>
      </dl>
      <div id="sp-lista">${estadoCarregando("Lendo os PDFs da pasta…")}</div>`;
    carregarListaSharePoint(fundo, estado, categoriaPelaPastaPadrao, fechar);
  }).catch(err => { corpo.innerHTML = estadoErro({ titulo: "Não foi possível verificar o SharePoint", texto: err.message }); });
}

function carregarListaSharePoint(fundo, estado, categoriaPelaPastaPadrao, fechar){
  const cont = fundo.querySelector("#sp-lista");
  api.cmsSharePointArquivos().then(arquivos => {
    if(!arquivos.length){
      cont.innerHTML = estadoVazio({ titulo: "Nenhum PDF na pasta", texto: "Coloque os manuais em PDF na pasta configurada do SharePoint e clique em atualizar.",
        acaoHtml: `<button class="btn-secundario" data-sp-recarregar>${ICONES.atualizar} Atualizar</button>` });
      cont.querySelector("[data-sp-recarregar]").addEventListener("click", () => { cont.innerHTML = estadoCarregando("Lendo os PDFs da pasta…"); carregarListaSharePoint(fundo, estado, categoriaPelaPastaPadrao, fechar); });
      return;
    }
    const pendentes = arquivos.filter(a => a.situacao === "NOVO" || a.situacao === "ALTERADO").length;
    cont.innerHTML = `
      <p class="sp-resumo">${arquivos.length} PDF(s) na pasta · <strong>${pendentes}</strong> novo(s) ou alterado(s) para importar.</p>
      <div class="tabela-wrap sp-tabela"><table class="tabela">
        <thead><tr><th><input type="checkbox" id="sp-todos" aria-label="Marcar todos"></th><th>Arquivo</th><th>Modificado</th><th>Situação</th></tr></thead>
        <tbody>${arquivos.map(a => `<tr>
          <td><input type="checkbox" data-sp-item="${esc(a.itemId)}" ${a.situacao === "NOVO" || a.situacao === "ALTERADO" ? "checked" : ""} ${a.situacao === "GRANDE_DEMAIS" ? "disabled" : ""}></td>
          <td data-rot="Arquivo"><strong>${esc(a.nome)}</strong><br><small class="texto-suave">${a.caminho ? esc(a.caminho) + " · " : ""}${formatarTamanho(a.tamanhoBytes)}${a.webUrl ? ` · <a href="${esc(a.webUrl)}" target="_blank" rel="noopener">abrir</a>` : ""}</small></td>
          <td data-rot="Modificado">${a.modificadoEm ? esc(new Date(a.modificadoEm).toLocaleDateString("pt-BR")) : "—"}</td>
          <td data-rot="Situação"><span class="badge ${CLASSE_SITUACAO_SP[a.situacao]}">${esc(ROTULO_SITUACAO_SP[a.situacao])}</span>${a.manualTitulo ? `<br><small class="texto-suave">${esc(a.manualTitulo)}</small>` : ""}</td>
        </tr>`).join("")}</tbody>
      </table></div>
      <div class="linha-campos sp-opcoes">
        <label class="interruptor"><input type="checkbox" id="sp-pela-pasta" ${categoriaPelaPastaPadrao ? "checked" : ""}><i></i> Usar a subpasta como categoria (cria se não existir)</label>
        <div class="campo"><label for="sp-categoria">Categoria dos arquivos fora de subpasta</label><select id="sp-categoria">${opcoesCategoria(estado.categorias.filter(c => c.ativo), null)}</select></div>
      </div>
      <ul class="lista-upload" id="sp-resultado"></ul>
      <div class="modal-acoes">
        <button type="button" class="btn-fantasma" data-sp-recarregar>${ICONES.atualizar} Atualizar lista</button>
        <button type="button" class="btn-principal" id="sp-importar"></button>
      </div>`;

    const marcados = () => Array.from(cont.querySelectorAll("[data-sp-item]:checked")).map(c => c.getAttribute("data-sp-item"));
    const btn = cont.querySelector("#sp-importar");
    const atualizarBotao = () => { const n = marcados().length; btn.textContent = n ? `Importar ${n} arquivo(s)` : "Nada selecionado"; btn.disabled = !n; };
    atualizarBotao();
    cont.addEventListener("change", (e) => {
      if(e.target.id === "sp-todos") cont.querySelectorAll("[data-sp-item]:not(:disabled)").forEach(c => { c.checked = e.target.checked; });
      atualizarBotao();
    });
    cont.querySelector("[data-sp-recarregar]").addEventListener("click", () => { cont.innerHTML = estadoCarregando("Lendo os PDFs da pasta…"); carregarListaSharePoint(fundo, estado, categoriaPelaPastaPadrao, fechar); });

    btn.addEventListener("click", () => {
      const itens = marcados();
      btn.disabled = true; btn.innerHTML = `<span class="spinner-mini"></span> Baixando e importando…`;
      api.cmsSharePointImportar(itens, cont.querySelector("#sp-categoria").value, cont.querySelector("#sp-pela-pasta").checked).then(resultados => {
        cont.querySelector("#sp-resultado").innerHTML = resultados.map(r => `
          <li class="${r.resultado === "ERRO" ? "erro" : ["IMPORTADO", "ATUALIZADO", "VINCULADO"].includes(r.resultado) ? "ok" : ""}">
            <span>${esc(r.nome || r.itemId)}</span><small>${esc(ROTULO_RESULTADO_SP[r.resultado])} — ${esc(r.mensagem || "")}</small></li>`).join("");
        const ok = resultados.filter(r => r.resultado === "IMPORTADO" || r.resultado === "ATUALIZADO").length;
        btn.textContent = "Concluído";
        if(ok) toast(`${ok} manual(is) do SharePoint em processamento. A lista atualiza sozinha.`, "ok");
        return Promise.all([api.cmsListarManuais(), api.cmsListarCategorias()]).then(([manuais, categorias]) => {
          estado.manuais = manuais; estado.categorias = categorias; pintarTabelaManuais(estado);
        });
      }).catch(err => { btn.disabled = false; atualizarBotao(); toast(err.message, "erro"); });
    });
  }).catch(err => {
    cont.innerHTML = estadoErro({ titulo: "Não foi possível ler a pasta do SharePoint", texto: err.message });
  });
}

/* ---------------- Categorias (real: /api/admin/categorias) ---------------- */
rota("/admin/categorias", () => {
  montarAdmin("categorias", estadoCarregando("Carregando categorias…"));
  api.cmsListarCategorias().then(telaCategoriasAdmin)
    .catch(err => montarAdmin("categorias", estadoErro({ titulo: "Não foi possível carregar as categorias", texto: err.message })));
}, ["ADMIN"]);

function telaCategoriasAdmin(categorias){
  montarAdmin("categorias", `
    ${cabAdmin("Categorias", "Agrupam os manuais na busca. Desativar uma categoria tira todos os manuais dela da busca e do chat.",
      `<button class="btn-principal" id="btn-nova-categoria">${ICONES.mais} Nova categoria</button>`)}
    ${categorias.length ? `
    <div class="tabela-wrap"><table class="tabela">
      <thead><tr><th>Nome</th><th>Descrição</th><th>Manuais</th><th>Ativa</th><th class="col-acoes">Ações</th></tr></thead>
      <tbody>${categorias.map(c => `<tr>
        <td data-rot="Nome"><strong>${esc(c.nome)}</strong></td>
        <td data-rot="Descrição">${c.descricao ? esc(c.descricao) : `<span class="texto-suave">—</span>`}</td>
        <td data-rot="Manuais">${c.totalManuais}</td>
        <td data-rot="Ativa"><label class="interruptor"><input type="checkbox" data-ativa-cat="${c.id}" ${c.ativo ? "checked" : ""}><i></i></label></td>
        <td data-rot="Ações" class="col-acoes">
          <button class="btn-fantasma" data-editar-cat="${c.id}" title="Editar">${ICONES.lapis}</button>
          <button class="btn-fantasma" data-excluir-cat="${c.id}" title="Excluir">${ICONES.lixeira}</button>
        </td>
      </tr>`).join("")}</tbody>
    </table></div>` : estadoVazio({ titulo: "Nenhuma categoria", texto: "Crie categorias para organizar os manuais (ex.: Rolamento de Cubo, Transmissão)." })}`);

  const recarregar = () => api.cmsListarCategorias().then(telaCategoriasAdmin).catch(err => toast(err.message, "erro"));
  document.getElementById("btn-nova-categoria").addEventListener("click", () => abrirFormularioCategoria(null, recarregar));
  document.querySelectorAll("[data-editar-cat]").forEach(b => b.addEventListener("click", () =>
    abrirFormularioCategoria(categorias.find(c => c.id === b.getAttribute("data-editar-cat")), recarregar)));
  document.querySelectorAll("[data-ativa-cat]").forEach(chk => chk.addEventListener("change", () => {
    const c = categorias.find(x => x.id === chk.getAttribute("data-ativa-cat"));
    chk.disabled = true;
    api.cmsSalvarCategoria(c.id, { nome: c.nome, descricao: c.descricao, ativo: chk.checked }).then(() => {
      toast(chk.checked ? `"${c.nome}" ativada.` : `"${c.nome}" desativada: os manuais dela saíram da busca e do chat.`, "ok");
      recarregar();
    }).catch(err => { chk.checked = !chk.checked; chk.disabled = false; toast(err.message, "erro"); });
  }));
  document.querySelectorAll("[data-excluir-cat]").forEach(b => b.addEventListener("click", () => {
    const c = categorias.find(x => x.id === b.getAttribute("data-excluir-cat"));
    abrirModalConfirmacao({
      titulo: "Excluir categoria",
      corpo: `Excluir "${c.nome}"?${c.totalManuais ? ` Os ${c.totalManuais} manual(is) dela não são apagados: ficam sem categoria.` : ""}`,
      textoConfirmar: "Excluir", perigo: true,
      aoConfirmar: () => api.cmsExcluirCategoria(c.id).then(() => { toast("Categoria excluída.", "ok"); recarregar(); })
        .catch(err => toast(err.message, "erro"))
    });
  }));
}

function abrirFormularioCategoria(c, aoSalvar){
  const { fundo, form, fechar } = abrirModalFormulario(c ? "Editar categoria" : "Nova categoria", `
    <div class="campo" id="campo-cat-nome"><label for="cat-nome">Nome</label><input id="cat-nome" type="text" maxlength="100" value="${c ? esc(c.nome) : ""}" placeholder="Ex: Rolamento de Cubo"></div>
    <div class="campo"><label for="cat-descricao">Descrição <span class="opcional">(opcional)</span></label><textarea id="cat-descricao" maxlength="500">${c && c.descricao ? esc(c.descricao) : ""}</textarea></div>
    <label class="interruptor"><input type="checkbox" id="cat-ativo" ${!c || c.ativo ? "checked" : ""}><i></i> Ativa (manuais aparecem na busca e no chat)</label>`,
    c ? "Salvar" : "Criar categoria");
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const nome = fundo.querySelector("#cat-nome").value.trim();
    fundo.querySelector("#campo-cat-nome").classList.toggle("invalido", !nome);
    if(!nome){ toast("Informe o nome da categoria.", "erro"); return; }
    api.cmsSalvarCategoria(c ? c.id : null, {
      nome, descricao: fundo.querySelector("#cat-descricao").value.trim(), ativo: fundo.querySelector("#cat-ativo").checked
    }).then(() => { fechar(); toast(c ? "Categoria atualizada." : "Categoria criada.", "ok"); aoSalvar(); })
      .catch(err => { fundo.querySelector("#campo-cat-nome").classList.add("invalido"); toast(err.message, "erro"); });
  });
}

/* ---------------- Tutoriais (passo a passo por peça) ---------------- */
rota("/admin/tutoriais", () => {
  montarAdmin("tutoriais", estadoCarregando());
  api.cmsListarPecas().then(pecas => {
    montarAdmin("tutoriais", `
      ${cabAdmin("Tutoriais", "Passo a passo de instalação, um por peça.")}
      <div class="tabela-wrap"><table class="tabela">
        <thead><tr><th>Peça</th><th>Passos</th><th class="col-acoes">Ações</th></tr></thead>
        <tbody>${pecas.map(p => `<tr>
          <td data-rot="Peça"><span class="mono">${esc(p.partNumber)}</span> ${esc(p.nome)}</td>
          <td data-rot="Passos">${p.passos.length} passo(s)</td>
          <td data-rot="Ações" class="col-acoes"><a class="btn-fantasma" href="#/peca/${p.id}/instalacao" title="Visualizar">${ICONES.olho}</a></td>
        </tr>`).join("")}</tbody>
      </table></div>`);
  });
}, ["ADMIN", "TECNICO"]);

/* ---------------- Vídeos ---------------- */
rota("/admin/videos", () => {
  montarAdmin("videos", estadoCarregando());
  api.cmsListarPecas().then(pecas => {
    const linhas = pecas.flatMap(p => p.videos.map(v => ({ ...v, pn: p.partNumber, pecaId: p.id })));
    montarAdmin("videos", `
      ${cabAdmin("Vídeos", "Vídeos curtos vinculados às peças.")}
      <div class="tabela-wrap"><table class="tabela">
        <thead><tr><th>Título</th><th>Duração</th><th>Peça</th><th class="col-acoes">Ações</th></tr></thead>
        <tbody>${linhas.map(v => `<tr>
          <td data-rot="Título">${esc(v.titulo)}</td>
          <td data-rot="Duração">${esc(v.duracao)}</td>
          <td data-rot="Peça" class="mono">${esc(v.pn)}</td>
          <td data-rot="Ações" class="col-acoes"><a class="btn-fantasma" href="#/peca/${v.pecaId}/videos" title="Visualizar">${ICONES.olho}</a></td>
        </tr>`).join("")}</tbody>
      </table></div>`);
  });
}, ["ADMIN", "TECNICO"]);

/* ---------------- Ferramentas (catálogo) ---------------- */
rota("/admin/ferramentas", () => {
  montarAdmin("ferramentas", estadoCarregando());
  api.cmsListarFerramentas().then(lista => renderListaFerramentasAdmin(lista));
}, ["ADMIN", "TECNICO"]);

function renderListaFerramentasAdmin(lista){
  montarAdmin("ferramentas", `
    ${cabAdmin("Ferramentas", "Catálogo reutilizado entre as peças.")}
    <div class="tabela-wrap"><table class="tabela">
      <thead><tr><th>Nome</th><th>Uso</th><th>Descrição</th><th class="col-acoes">Ações</th></tr></thead>
      <tbody>${lista.map(f => `<tr>
        <td data-rot="Nome">${esc(f.nome)}</td>
        <td data-rot="Uso">${esc(f.uso)}</td>
        <td data-rot="Descrição">${esc(f.descricao)}</td>
        <td data-rot="Ações" class="col-acoes"><button class="btn-fantasma" data-ver-ferramenta="${f.id}" title="Visualizar">${ICONES.olho}</button></td>
      </tr>`).join("")}</tbody>
    </table></div>`);
  document.querySelectorAll("[data-ver-ferramenta]").forEach(b => b.addEventListener("click", () => {
    const f = lista.find(x => x.id === b.getAttribute("data-ver-ferramenta"));
    abrirModalVisualizar(f.nome, `<p><strong>Uso:</strong> ${esc(f.uso)}</p><p style="margin-top:8px;"><strong>Descrição:</strong> ${esc(f.descricao)}</p><p style="margin-top:8px;"><strong>Procedimento:</strong> ${esc(f.procedimento)}</p>`);
  }));
}

/* ---------------- Problemas / Soluções (catálogo) ---------------- */
rota("/admin/problemas", () => {
  montarAdmin("problemas", estadoCarregando());
  api.cmsListarProblemas().then(lista => renderListaProblemasAdmin(lista));
}, ["ADMIN", "TECNICO"]);
rota("/admin/solucoes", () => {
  montarAdmin("solucoes", estadoCarregando());
  api.cmsListarProblemas().then(lista => renderListaProblemasAdmin(lista, true));
}, ["ADMIN", "TECNICO"]);

function renderListaProblemasAdmin(lista, focoSolucao){
  const secao = focoSolucao ? "solucoes" : "problemas";
  montarAdmin(secao, `
    ${cabAdmin(focoSolucao ? "Soluções" : "Problemas", focoSolucao ? "Solução aplicada a cada problema catalogado." : "Problemas comuns, catalogados por causa provável.")}
    <div class="tabela-wrap"><table class="tabela">
      <thead><tr><th>Problema</th><th>${focoSolucao ? "Solução" : "Causa provável"}</th><th class="col-acoes">Ações</th></tr></thead>
      <tbody>${lista.map(p => `<tr>
        <td data-rot="Problema">${esc(p.titulo)}</td>
        <td data-rot="${focoSolucao ? "Solução" : "Causa"}">${esc(focoSolucao ? p.solucao : p.causas)}</td>
        <td data-rot="Ações" class="col-acoes"><button class="btn-fantasma" data-ver-problema="${p.id}" title="Visualizar">${ICONES.olho}</button></td>
      </tr>`).join("")}</tbody>
    </table></div>`);
  document.querySelectorAll("[data-ver-problema]").forEach(b => b.addEventListener("click", () => {
    const p = lista.find(x => x.id === b.getAttribute("data-ver-problema"));
    abrirModalVisualizar(p.titulo, `
      <p><strong>Causas:</strong> ${esc(p.causas)}</p>
      <p style="margin-top:8px;"><strong>O que verificar:</strong> ${p.verificar.map(esc).join(", ")}</p>
      <p style="margin-top:8px;"><strong>Solução:</strong> ${esc(p.solucao)}</p>
      <p style="margin-top:8px;"><strong>Quando procurar suporte:</strong> ${esc(p.quandoTecnico)}</p>`);
  }));
}

function abrirModalVisualizar(titulo, corpoHtml){
  const fundo = document.createElement("div");
  fundo.className = "modal-fundo";
  fundo.innerHTML = `<div class="modal" role="dialog" aria-modal="true">
    <div class="modal-cab"><h3>${esc(titulo)}</h3><button class="btn-fantasma" data-fechar aria-label="Fechar">✕</button></div>
    <div class="modal-corpo">${corpoHtml}</div>
    <div class="modal-acoes"><button class="btn-principal" data-fechar>Fechar</button></div>
  </div>`;
  document.body.appendChild(fundo);
  const fechar = () => fundo.remove();
  fundo.addEventListener("click", (e) => { if(e.target === fundo) fechar(); });
  fundo.querySelectorAll("[data-fechar]").forEach(b => b.addEventListener("click", fechar));
  return fundo;
}

/* ---------------- Usuários (real: /api/admin/usuarios) ----------------
   Quem se cadastra entra como Mecânico; aqui o admin promove. */
const PERFIS_USUARIO = ["MECANICO", "TECNICO", "ADMIN", "CLIENTE"];

rota("/admin/usuarios", () => {
  montarAdmin("usuarios", estadoCarregando("Carregando usuários…"));
  api.cmsListarUsuarios().then(telaUsuariosAdmin)
    .catch(err => montarAdmin("usuarios", estadoErro({ titulo: "Não foi possível carregar os usuários", texto: err.message })));
}, ["ADMIN"]);

function telaUsuariosAdmin(usuarios){
  const sessao = Sessao.atual();
  const data = (v) => v ? new Date(v).toLocaleDateString("pt-BR") : "—";
  montarAdmin("usuarios", `
    ${cabAdmin("Usuários", "Contas de acesso. Quem se cadastra pela tela de login entra como Mecânico — promova aqui para Técnico ou Administrador.")}
    <div class="barra-ferramentas">
      <div class="campo-busca">${ICONES.busca}<input id="busca-usuarios" type="text" placeholder="Buscar por nome ou e-mail…"></div>
    </div>
    <div class="tabela-wrap"><table class="tabela">
      <thead><tr><th>Usuário</th><th>Perfil</th><th>Cadastro</th><th>Último acesso</th><th>E-mail</th></tr></thead>
      <tbody id="corpo-usuarios">${usuarios.map(u => `<tr data-busca="${esc((u.nome + " " + u.email).toLowerCase())}">
        <td data-rot="Usuário"><strong>${esc(u.nome)}</strong>${u.id === sessao.id ? ` <span class="texto-suave">(você)</span>` : ""}<br><small class="texto-suave">${esc(u.email || "")}</small></td>
        <td data-rot="Perfil">
          <select class="select-perfil" data-perfil-usuario="${esc(u.id)}" data-atual="${esc(u.perfil || "")}" aria-label="Perfil de ${esc(u.nome)}">
            ${u.perfil ? "" : `<option value="" selected>Sem perfil — escolha</option>`}
            ${PERFIS_USUARIO.map(p => `<option value="${p}" ${u.perfil === p ? "selected" : ""}>${esc(rotuloPerfil(p))}</option>`).join("")}
          </select>
          ${u.perfil ? "" : `<div class="erro-doc">Conta sem perfil: não consegue entrar até receber um.</div>`}
        </td>
        <td data-rot="Cadastro">${data(u.criadoEm)}</td>
        <td data-rot="Último acesso">${u.ultimoAcesso ? data(u.ultimoAcesso) : `<span class="texto-suave">nunca</span>`}</td>
        <td data-rot="E-mail">${u.emailConfirmado ? `<span class="badge b-ok">confirmado</span>` : `<span class="badge b-alerta">não confirmado</span>`}</td>
      </tr>`).join("")}</tbody>
    </table></div>
    ${usuarios.length ? "" : estadoVazio({ titulo: "Nenhum usuário", texto: "Quando alguém criar conta pela tela de login, aparece aqui." })}`);

  document.getElementById("busca-usuarios").addEventListener("input", (e) => {
    const t = e.target.value.trim().toLowerCase();
    document.querySelectorAll("#corpo-usuarios tr").forEach(tr => { tr.style.display = !t || tr.dataset.busca.includes(t) ? "" : "none"; });
  });
  document.getElementById("corpo-usuarios").addEventListener("change", (e) => {
    const sel = e.target.closest("[data-perfil-usuario]");
    if(!sel || !sel.value) return;
    const anterior = sel.dataset.atual;
    sel.disabled = true;
    api.cmsAlterarPerfilUsuario(sel.dataset.perfilUsuario, sel.value).then(u => {
      sel.dataset.atual = u.perfil;
      toast(`${u.nome} agora é ${rotuloPerfil(u.perfil)}.`, "ok");
      if(!anterior) api.cmsListarUsuarios().then(telaUsuariosAdmin);
    }).catch(err => {
      sel.value = anterior;
      toast(err.message, "erro");
    }).finally(() => { sel.disabled = false; });
  });
}

/* ---------------- Testar IA (real: chat livre x RAG) ----------------
   Para o admin experimentar a IA sem depender dos manuais/SharePoint:
   - Livre: o Gemini responde qualquer coisa, com o conhecimento dele
   - Com os manuais: o RAG de verdade (mesma chamada da área pública)
   - Comparar: os dois lado a lado — mostra por que o RAG existe. */
const EXEMPLOS_TESTE_IA = [
  "Qual o torque da porca do cubo do rolamento FRS-1001?",
  "Explique em poucas palavras o que é RAG.",
  "Qual a capital da França?",
  "Como lubrificar um rolamento de roda de caminhão?"
];

rota("/admin/teste-ia", () => {
  montarAdmin("teste-ia", `
    ${cabAdmin("Testar IA", "Experimente o Gemini sem precisar de manuais ou do SharePoint — e compare com a resposta baseada nos manuais.")}
    <div class="painel">
      <form id="form-teste-ia">
        <textarea id="pergunta-teste-ia" maxlength="2000" placeholder="Pergunte qualquer coisa…"></textarea>
        <div class="exemplos" style="margin:10px 0;">
          ${EXEMPLOS_TESTE_IA.map(ex => `<button type="button" class="exemplo" data-exemplo="${esc(ex)}">${esc(ex)}</button>`).join("")}
        </div>
        <div class="grupo-botoes">
          <button type="submit" class="btn-principal" data-modo="livre">${ICONES.ia} Livre (sem manuais)</button>
          <button type="submit" class="btn-secundario" data-modo="rag">${ICONES.manual} Com os manuais (RAG)</button>
          <button type="submit" class="btn-secundario" data-modo="comparar">Comparar lado a lado</button>
        </div>
      </form>
      <p class="texto-suave" style="margin-top:10px;">O modo livre só existe aqui no CMS: na área pública a IA responde apenas com base nos manuais.</p>
    </div>
    <div id="resultados-teste-ia"></div>`);
  ligarTesteIA();
}, ["ADMIN"]);

function ligarTesteIA(){
  const form = document.getElementById("form-teste-ia");
  const campo = document.getElementById("pergunta-teste-ia");
  const cont = document.getElementById("resultados-teste-ia");
  let modo = "livre";
  form.querySelectorAll("[data-modo]").forEach(b => b.addEventListener("click", () => { modo = b.dataset.modo; }));
  form.querySelectorAll("[data-exemplo]").forEach(b => b.addEventListener("click", () => { campo.value = b.dataset.exemplo; campo.focus(); }));

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const pergunta = campo.value.trim();
    if(!pergunta){ toast("Digite uma pergunta.", "erro"); return; }
    cont.querySelectorAll(".teste-ia-bloco.aberto").forEach(b => b.classList.remove("aberto"));
    const bloco = document.createElement("div");
    bloco.className = "painel teste-ia-bloco aberto";
    const titulos = { livre: "Livre (sem manuais)", rag: "Com os manuais (RAG)", comparar: "Comparação" };
    bloco.innerHTML = `<div class="ia-rotulo">${esc(titulos[modo])}</div><p class="teste-ia-pergunta">${esc(pergunta)}</p>
      <div class="teste-ia-colunas ${modo === "comparar" ? "duas" : ""}">
        ${modo !== "rag" ? `<div class="teste-ia-col" data-col="livre">${modo === "comparar" ? `<h4>Livre (sem manuais)</h4>` : ""}${estadoCarregando("Perguntando ao Gemini…")}</div>` : ""}
        ${modo !== "livre" ? `<div class="teste-ia-col" data-col="rag">${modo === "comparar" ? `<h4>Com os manuais (RAG)</h4>` : ""}${estadoCarregando("Buscando nos manuais…")}</div>` : ""}
      </div>
      ${modo === "comparar" ? `<p class="texto-suave teste-ia-nota">${ICONES.info} Sem RAG o modelo responde com o que "acha" que sabe e pode inventar valores técnicos. Com RAG ele só usa os trechos dos manuais e mostra de onde tirou — ou diz que não encontrou.</p>` : ""}`;
    cont.insertBefore(bloco, cont.firstChild);
    const botoes = form.querySelectorAll("button[type=submit]");
    botoes.forEach(b => { b.disabled = true; });
    const erroTexto = (err) => err.status === 503 ? "A IA está indisponível agora. Tente de novo em instantes." : err.message;
    const titulo = (col) => modo === "comparar" ? `<h4>${col === "livre" ? "Livre (sem manuais)" : "Com os manuais (RAG)"}</h4>` : "";

    const tarefas = [];
    const colLivre = bloco.querySelector('[data-col="livre"]');
    if(colLivre){
      tarefas.push(api.cmsPerguntarLivre(pergunta).then(r => {
        colLivre.innerHTML = `${titulo("livre")}<div class="ia-resposta">${formatarRespostaIA(r.resposta)}</div>
          <p class="ia-modelo">Gerado por ${esc(r.modelo)} · sem fontes: conhecimento geral do modelo.</p>`;
      }).catch(err => { colLivre.innerHTML = titulo("livre") + estadoErro({ titulo: "Falhou", texto: erroTexto(err) }); }));
    }
    const colRag = bloco.querySelector('[data-col="rag"]');
    if(colRag){
      tarefas.push(api.perguntarIA(pergunta, null).then(res => {
        colRag.innerHTML = `${titulo("rag")}<div class="ia-resposta ${res.encontrouConteudo ? "" : "sem-info"}">${formatarRespostaIA(res.resposta)}</div>
          ${res.fontes.length ? `<h4 style="margin:10px 0 6px;">Fontes</h4>` + res.fontes.map(f => `
            <div class="chunk-item"><strong>${esc(f.documento)}${f.pagina ? ` — página ${f.pagina}` : ""}</strong><p>${esc(f.trecho)}</p>
              <a class="link-simples" href="${esc(f.pdfUrl)}" target="_blank" rel="noopener">Abrir PDF</a></div>`).join("") : ""}
          ${res.modelo ? `<p class="ia-modelo">Gerado por ${esc(res.modelo)} a partir dos manuais cadastrados.</p>` : ""}`;
      }).catch(err => { colRag.innerHTML = titulo("rag") + estadoErro({ titulo: "Falhou", texto: erroTexto(err) }); }));
    }
    Promise.allSettled(tarefas).then(() => botoes.forEach(b => { b.disabled = false; }));
  });
}

/* ---------------- Documentos: antigo atalho, hoje vai para Manuais ---------------- */
rota("/admin/documentos", () => { irPara("/admin/manuais"); }, ["ADMIN"]);

/* ---------------- Base de conhecimento (real: resumo dos manuais + teste do RAG) ---------------- */
rota("/admin/base-conhecimento", () => {
  montarAdmin("base-conhecimento", estadoCarregando());
  api.cmsListarManuais().then(manuais => renderBaseConhecimento(resumoManuais(manuais)))
    .catch(err => montarAdmin("base-conhecimento", estadoErro({ titulo: "Não foi possível carregar a base", texto: err.message })));
}, ["ADMIN"]);

function renderBaseConhecimento(r){
  montarAdmin("base-conhecimento", `
    ${cabAdmin("Base de conhecimento", "Como os manuais viram contexto para a Busca por IA (RAG), e um teste da busca de verdade.",
      `<a class="btn-secundario" href="#/admin/manuais">${ICONES.manual} Gerenciar manuais</a>`)}

    <div class="painel">
      <h3>Como um manual chega até a IA</h3>
      <div class="fluxo-pipeline">
        <div class="etapa-pipe"><strong>1. Upload</strong><span>Admin envia o PDF no CMS</span></div>
        <div class="etapa-pipe"><strong>2. Extração</strong><span>Texto página por página (PDFBox)</span></div>
        <div class="etapa-pipe"><strong>3. Trechos</strong><span>~1.200 caracteres, 200 de sobreposição</span></div>
        <div class="etapa-pipe"><strong>4. Embeddings</strong><span>Gemini, vetor de 768 posições</span></div>
        <div class="etapa-pipe"><strong>5. Base vetorial</strong><span>Postgres + pgvector (HNSW)</span></div>
        <div class="etapa-pipe"><strong>6. RAG + Gemini</strong><span>Resposta com a página da fonte</span></div>
      </div>
    </div>

    <div class="stats stats-4">
      <a class="stat destaque" href="#/admin/manuais"><div class="num">${r.disponiveis}</div><div class="rot">Manuais disponíveis para a IA</div><div class="sub">ativos e processados, de ${r.total}</div></a>
      <div class="stat"><div class="num">${r.trechos}</div><div class="rot">Trechos indexados</div></div>
      <a class="stat" href="#/admin/manuais?status=PROCESSANDO"><div class="num">${r.processando}</div><div class="rot">Processando</div></a>
      <a class="stat ${r.erro ? "stat-erro" : ""}" href="#/admin/manuais?status=ERRO"><div class="num">${r.erro}</div><div class="rot">Com erro</div></a>
    </div>

    <div class="painel" style="margin-top:20px;">
      <h3>Testar a busca</h3>
      <p style="margin-bottom:10px;">Faça uma pergunta como um mecânico faria e veja quais trechos a IA usou para responder — é a mesma chamada da tela "Perguntar à IA".</p>
      <form class="teste-rec" id="form-teste-rec">
        <input id="teste-recuperacao" type="text" maxlength="1000" placeholder="Ex: qual o torque da porca do cubo?">
        <button class="btn-secundario" type="submit" id="btn-testar-rec">Testar</button>
      </form>
      <div id="resultado-teste-rec" style="margin-top:12px;"></div>
    </div>
  `);

  document.getElementById("form-teste-rec").addEventListener("submit", (e) => {
    e.preventDefault();
    const termo = document.getElementById("teste-recuperacao").value.trim();
    const cont = document.getElementById("resultado-teste-rec");
    const btn = document.getElementById("btn-testar-rec");
    if(!termo){ cont.innerHTML = ""; return; }
    cont.innerHTML = estadoCarregando("Buscando trechos e gerando a resposta…");
    btn.disabled = true;
    api.perguntarIA(termo, null).then(res => {
      if(!res.encontrouConteudo){
        cont.innerHTML = `<div class="aviso aviso-cuidado"><h4>${ICONES.alerta} Sem informação suficiente nos manuais</h4><p>${esc(res.resposta)}</p><p style="margin-top:6px;">Sinal de conteúdo que falta: talvez valha enviar um manual que cubra este assunto.</p></div>`;
        return;
      }
      cont.innerHTML = `<div class="ia-resposta">${formatarRespostaIA(res.resposta)}</div>
        <h4 style="margin:12px 0 8px;">Trechos usados</h4>`
        + res.fontes.map(f => `<div class="chunk-item"><strong>[${f.numero}] ${esc(f.documento)}${f.pagina ? ` — página ${f.pagina}` : ""} · similaridade ${String(f.score).replace(".", ",")}</strong><p>${esc(f.trecho)}</p></div>`).join("")
        + (res.modelo ? `<p class="ia-modelo">Gerado por ${esc(res.modelo)}.</p>` : "");
    }).catch(err => {
      cont.innerHTML = estadoErro({ titulo: "Não foi possível testar", texto: err.status === 503 ? "A IA está indisponível agora. Tente de novo em instantes." : err.message });
    }).finally(() => { btn.disabled = false; });
  });
}
