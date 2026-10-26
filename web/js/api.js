/* =====================================================================
   FERSA Care — camada de API (MOCK)
   Toda tela conversa só com este objeto `api`. Quando o backend REST
   existir, basta trocar as implementações abaixo por chamadas fetch()
   mantendo a mesma assinatura — nenhuma tela precisará mudar.
   ===================================================================== */

const LATENCIA = 420; // ms — simula uma chamada de rede real

function atraso(valor, ms = LATENCIA){
  return new Promise((resolve) => setTimeout(() => resolve(valor), ms));
}
function atrasoErro(mensagem, ms = LATENCIA){
  return new Promise((_, reject) => setTimeout(() => reject(new Error(mensagem)), ms));
}

const api = {
  /* ---------------- Autenticação ---------------- */
  login(email, senha){
    const e = (email || "").trim().toLowerCase();
    const usuario = USUARIOS.find(u => u.email.toLowerCase() === e);
    if(!usuario) return atrasoErro("E-mail não encontrado. Confira e tente novamente.");
    if(usuario.senha !== senha) return atrasoErro("Senha incorreta.");
    const { senha: _s, ...semSenha } = usuario;
    return atraso({ ...semSenha });
  },
  recuperarSenha(email){
    if(!email || !email.includes("@")) return atrasoErro("Informe um e-mail válido.");
    return atraso({ enviado: true });
  },

  /* ---------------- Peças / busca tradicional ---------------- */
  buscarPecas(termo, modo){
    const t = (termo || "").trim().toLowerCase();
    const resultado = PECAS.filter(p => {
      if(!t) return true;
      const pn = p.partNumber.toLowerCase().replace(/[\s-]/g, "");
      const tNorm = t.replace(/[\s-]/g, "");
      if(modo === "partNumber") return pn.includes(tNorm);
      if(modo === "categoria") return p.categoria.toLowerCase().includes(t);
      if(modo === "veiculo") return p.aplicacoes.some(a => a.veiculo.toLowerCase().includes(t));
      if(modo === "modelo") return p.aplicacoes.some(a => a.modelo.toLowerCase().includes(t));
      if(modo === "aplicacao") return p.aplicacoes.some(a => (a.veiculo + " " + a.modelo).toLowerCase().includes(t));
      return (
        pn.includes(tNorm) ||
        p.nome.toLowerCase().includes(t) ||
        p.categoria.toLowerCase().includes(t) ||
        p.aplicacoes.some(a => (a.veiculo + " " + a.modelo).toLowerCase().includes(t))
      );
    });
    return atraso(resultado);
  },
  buscarPecaPorId(id){
    const peca = PECAS.find(p => p.id === Number(id));
    if(!peca) return atrasoErro("Peça não encontrada.");
    return atraso(peca);
  },
  listarTodasPecas(){
    return atraso(PECAS);
  },
  ferramentasDaPeca(peca){
    return (peca.ferramentas || []).map(id => FERRAMENTAS_POR_ID[id]).filter(Boolean);
  },
  problemasDaPeca(peca){
    return (peca.problemas || []).map(id => PROBLEMAS_POR_ID[id]).filter(Boolean);
  },
  manualDaPeca(peca){
    return MANUAIS[peca.manualId] || null;
  },

  /* ---------------- Consultas recentes (por sessão do navegador) ---------------- */
  registrarConsultaRecente(peca){
    try{
      const chave = "fersa_recentes";
      let lista = JSON.parse(localStorage.getItem(chave) || "[]");
      lista = lista.filter(id => id !== peca.id);
      lista.unshift(peca.id);
      lista = lista.slice(0, 5);
      localStorage.setItem(chave, JSON.stringify(lista));
    }catch(e){ /* localStorage indisponível — segue sem recentes */ }
  },
  consultasRecentes(){
    try{
      const chave = "fersa_recentes";
      const ids = JSON.parse(localStorage.getItem(chave) || "[]");
      return atraso(ids.map(id => PECAS.find(p => p.id === id)).filter(Boolean));
    }catch(e){ return atraso([]); }
  },

  /* ---------------- Chamados ---------------- */
  abrirChamado(dados){
    SEQ_CHAMADO += 1;
    const novo = {
      id: SEQ_CHAMADO,
      protocolo: "FC-2026-" + SEQ_CHAMADO,
      status: "ABERTO",
      prioridade: dados.prioridade || "MEDIA",
      assunto: dados.assunto,
      descricao: dados.descricao,
      partNumber: dados.partNumber || null,
      pecaId: dados.pecaId || null,
      aplicacao: dados.aplicacao || null,
      solicitante: dados.solicitante || "Mecânico",
      origemIA: !!dados.origemIA,
      consultaIA: dados.consultaIA || null,
      abertoEm: new Date().toLocaleString("pt-BR"),
      atualizadoEm: new Date().toLocaleString("pt-BR"),
      historico: [{ data: new Date().toLocaleString("pt-BR"), texto: "Chamado aberto" + (dados.origemIA ? " a partir da Busca por IA." : ".") }],
      solucaoTecnico: null
    };
    CHAMADOS.unshift(novo);
    return atraso(novo, LATENCIA + 300);
  },
  listarChamados(filtro){
    let lista = CHAMADOS;
    if(filtro && filtro !== "TODOS") lista = lista.filter(c => c.status === filtro);
    return atraso(lista);
  },
  buscarChamadoPorId(id){
    const chamado = CHAMADOS.find(c => c.id === Number(id));
    if(!chamado) return atrasoErro("Chamado não encontrado.");
    return atraso(chamado);
  },
  registrarSolucao(id, solucao){
    const chamado = CHAMADOS.find(c => c.id === Number(id));
    if(!chamado) return atrasoErro("Chamado não encontrado.");
    chamado.status = "RESOLVIDO";
    chamado.solucaoTecnico = solucao;
    chamado.atualizadoEm = new Date().toLocaleString("pt-BR");
    chamado.historico.push({ data: chamado.atualizadoEm, texto: "Solução registrada: " + solucao });
    return atraso(chamado);
  },
  assumirChamado(id, tecnico){
    const chamado = CHAMADOS.find(c => c.id === Number(id));
    if(!chamado) return atrasoErro("Chamado não encontrado.");
    chamado.status = "EM_ATENDIMENTO";
    chamado.atualizadoEm = new Date().toLocaleString("pt-BR");
    chamado.historico.push({ data: chamado.atualizadoEm, texto: `Técnico ${tecnico} assumiu o chamado.` });
    return atraso(chamado);
  },

  /* ---------------- CMS: Peças ---------------- */
  cmsListarPecas(){ return atraso(PECAS); },
  cmsSalvarPeca(dados){
    if(dados.id){
      const idx = PECAS.findIndex(p => p.id === Number(dados.id));
      if(idx >= 0) PECAS[idx] = { ...PECAS[idx], ...dados, id: PECAS[idx].id };
      return atraso(PECAS[idx]);
    }
    const novo = { id: Math.max(0, ...PECAS.map(p => p.id)) + 1, aplicacoes: [], videos: [], passos: [], ferramentas: [], problemas: [], lubrificacao: {}, ...dados };
    PECAS.push(novo);
    return atraso(novo);
  },
  cmsExcluirPeca(id){
    const idx = PECAS.findIndex(p => p.id === Number(id));
    if(idx >= 0) PECAS.splice(idx, 1);
    return atraso({ ok: true });
  },

  /* ---------------- CMS: Ferramentas / Problemas ---------------- */
  cmsListarFerramentas(){ return atraso(FERRAMENTAS); },
  cmsListarProblemas(){ return atraso(PROBLEMAS); },

  /* ---------------- CMS: Dashboard ---------------- */
  cmsEstatisticas(){
    return atraso({
      totalPecas: PECAS.length,
      totalManuais: Object.keys(MANUAIS).length,
      totalTutoriais: PECAS.reduce((n, p) => n + (p.passos ? 1 : 0), 0),
      totalVideos: PECAS.reduce((n, p) => n + (p.videos ? p.videos.length : 0), 0),
      totalFerramentas: FERRAMENTAS.length,
      totalProblemas: PROBLEMAS.length,
      chamadosAbertos: CHAMADOS.filter(c => c.status !== "RESOLVIDO").length,
      porCategoria: Object.entries(
        PECAS.reduce((acc, p) => { acc[p.categoria] = (acc[p.categoria] || 0) + 1; return acc; }, {})
      ),
      lacunasIA: [
        { pergunta: "Qual o torque do parafuso de fixação do FRS-1005?", quando: "20/09/2026" },
        { pergunta: "Posso usar graxa de cálcio no lugar da EP2?", quando: "19/09/2026" }
      ]
    });
  }
};

