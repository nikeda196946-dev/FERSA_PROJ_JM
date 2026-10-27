/* =====================================================================
   FERSA Care — ficha da peça e sub-telas
   ===================================================================== */

const ABAS_PECA = [
  { id: "", label: "Visão geral" },
  { id: "instalacao", label: "Instalação" },
  { id: "lubrificacao", label: "Lubrificação" },
  { id: "ferramentas", label: "Ferramentas" },
  { id: "videos", label: "Vídeos" },
  { id: "problemas", label: "Problemas" },
  { id: "manual", label: "Manual" }
];

function miniCabecalho(peca, abaAtiva, vindoDeFluxo){
  return `
    <a class="voltar" href="#/busca/${vindoDeFluxo || "geral"}">${ICONES.setaEsq} Voltar para a busca</a>
    <div class="mini-cab">
      <a class="pn" href="#/peca/${peca.id}">${esc(peca.partNumber)}</a>
      <span class="nome">${esc(peca.nome)}</span>
    </div>
    <div class="abas-peca" role="tablist">
      ${ABAS_PECA.map(a => `<a href="#/peca/${peca.id}${a.id ? "/" + a.id : ""}" class="${abaAtiva === a.id ? "ativo" : ""}">${esc(a.label)}</a>`).join("")}
    </div>`;
}

function carregarPeca(id, aoCarregar){
  montarConteudo(estadoCarregando("Carregando peça…"));
  api.buscarPecaPorId(id).then(peca => {
    api.registrarConsultaRecente(peca);
    aoCarregar(peca);
  }).catch(err => {
    montarConteudo(`<div class="conteudo">${estadoErro({ titulo: "Peça não encontrada", texto: err.message, acaoHtml: `<a class="btn-principal" href="#/busca/geral">Voltar à busca</a>` })}</div>`);
  });
}

/* ---------------- Visão geral (com destaque opcional vindo da Home) ---------------- */
/* Registradas por ÚLTIMO neste arquivo (função renderVisaoGeral abaixo): o roteador usa a
   primeira rota que casar, e ":fluxo" aqui casaria com "/instalacao", "/manual" etc. antes
   das rotas específicas dessas telas. Ver o final do arquivo. */

function renderVisaoGeral(id, fluxo){
  carregarPeca(id, (peca) => {
    const destaques = { instalacao: "instalacao", lubrificacao: "lubrificacao", ferramentas: "ferramentas", problemas: "problemas", manual: "manual" };
    const destacar = destaques[fluxo] || null;

    const atalhos = [
      { id: "instalacao", ico: "instalar", titulo: "Instalação", sub: peca.passos.length + " passos ilustrados" },
      { id: "lubrificacao", ico: "lubrificar", titulo: "Lubrificação", sub: peca.lubrificacao.tipo },
      { id: "ferramentas", ico: "ferramentas", titulo: "Ferramentas", sub: peca.ferramentas.length + " ferramentas necessárias" },
      { id: "videos", ico: "video", titulo: "Vídeos", sub: peca.videos.length + " vídeo(s) curto(s)" },
      { id: "problemas", ico: "problema", titulo: "Problemas comuns", sub: peca.problemas.length + " problemas catalogados", alerta: true },
      { id: "manual", ico: "manual", titulo: "Manual completo", sub: "Referência técnica detalhada" }
    ];

    montarConteudo(`
      <div class="conteudo">
        <a class="voltar" href="#/busca/${fluxo || "geral"}">${ICONES.setaEsq} Voltar para a busca</a>

        <div class="cabecalho-peca">
          <div>
            <div class="pn-grande">${esc(peca.partNumber)}</div>
            <div class="nome-peca">${esc(peca.nome)}</div>
            <div class="categoria-peca">${esc(peca.categoria)}</div>
          </div>
          <div style="min-width:160px;">
            <span class="tag">${esc(peca.imagem || "Sem imagem cadastrada")}</span>
          </div>
        </div>

        <table class="tabela-aplicacoes">
          <thead><tr><th>Veículo</th><th>Modelo</th></tr></thead>
          <tbody>${peca.aplicacoes.map(a => `<tr><td>${esc(a.veiculo)}</td><td>${esc(a.modelo)}</td></tr>`).join("")}</tbody>
        </table>

        <h3 class="titulo-bloco">O que você precisa?</h3>
        <div class="grade-atalhos">
          ${atalhos.map(a => `
            <a class="atalho ${a.alerta ? "destaque-alerta" : ""}" href="#/peca/${peca.id}/${a.id}"${destacar === a.id ? ' style="border-color:var(--accent);border-bottom-color:var(--accent-dark);background:var(--info-bg);"' : ""}>
              ${ICONES[a.ico]}<strong>${esc(a.titulo)}</strong><small>${esc(a.sub)}</small>
            </a>`).join("")}
          <a class="atalho destaque-ia" href="#/ia?peca=${peca.id}">
            ${ICONES.ia}<strong>Perguntar à IA sobre esta peça</strong><small>Descreva a dúvida com suas palavras</small>
          </a>
        </div>

        <div class="grupo-botoes" style="margin-top:24px;">
          <a class="btn-secundario" href="#/chamado/novo?peca=${peca.id}">${ICONES.chamado} Falar com técnico sobre esta peça</a>
        </div>
      </div>`);
  });
}

