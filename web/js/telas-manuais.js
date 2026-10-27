/* =====================================================================
   FERSA Care — Manuais (área pública: abre sem login)
   Busca nos manuais ativos e já processados (GET /api/manuais), por
   título, categoria ou texto do próprio manual.
   ===================================================================== */

rota("/manuais", () => {
  const sessao = Sessao.atual();
  const termoInicial = obterQuery().get("q") || "";
  montarConteudo(`
    <div class="conteudo">
      ${sessao ? `<a class="voltar" href="#/inicio">${ICONES.setaEsq} Voltar ao início</a>` : ""}
      <h1>Manuais técnicos</h1>
      <p style="margin:6px 0 16px;">Busque pelo título, pela categoria ou por qualquer palavra que apareça dentro do manual.</p>

      <form id="form-manuais">
        <div class="barra-busca">
          <input id="busca-manuais" type="text" placeholder="Part Number (ex: FRS-1001), título ou assunto (graxa, folga axial…)" autocomplete="off" value="${esc(termoInicial)}">
          <button type="submit">Buscar</button>
        </div>
      </form>
      <div class="modos-busca" id="filtro-categorias" role="tablist">
        <button class="modo ativo" data-categoria="" type="button">Todas</button>
      </div>
      <p class="total-resultados" id="total-manuais"></p>
      <div id="lista-manuais"></div>

      <a class="card-ia" href="#/ia" style="margin-top:24px;">
        ${ICONES.ia}
        <div><strong>Não achou o que procura?</strong><p>Pergunte em linguagem natural: a IA procura a resposta dentro dos manuais e mostra a página.</p></div>
        ${ICONES.seta}
      </a>
    </div>`);
  ligarManuaisPublicos();
}, null);

function ligarManuaisPublicos(){
  const campo = document.getElementById("busca-manuais");
  const lista = document.getElementById("lista-manuais");
  const total = document.getElementById("total-manuais");
  const filtros = document.getElementById("filtro-categorias");
  let categoriaAtiva = "";
  let espera = null;
  let pedido = 0; // descarta respostas que chegam fora de ordem

  api.listarCategoriasPublicas().then(cats => {
    filtros.insertAdjacentHTML("beforeend",
      cats.map(c => `<button class="modo" data-categoria="${esc(c.id)}" type="button">${esc(c.nome)}</button>`).join(""));
  }).catch(() => { /* sem categorias, o filtro fica só com "Todas" */ });

  function buscar(){
    const meu = ++pedido;
    lista.innerHTML = `<div class="esqueleto"></div><div class="esqueleto"></div>`;
    total.textContent = "";
    api.listarManuaisPublicos(campo.value.trim(), categoriaAtiva).then(manuais => {
      if(meu !== pedido) return;
      if(!manuais.length){
        lista.innerHTML = estadoVazio({
          titulo: "Nenhum manual encontrado",
          texto: campo.value.trim() ? "Tente outra palavra ou outra categoria." : "Ainda não há manuais publicados.",
          acaoHtml: `<a class="btn-secundario" href="#/ia">Perguntar à IA</a>`
        });
        return;
      }
      total.textContent = `${manuais.length} manua${manuais.length > 1 ? "is" : "l"} encontrado${manuais.length > 1 ? "s" : ""}`;
      lista.innerHTML = `<div class="lista-resultados">${manuais.map(cardManual).join("")}</div>`;
    }).catch(err => {
      if(meu !== pedido) return;
      lista.innerHTML = estadoErro({ titulo: "Não foi possível buscar os manuais", texto: err.message });
    });
  }

  document.getElementById("form-manuais").addEventListener("submit", (e) => { e.preventDefault(); buscar(); });
  campo.addEventListener("input", () => { clearTimeout(espera); espera = setTimeout(buscar, 350); });
  filtros.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-categoria]");
    if(!btn) return;
    filtros.querySelectorAll(".modo").forEach(b => b.classList.remove("ativo"));
    btn.classList.add("ativo");
    categoriaAtiva = btn.getAttribute("data-categoria");
    buscar();
  });
  buscar();
}

/* Part Numbers das peças de que o manual trata (ligadas sozinhas pelo texto ou pelo admin). */
function chipsPartNumber(pecas){
  if(!pecas || !pecas.length) return "";
  return `<span class="chips-pn">${pecas.map(p => `<span class="chip-pn" title="${esc(p.nome)}">${esc(p.partNumber)}</span>`).join("")}</span>`;
}

function cardManual(m){
  const meta = [m.totalPaginas ? `${m.totalPaginas} página${m.totalPaginas > 1 ? "s" : ""}` : null,
    `Atualizado em ${new Date(m.atualizadoEm).toLocaleDateString("pt-BR")}`].filter(Boolean).join(" · ");
  return `
    <div class="resultado-peca">
      <div class="info-peca">
        <span class="pn">${esc(m.categoriaNome || "Manual")}</span>
        <span class="nome">${esc(m.titulo)}</span>
        <span class="meta">${esc(meta)}</span>
        ${chipsPartNumber(m.pecas)}
      </div>
      <div class="acoes-card grupo-botoes">
        <a class="btn-principal btn-pequeno" href="${esc(urlPdfPublico(m.id))}" target="_blank" rel="noopener">${ICONES.documento} Abrir PDF</a>
        <a class="btn-secundario btn-pequeno" href="#/ia?${m.pecas && m.pecas.length === 1 ? `peca=${encodeURIComponent(m.pecas[0].id)}&` : ""}rascunho=${encodeURIComponent("Sobre o " + m.titulo + ": ")}">${ICONES.ia} Perguntar à IA</a>
      </div>
    </div>`;
}