/* ---------------- Integração REST ----------------
   Já vêm do backend: login, catálogo, chamados, Busca por IA (RAG),
   manuais e categorias (área pública e CMS). Continuam no mock acima só
   as seções do CMS de peças/ferramentas/problemas/usuários, marcadas como
   "demo" nas telas. */
function apiUrl(path){ return FERSA_CONFIG.apiUrl.replace(/\/$/, "") + path; }
let configSupabasePromise;
async function validarConfigSupabase(){
  if(!configSupabasePromise){
    configSupabasePromise = fetch(apiUrl("/api/v1/config"))
      .then(async r => {
        if(!r.ok) throw new Error("Não foi possível carregar a configuração do Supabase.");
        const config = await r.json();
        FERSA_CONFIG.supabaseUrl = config.supabaseUrl || "";
        FERSA_CONFIG.supabaseAnonKey = config.supabaseAnonKey || "";
      })
      .catch(e => { configSupabasePromise = null; throw e; });
  }
  await configSupabasePromise;
  if(!FERSA_CONFIG.supabaseUrl || !FERSA_CONFIG.supabaseAnonKey){
    throw new Error("Configure SUPABASE_URL e SUPABASE_ANON_KEY no .env do backend para habilitar o login.");
  }
}
/* O access_token do Supabase vale 1 hora. Num 401, renovamos com o
   refresh_token e repetimos a chamada uma vez; se nem isso funcionar, a
   sessão acabou de verdade e a pessoa volta para o login. */