/* ---------------- Instalação (passo a passo navegável) ---------------- */
rota("/peca/:id/instalacao", (params) => {
  carregarPeca(params.id, (peca) => {
    let atual = 0;
    const total = peca.passos.length;

    function render(){
      const passo = peca.passos[atual];
      const concluido = atual === total - 1 ? false : null;
      document.getElementById("conteudo-instalacao").innerHTML = `
        <div class="stepper-topo"><span>Passo ${atual + 1} de ${total}</span><div class="progresso"><i style="width:${((atual + 1) / total) * 100}%"></i></div></div>
        <div class="passo-card">
          <span class="passo-num">${String(atual + 1).padStart(2, "0")}</span>
          <div style="min-width:0;">
            <h3>${esc(peca.passos[atual].titulo)}</h3>
            <p>${esc(passo.texto)}</p>
            <div class="passo-imagem">${ICONES.imagem}<span>Imagem ilustrativa do passo ${atual + 1} (exemplo)</span></div>
          </div>
        </div>
        <div class="passo-nav">
          <button class="btn-secundario" id="btn-anterior" ${atual === 0 ? "disabled" : ""}>${ICONES.setaEsq} Passo anterior</button>
          ${atual < total - 1
            ? `<button class="btn-principal" id="btn-proximo">Próximo passo ${ICONES.seta}</button>`
            : `<button class="btn-principal" id="btn-concluir">${ICONES.check} Concluir instalação</button>`}
        </div>
        <div class="chips-passos">
          ${peca.passos.map((_, i) => `<button class="chip-passo ${i === atual ? "ativo" : i < atual ? "feito" : ""}" data-ir="${i}">${i + 1}</button>`).join("")}
        </div>
        ${atual === total - 1 ? "" : ""}
      `;
      document.getElementById("btn-anterior")?.addEventListener("click", () => { atual = Math.max(0, atual - 1); render(); });
      document.getElementById("btn-proximo")?.addEventListener("click", () => { atual = Math.min(total - 1, atual + 1); render(); });
      document.getElementById("btn-concluir")?.addEventListener("click", () => { atual = total; renderConclusao(); });
      document.querySelectorAll("[data-ir]").forEach(b => b.addEventListener("click", () => { atual = Number(b.getAttribute("data-ir")); render(); }));
    }

    function renderConclusao(){
      document.getElementById("conteudo-instalacao").innerHTML = `
        <div class="conclusao">
          ${ICONES.check}
          <h3>Instalação concluída</h3>
          <p>Você seguiu todos os ${total} passos para o ${esc(peca.partNumber)}. Não esqueça de conferir a lubrificação recomendada.</p>
          <div class="grupo-botoes">
            <a class="btn-principal" href="#/peca/${peca.id}/lubrificacao">Ver lubrificação</a>
            <button class="btn-secundario" id="btn-revisar">Revisar os passos</button>
          </div>
        </div>`;
      document.getElementById("btn-revisar").addEventListener("click", () => { atual = 0; render(); });
    }

    montarConteudo(`
      <div class="conteudo">
        ${miniCabecalho(peca, "instalacao")}
        <h2 class="titulo-secao-tela">Instalação</h2>
        <p style="margin-bottom:6px;">Siga os passos na ordem. Use "Anterior" e "Próximo" para navegar.</p>
        <div id="conteudo-instalacao"></div>

        <div class="grade-2">
          <div class="aviso aviso-cuidado">
            <h4>${ICONES.alerta} Cuidados</h4>
            <ul>${peca.cuidadosInstalacao.map(c => `<li>${esc(c)}</li>`).join("")}</ul>
          </div>
          <div class="aviso aviso-erro">
            <h4>${ICONES.alerta} Erros comuns</h4>
            <ul>${peca.errosComunsInstalacao.map(c => `<li>${esc(c)}</li>`).join("")}</ul>
          </div>
        </div>
      </div>`);
    render();
  });
}, ["MECANICO"]);

/* ---------------- Lubrificação ---------------- */
rota("/peca/:id/lubrificacao", (params) => {
  carregarPeca(params.id, (peca) => {
    const l = peca.lubrificacao;
    montarConteudo(`
      <div class="conteudo">
        ${miniCabecalho(peca, "lubrificacao")}
        <h2 class="titulo-secao-tela">Lubrificação</h2>
        <p style="margin-bottom:16px;">Recomendações específicas para o ${esc(peca.partNumber)}.</p>

        <div class="grade-3">
          <div class="info-caixa"><div class="rot">Tipo recomendado</div><strong>${esc(l.tipo)}</strong></div>
          <div class="info-caixa"><div class="rot">Quantidade</div><strong>${esc(l.quantidade)}</strong></div>
          <div class="info-caixa"><div class="rot">Intervalo</div><strong>${esc(l.intervalo)}</strong></div>
        </div>

        <div class="aviso aviso-info" style="margin-bottom:16px;">
          <h4>Procedimento</h4>
          <p>${esc(l.procedimento)}</p>
        </div>

        <div class="grade-2">
          <div class="aviso aviso-cuidado"><h4>${ICONES.alerta} Cuidados</h4><ul>${l.cuidados.map(c => `<li>${esc(c)}</li>`).join("")}</ul></div>
          <div class="aviso aviso-erro"><h4>${ICONES.alerta} Erros comuns</h4><ul>${l.errosComuns.map(c => `<li>${esc(c)}</li>`).join("")}</ul></div>
        </div>

        ${peca.videos.length ? `<h3 class="titulo-bloco">Vídeo</h3><a href="#/peca/${peca.id}/videos" class="chip-link">${ICONES.play} Ver vídeo relacionado</a>` : ""}
      </div>`);
  });
}, ["MECANICO"]);

/* ---------------- Ferramentas ---------------- */
rota("/peca/:id/ferramentas", (params) => {
  carregarPeca(params.id, (peca) => {
    const ferramentas = api.ferramentasDaPeca(peca);
    montarConteudo(`
      <div class="conteudo">
        ${miniCabecalho(peca, "ferramentas")}
        <h2 class="titulo-secao-tela">Ferramentas</h2>
        <p style="margin-bottom:16px;">Necessárias para instalar e dar manutenção no ${esc(peca.partNumber)}.</p>
        <div class="lista-ferramentas-cards">
          ${ferramentas.map(f => `
            <div class="card-ferramenta">
              ${ICONES.chave}
              <div>
                <h3>${esc(f.nome)}</h3>
                <p>${esc(f.descricao)}</p>
                <dl>
                  <dt>Uso</dt><dd>${esc(f.uso)}</dd>
                  <dt>Procedimento</dt><dd>${esc(f.procedimento)}</dd>
                </dl>
              </div>
            </div>`).join("")}
        </div>
      </div>`);
  });
}, ["MECANICO"]);

/* ---------------- Vídeos ---------------- */
rota("/peca/:id/videos", (params) => {
  carregarPeca(params.id, (peca) => {
    montarConteudo(`
      <div class="conteudo">
        ${miniCabecalho(peca, "videos")}
        <h2 class="titulo-secao-tela">Vídeos</h2>
        <p style="margin-bottom:16px;">Conteúdo em vídeo para o ${esc(peca.partNumber)}. Protótipo: sem arquivo real, apenas simulação do player.</p>
        <div class="caixa-video" id="player-video">
          <div class="play" id="btn-play">${ICONES.play}</div>
          <span id="label-video">${esc(peca.videos[0].titulo)} (${peca.videos[0].duracao})</span>
          <small>Toque para simular a reprodução</small>
          <div class="barra"><i></i></div>
        </div>
        <div class="lista-videos">
          ${peca.videos.map((v, i) => `
            <button class="item-video ${i === 0 ? "ativo" : ""}" data-idx="${i}">
              ${ICONES.video}
              <span class="t"><strong>${esc(v.titulo)}</strong><small>${esc(v.duracao)}</small></span>
            </button>`).join("")}
        </div>
      </div>`);

    const caixa = document.getElementById("player-video");
    const label = document.getElementById("label-video");
    document.getElementById("btn-play").addEventListener("click", () => {
      caixa.classList.toggle("tocando");
      document.getElementById("btn-play").innerHTML = caixa.classList.contains("tocando") ? ICONES.pausa : ICONES.play;
    });
    document.querySelectorAll("[data-idx]").forEach(b => b.addEventListener("click", () => {
      const v = peca.videos[Number(b.getAttribute("data-idx"))];
      label.textContent = `${v.titulo} (${v.duracao})`;
      caixa.classList.remove("tocando");
      document.getElementById("btn-play").innerHTML = ICONES.play;
      document.querySelectorAll(".item-video").forEach(x => x.classList.remove("ativo"));
      b.classList.add("ativo");
    }));
  });
}, ["MECANICO"]);