async function renovarSessao(){
  const sessao = Sessao.atual();
  if(!sessao?.refreshToken || !FERSA_CONFIG.supabaseUrl) return false;
  try{
    const r = await fetch(`${FERSA_CONFIG.supabaseUrl.replace(/\/$/, "")}/auth/v1/token?grant_type=refresh_token`, {
      method: "POST", headers: { "Content-Type": "application/json", apikey: FERSA_CONFIG.supabaseAnonKey },
      body: JSON.stringify({ refresh_token: sessao.refreshToken })
    });
    if(!r.ok) return false;
    const novo = await r.json();
    Sessao.entrar({ ...sessao, accessToken: novo.access_token, refreshToken: novo.refresh_token || sessao.refreshToken });
    return true;
  }catch(e){ return false; }
}
function sessaoExpirada(){
  Sessao.sair();
  toast("Sua sessão expirou. Entre de novo.", "erro");
  irPara("/login");
}

/* Erro da API com o status HTTP junto (as telas tratam 503 da IA de um jeito próprio). */
class ErroApi extends Error {
  constructor(mensagem, status){ super(mensagem); this.status = status; }
}

async function chamarBackend(path, opcoes, montarHeaders, jaRenovou){
  const sessao = Sessao.atual();
  const headers = montarHeaders();
  if(sessao?.accessToken && !headers.Authorization) headers.Authorization = `Bearer ${sessao.accessToken}`;
  const resposta = await fetch(apiUrl(path), { ...opcoes, headers });
  if(resposta.status === 401 && sessao?.accessToken && !jaRenovou && !(opcoes.headers || {}).Authorization){
    if(await renovarSessao()) return chamarBackend(path, opcoes, montarHeaders, true);
    sessaoExpirada();
    throw new ErroApi("Sessão expirada.", 401);
  }
  return resposta;
}
async function lerResposta(resposta){
  const texto = await resposta.text();
  let dados;
  try{ dados = texto ? JSON.parse(texto) : null; }catch(e){ dados = texto; }
  if(!resposta.ok){
    const mensagem = dados?.mensagem || dados?.erro || dados?.message || `Falha na API (${resposta.status}).`;
    throw new ErroApi(mensagem, resposta.status);
  }
  return dados;
}
/* Depois do login/cadastro no Supabase: pergunta ao backend quem é a pessoa (nome e perfil). */
async function sessaoDoBackend(accessToken, refreshToken){
  try{
    const perfil = await requisicaoBackend("/api/v1/auth/me", { headers: { Authorization: `Bearer ${accessToken}` } });
    return { id: perfil.id, nome: perfil.nome, perfil: perfil.perfil, oficina: "", accessToken, refreshToken };
  }catch(e){
    if(e.status === 404) throw new Error("Sua conta existe, mas o cadastro no FERSA Care não foi concluído. Fale com um administrador.");
    throw e;
  }
}
function traduzirErroCadastro(d){
  const msg = String(d.msg || d.message || d.error_description || d.error || "");
  const codigo = String(d.error_code || d.code || "");
  if(/already registered|already exists|user_already_exists/i.test(msg + codigo)) return "Este e-mail já tem cadastro. Entre com ele ou use \"Esqueci minha senha\".";
  if(/password/i.test(msg) && /(at least|6|weak|short)/i.test(msg)) return "Senha fraca: use pelo menos 6 caracteres.";
  if(/email/i.test(msg) && /(invalid|valid)/i.test(msg)) return "E-mail inválido.";
  if(/signups? (not allowed|disabled)/i.test(msg)) return "O cadastro está desativado no Supabase (Authentication → Sign In / Providers).";
  if(/rate limit|too many/i.test(msg)) return "Muitas tentativas seguidas. Espere um pouco e tente de novo.";
  return msg || "Não foi possível criar a conta.";
}

async function requisicaoBackend(path, opcoes = {}){
  const r = await chamarBackend(path, opcoes, () => ({ "Content-Type": "application/json", ...(opcoes.headers || {}) }));
  return lerResposta(r);
}
/* Upload: com FormData o navegador monta o Content-Type (multipart + boundary) sozinho. */
async function requisicaoMultipart(path, formData){
  const r = await chamarBackend(path, { method: "POST", body: formData }, () => ({}));
  return lerResposta(r);
}
/* PDF protegido (CMS): baixa com o token e abre numa aba nova. */
async function abrirPdfProtegido(path){
  const aba = window.open("", "_blank"); // aberta já no clique, senão o navegador bloqueia o pop-up
  try{
    const r = await chamarBackend(path, { method: "GET" }, () => ({}));
    if(!r.ok) throw new ErroApi(`Não foi possível abrir o PDF (${r.status}).`, r.status);
    const url = URL.createObjectURL(await r.blob());
    if(aba) aba.location = url; else window.open(url, "_blank");
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  }catch(e){
    if(aba) aba.close();
    throw e;
  }
}
function urlPdfPublico(manualId, pagina){
  return apiUrl(`/api/manuais/${encodeURIComponent(manualId)}/pdf`) + (pagina ? `#page=${pagina}` : "");
}
function dataLegivel(valor){
  if(!valor) return "—";
  const d = new Date(valor);
  return Number.isNaN(d.getTime()) ? valor : d.toLocaleString("pt-BR");
}
function normalizarPeca(p){
  const ferramentasDTO = p.ferramentas || [];
  const problemasDTO = p.problemas || [];
  return {
    ...p,
    imagem: p.imagemDescricao || p.imagem || "",
    cuidadosInstalacao: p.cuidadosInstalacao || [],
    errosComunsInstalacao: p.errosComunsInstalacao || [],
    passos: (p.passos || []).slice().sort((a,b) => (a.ordem || 0) - (b.ordem || 0)),
    videos: (p.videos || []).map(v => ({ ...v, duracao: v.duracao || (v.duracaoSegundos ? `${Math.floor(v.duracaoSegundos/60)}:${String(v.duracaoSegundos%60).padStart(2,"0")}` : "") })),
    ferramentas: ferramentasDTO.map(f => f.id || f),
    ferramentasDTO,
    problemas: problemasDTO.map(pr => pr.id || pr),
    problemasDTO,
    manualId: null
  };
}
function normalizarChamado(c){
  return { ...c, id: c.id, solicitante: c.solicitanteNome, aplicacao: c.aplicacaoTexto,
    origemIA: c.origemIa, abertoEm: dataLegivel(c.criadoEm), atualizadoEm: dataLegivel(c.atualizadoEm),
    historico: (c.historico || []).map(h => ({ ...h, data: dataLegivel(h.criadoEm || h.data) })) };
}
Object.assign(api, {
  async login(email, senha){
    await validarConfigSupabase();
    const base = FERSA_CONFIG.supabaseUrl.replace(/\/$/, "");
    const auth = await fetch(`${base}/auth/v1/token?grant_type=password`, {
      method: "POST", headers: { "Content-Type": "application/json", apikey: FERSA_CONFIG.supabaseAnonKey },
      body: JSON.stringify({ email: (email || "").trim(), password: senha })
    });
    const credenciais = await auth.json().catch(() => ({}));
    if(!auth.ok) throw new Error(credenciais.msg || credenciais.message || credenciais.error_description || "Não foi possível entrar. Confira e-mail e senha.");
    return sessaoDoBackend(credenciais.access_token, credenciais.refresh_token);
  },
  /* Cadastro pelo Supabase Auth. O profile (sempre MECANICO) é criado pelo
     trigger de database/cadastro.sql. Com a confirmação de e-mail ligada no
     Supabase, a resposta não traz sessão: a pessoa confirma e depois entra. */
  async cadastrar(nome, email, senha){
    validarConfigSupabase();
    const r = await fetch(`${FERSA_CONFIG.supabaseUrl.replace(/\/$/, "")}/auth/v1/signup`, {
      method: "POST", headers: { "Content-Type": "application/json", apikey: FERSA_CONFIG.supabaseAnonKey },
      body: JSON.stringify({ email: (email || "").trim(), password: senha, data: { nome: (nome || "").trim() } })
    });
    const dados = await r.json().catch(() => ({}));
    if(!r.ok) throw new Error(traduzirErroCadastro(dados));
    const token = dados.access_token || dados.session?.access_token;
    if(!token) return { precisaConfirmar: true };
    return { usuario: await sessaoDoBackend(token, dados.refresh_token || dados.session?.refresh_token) };
  },
  async recuperarSenha(email){
    await validarConfigSupabase();
    const r = await fetch(`${FERSA_CONFIG.supabaseUrl.replace(/\/$/, "")}/auth/v1/recover`, {
      method: "POST", headers: { "Content-Type": "application/json", apikey: FERSA_CONFIG.supabaseAnonKey }, body: JSON.stringify({ email })
    });
    if(!r.ok) throw new Error("Não foi possível solicitar a recuperação de senha.");
    return { enviado: true };
  },
  async buscarPecas(termo, modo){
    const qs = new URLSearchParams(); if(termo) qs.set("termo", termo); if(modo) qs.set("modo", modo);
    return (await requisicaoBackend(`/api/v1/pecas${qs.size ? `?${qs}` : ""}`)).map(normalizarPeca);
  },
  async buscarPecaPorId(id){ return normalizarPeca(await requisicaoBackend(`/api/v1/pecas/${encodeURIComponent(id)}`)); },
  async listarTodasPecas(){ return (await requisicaoBackend("/api/v1/pecas")).map(normalizarPeca); },
  async consultasRecentes(){
    try{
      const ids = JSON.parse(localStorage.getItem("fersa_recentes") || "[]");
      const pecas = await api.listarTodasPecas();
      return ids.map(id => pecas.find(p => String(p.id) === String(id))).filter(Boolean);
    }catch(e){ return []; }
  },
  ferramentasDaPeca(peca){ return peca.ferramentasDTO || (peca.ferramentas || []).map(id => FERRAMENTAS_POR_ID[id]).filter(Boolean); },
  problemasDaPeca(peca){ return peca.problemasDTO || (peca.problemas || []).map(id => PROBLEMAS_POR_ID[id]).filter(Boolean); },
  async abrirChamado(dados){
    const req = { assunto: dados.assunto, descricao: dados.descricao, pecaId: dados.pecaId || null,
      aplicacaoTexto: dados.aplicacao || null, prioridade: dados.prioridade || "MEDIA" };
    return normalizarChamado(await requisicaoBackend("/api/v1/chamados", { method: "POST", body: JSON.stringify(req) }));
  },
  async listarChamados(filtro){
    const lista = (await requisicaoBackend("/api/v1/chamados")).map(normalizarChamado);
    return filtro && filtro !== "TODOS" ? lista.filter(c => c.status === filtro) : lista;
  },
  async buscarChamadoPorId(id){ return normalizarChamado(await requisicaoBackend(`/api/v1/chamados/${encodeURIComponent(id)}`)); },
  async registrarSolucao(id, solucao){ return normalizarChamado(await requisicaoBackend(`/api/v1/chamados/${encodeURIComponent(id)}/solucao`, { method: "POST", body: JSON.stringify({ solucao }) })); },
  async assumirChamado(id){ return normalizarChamado(await requisicaoBackend(`/api/v1/chamados/${encodeURIComponent(id)}/assumir`, { method: "POST" })); },

  /* ---------------- Busca por IA (RAG real: POST /api/chat) ---------------- */
  async perguntarIA(pergunta, peca){
    const texto = (pergunta || "").trim();
    if(!texto) throw new Error("Digite uma pergunta.");
    // Os manuais citam o Part Number; pôr a peça na pergunta ajuda a busca vetorial.
    const enviada = peca ? `Sobre a peça ${peca.partNumber} (${peca.nome}): ${texto}` : texto;
    const r = await requisicaoBackend("/api/chat", { method: "POST", body: JSON.stringify({ pergunta: enviada }) });
    return {
      id: "consulta-" + Date.now(),
      pergunta: texto,
      pecaContexto: peca ? peca.partNumber : null,
      pecaRelacionadaId: peca ? peca.id : null,
      encontrouConteudo: r.encontrou,
      resposta: r.resposta,
      modelo: r.modelo,
      sugestoesPeca: [],
      fontes: (r.fontes || []).map(f => ({
        numero: f.numero,
        manualId: f.manualId,
        documento: f.tituloManual,
        pagina: f.pagina,
        secao: f.pagina ? `Página ${f.pagina}` : "",
        trecho: f.trecho,
        score: f.score,
        pdfUrl: urlPdfPublico(f.manualId, f.pagina)
      }))
    };
  },

  /* ---------------- Manuais (área pública) ---------------- */
  async listarManuaisPublicos(termo, categoriaId){
    const qs = new URLSearchParams();
    if(termo) qs.set("termo", termo);
    if(categoriaId) qs.set("categoriaId", categoriaId);
    return requisicaoBackend(`/api/manuais${qs.size ? `?${qs}` : ""}`);
  },
  async listarCategoriasPublicas(){ return requisicaoBackend("/api/categorias"); },

  /* ---------------- CMS: manuais (só ADMIN) ---------------- */
  async cmsListarManuais(){ return requisicaoBackend("/api/admin/manuais"); },
  async cmsBuscarManual(id){ return requisicaoBackend(`/api/admin/manuais/${encodeURIComponent(id)}`); },
  async cmsEnviarManual(arquivo, titulo, categoriaId){
    const fd = new FormData();
    fd.append("arquivo", arquivo);
    if(titulo) fd.append("titulo", titulo);
    if(categoriaId) fd.append("categoriaId", categoriaId);
    return requisicaoMultipart("/api/admin/manuais", fd);
  },
  async cmsChunksManual(id){ return requisicaoBackend(`/api/admin/manuais/${encodeURIComponent(id)}/chunks`); },
  async cmsAtualizarManual(id, titulo, categoriaId){
    return requisicaoBackend(`/api/admin/manuais/${encodeURIComponent(id)}`, { method: "PUT", body: JSON.stringify({ titulo, categoriaId: categoriaId || null }) });
  },
  async cmsAlterarAtivoManual(id, ativo){
    return requisicaoBackend(`/api/admin/manuais/${encodeURIComponent(id)}/ativo`, { method: "PATCH", body: JSON.stringify({ ativo }) });
  },
  async cmsReprocessarManual(id){ return requisicaoBackend(`/api/admin/manuais/${encodeURIComponent(id)}/reprocessar`, { method: "POST" }); },
  async cmsExcluirManual(id){ return requisicaoBackend(`/api/admin/manuais/${encodeURIComponent(id)}`, { method: "DELETE" }); },
  async cmsAbrirPdfManual(id){ return abrirPdfProtegido(`/api/admin/manuais/${encodeURIComponent(id)}/pdf`); },

  /* ---------------- CMS: importação do SharePoint (só ADMIN) ---------------- */
  async cmsSharePointStatus(){ return requisicaoBackend("/api/admin/sharepoint/status"); },
  async cmsSharePointArquivos(){ return requisicaoBackend("/api/admin/sharepoint/arquivos"); },
  async cmsSharePointImportar(itemIds, categoriaId, categoriaPelaPasta){
    return requisicaoBackend("/api/admin/sharepoint/importar", { method: "POST",
      body: JSON.stringify({ itemIds: itemIds || null, categoriaId: categoriaId || null, categoriaPelaPasta: !!categoriaPelaPasta }) });
  },

  /* ---------------- CMS: usuários (só ADMIN) ---------------- */
  async cmsListarUsuarios(){ return requisicaoBackend("/api/admin/usuarios"); },
  async cmsAlterarPerfilUsuario(id, perfil){
    return requisicaoBackend(`/api/admin/usuarios/${encodeURIComponent(id)}/perfil`, { method: "PATCH", body: JSON.stringify({ perfil }) });
  },

  /* ---------------- CMS: Testar IA — chat livre, sem manuais (só ADMIN) ---------------- */
  async cmsPerguntarLivre(pergunta){
    return requisicaoBackend("/api/admin/ia/livre", { method: "POST", body: JSON.stringify({ pergunta }) });
  },

  /* ---------------- CMS: categorias (só ADMIN) ---------------- */
  async cmsListarCategorias(){ return requisicaoBackend("/api/admin/categorias"); },
  async cmsSalvarCategoria(id, dados){
    const corpo = JSON.stringify({ nome: dados.nome, descricao: dados.descricao || null, ativo: dados.ativo !== false });
    return id
      ? requisicaoBackend(`/api/admin/categorias/${encodeURIComponent(id)}`, { method: "PUT", body: corpo })
      : requisicaoBackend("/api/admin/categorias", { method: "POST", body: corpo });
  },
  async cmsExcluirCategoria(id){ return requisicaoBackend(`/api/admin/categorias/${encodeURIComponent(id)}`, { method: "DELETE" }); }
});