/* ---------------- Problemas comuns (diagnóstico) ---------------- */
rota("/peca/:id/problemas", (params) => {
  carregarPeca(params.id, (peca) => {
    const problemas = api.problemasDaPeca(peca);
    montarConteudo(`
      <div class="conteudo">
        ${miniCabecalho(peca, "problemas")}
        <h2 class="titulo-secao-tela">Problemas comuns</h2>
        <p style="margin-bottom:16px;">Diagnóstico rápido para o ${esc(peca.partNumber)}. Toque em um problema para ver causas e solução.</p>
        <ul class="lista-problemas" id="lista-problemas">
          ${problemas.map((pr, i) => `
            <li class="item-problema" data-idx="${i}">
              <button class="item-problema-cab">
                <span>${esc(pr.titulo)}<small>${esc(pr.causas)}</small></span>
                <span class="mais">+</span>
              </button>
              <div class="item-problema-corpo">
                <div class="bloco-problema">
                  <h4>O que verificar</h4>
                  <ul class="checklist">${pr.verificar.map((v, vi) => `<li><label><input type="checkbox" id="chk-${i}-${vi}"><span>${esc(v)}</span></label></li>`).join("")}</ul>
                </div>
                <div class="bloco-problema">
                  <h4>Solução</h4>
                  <p style="color:var(--text);font-size:0.9rem;">${esc(pr.solucao)}</p>
                </div>
                <div class="bloco-problema aviso aviso-cuidado" style="margin-top:14px;">
                  <h4>${ICONES.alerta} Quando procurar suporte</h4>
                  <p>${esc(pr.quandoTecnico)}</p>
                </div>
                <div class="grupo-botoes" style="margin-top:14px;">
                  <a class="btn-secundario btn-pequeno" href="#/ia?peca=${peca.id}&pergunta=${encodeURIComponent(pr.titulo)}">Perguntar à IA sobre isto</a>
                  <a class="btn-secundario btn-pequeno" href="#/chamado/novo?peca=${peca.id}&assunto=${encodeURIComponent(pr.titulo)}">Abrir chamado</a>
                </div>
              </div>
            </li>`).join("")}
        </ul>
      </div>`);
    document.getElementById("lista-problemas").addEventListener("click", (e) => {
      const cab = e.target.closest(".item-problema-cab");
      if(!cab) return;
      cab.closest(".item-problema").classList.toggle("aberto");
    });
  });
}, ["MECANICO"]);

/* ---------------- Manuais da peça (reais: ligados pelo Part Number) ---------------- */
rota("/peca/:id/manual", (params) => {
  carregarPeca(params.id, (peca) => {
    montarConteudo(`<div class="conteudo">${miniCabecalho(peca, "manual")}${estadoCarregando("Buscando os manuais desta peça…")}</div>`);
    api.listarManuaisPublicos(null, null, peca.id).then(manuais => {
      montarConteudo(`
        <div class="conteudo">
          ${miniCabecalho(peca, "manual")}
          <p style="margin:0 0 14px;">Manuais que tratam do <strong>${esc(peca.partNumber)}</strong> — ligados automaticamente quando o código aparece no manual, ou pelo administrador.</p>
          ${manuais.length ? `<div class="lista-resultados">${manuais.map(cardManual).join("")}</div>`
            : estadoVazio({ titulo: "Nenhum manual ligado a esta peça", texto: "Ainda não há manual que cite este Part Number. Você pode procurar entre todos os manuais ou perguntar à IA.",
                acaoHtml: `<a class="btn-secundario" href="#/manuais?q=${encodeURIComponent(peca.partNumber)}">Procurar nos manuais</a>` })}
          <a class="card-ia" href="#/ia?peca=${encodeURIComponent(peca.id)}" style="margin-top:20px;">
            ${ICONES.ia}
            <div><strong>Perguntar à IA sobre o ${esc(peca.partNumber)}</strong><p>A busca começa pelos manuais desta peça, para não misturar com outras.</p></div>
            ${ICONES.seta}
          </a>
        </div>`);
    }).catch(err => {
      montarConteudo(`<div class="conteudo">${miniCabecalho(peca, "manual")}${estadoErro({ titulo: "Não foi possível carregar os manuais", texto: err.message })}</div>`);
    });
  });
}, ["MECANICO"]);

/* ---------------- Rotas genéricas (registradas por último de propósito) ----------------
   "/peca/:id" é a visão geral. "/peca/:id/:fluxo" só existe para o caso em que o mecânico
   veio de um atalho da Home (ex: "/busca/instalacao") e ainda está na visão geral da peça,
   com aquele atalho destacado — por isso precisa vir DEPOIS das rotas fixas acima
   (instalacao, lubrificacao, ferramentas, videos, problemas, manual), senão essas rotas
   fixas nunca seriam alcançadas (o roteador usa a primeira rota que casar). */
rota("/peca/:id", (params) => renderVisaoGeral(params.id, null), ["MECANICO"]);
rota("/peca/:id/:fluxo", (params) => renderVisaoGeral(params.id, params.fluxo), ["MECANICO"]);
