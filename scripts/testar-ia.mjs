#!/usr/bin/env node
/*
 * FERSA Care — roda as perguntas de teste da semana 4 contra a API e gera
 * a tabela de resultados (Markdown + CSV) para o README e os slides.
 *
 * Uso (Node 18+, sem instalar nada):
 *   node scripts/testar-ia.mjs
 *   node scripts/testar-ia.mjs --api http://localhost:8080 --gabarito docs/manuais-ficticios/gabarito.json
 *
 * Pré-requisitos: backend no ar e os manuais do gabarito importados e
 * PRONTOS (com as peças ligadas — botão "Detectar peças nos manuais").
 *
 * A conferência automática é uma AJUDA: ela olha se os termos esperados
 * aparecem na resposta e se a fonte certa foi citada. A nota final é
 * de vocês — a coluna "Revisão" do Markdown é para isso.
 */
import { readFile, writeFile } from "node:fs/promises";

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith("--")) acc.push([a.slice(2), arr[i + 1]]);
  return acc;
}, []));
const API = (args.api || "http://localhost:8080").replace(/\/$/, "");
const GABARITO = args.gabarito || "docs/manuais-ficticios/gabarito.json";
const SAIDA = args.saida || "resultado-testes-ia";
const PAUSA_MS = Number(args.pausa || 1500); // folga para a cota por minuto do Gemini

const normalizar = (s) => String(s || "").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
const soLetrasNumeros = (s) => normalizar(s).replace(/[^a-z0-9]/g, "");
const esperar = (ms) => new Promise(r => setTimeout(r, ms));

async function json(url, opcoes) {
  const r = await fetch(url, opcoes);
  const corpo = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(`HTTP ${r.status}: ${corpo.mensagem || corpo.erro || r.statusText}`);
  return corpo;
}

/* Part Number → id da peça, a partir das peças ligadas aos manuais públicos (não precisa de login). */
async function mapaDePecas() {
  const manuais = await json(`${API}/api/manuais`);
  const mapa = new Map();
  for (const m of manuais) for (const p of m.pecas || []) mapa.set(p.partNumber, p.id);
  return { mapa, totalManuais: manuais.length };
}

function conferir(pergunta, r) {
  if (pergunta.tipo === "nao_encontrar") {
    return { ok: !r.encontrou, termos: !r.encontrou, fonte: true };
  }
  const resposta = normalizar(r.resposta);
  const termos = (pergunta.conferir || []).every(grupo => grupo.some(t => resposta.includes(normalizar(t))));
  const alvos = String(pergunta.fonte || "").split("|").map(soLetrasNumeros).filter(Boolean);
  const fonte = !alvos.length || (r.fontes || []).some(f => alvos.some(a => soLetrasNumeros(f.tituloManual).includes(a)));
  return { ok: r.encontrou && termos && fonte, termos, fonte };
}

const celula = (s) => String(s ?? "").replace(/\|/g, "\\|").replace(/\s+/g, " ").trim();
const csv = (s) => `"${String(s ?? "").replace(/"/g, '""').replace(/\s+/g, " ").trim()}"`;

async function main() {
  const gabarito = JSON.parse(await readFile(GABARITO, "utf8"));
  console.log(`API: ${API}\nGabarito: ${GABARITO} (${gabarito.perguntas.length} perguntas)\n`);
  await json(`${API}/actuator/health`).catch(() => { throw new Error(`Backend fora do ar em ${API}.`); });
  const { mapa, totalManuais } = await mapaDePecas();
  console.log(`${totalManuais} manual(is) público(s); peças ligadas: ${[...mapa.keys()].join(", ") || "nenhuma"}\n`);

  const linhas = [];
  for (const p of gabarito.perguntas) {
    const pecaId = p.peca ? mapa.get(p.peca) : null;
    if (p.peca && !pecaId) console.warn(`  aviso: ${p.peca} não está ligado a nenhum manual — pergunta vai sem peça.`);
    const inicio = Date.now();
    let r, erro = null;
    try {
      r = await json(`${API}/api/chat`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ pergunta: p.pergunta, pecaId: pecaId || null })
      });
    } catch (e) {
      erro = e.message;
      r = { encontrou: false, resposta: `ERRO: ${e.message}`, fontes: [] };
    }
    const ms = Date.now() - inicio;
    const c = erro ? { ok: false, termos: false, fonte: false } : conferir(p, r);
    const fontes = (r.fontes || []).map(f => `${f.tituloManual}${f.pagina ? ` p.${f.pagina}` : ""}`).join("; ");
    linhas.push({ p, r, c, ms, fontes, erro });
    console.log(`${c.ok ? "✅" : "❌"} ${String(p.id).padStart(2)}. ${p.pergunta}${p.peca ? ` [${p.peca}]` : ""}`);
    console.log(`     resposta: ${String(r.resposta).replace(/\s+/g, " ").slice(0, 140)}`);
    console.log(`     fontes: ${fontes || "—"} · ${(ms / 1000).toFixed(1)} s${!erro && !c.termos ? " · termos esperados ausentes" : ""}${!erro && !c.fonte ? " · fonte esperada não citada" : ""}`);
    await esperar(PAUSA_MS);
  }

  const acertos = linhas.filter(l => l.c.ok).length;
  const tempoMedio = linhas.reduce((s, l) => s + l.ms, 0) / linhas.length / 1000;
  const quando = new Date().toLocaleString("pt-BR");
  const md = [
    `# Resultado dos testes da IA — ${quando}`,
    "",
    `**Acertos (conferência automática): ${acertos}/${linhas.length}** · tempo médio de resposta: ${tempoMedio.toFixed(1)} s · API: ${API}`,
    "",
    "A conferência automática verifica se os termos esperados aparecem na resposta e se a fonte certa foi citada. Revisem cada linha e anotem a nota final na coluna Revisão.",
    "",
    "| # | Pergunta | Peça | Esperado | Resposta da IA | Fontes citadas | Auto | Tempo | Revisão |",
    "|---|---|---|---|---|---|---|---|---|",
    ...linhas.map(({ p, r, c, ms, fontes }) =>
      `| ${p.id} | ${celula(p.pergunta)} | ${p.peca || "—"} | ${celula(p.esperado)} | ${celula(r.resposta)} | ${celula(fontes) || "—"} | ${c.ok ? "✅" : "❌"} | ${(ms / 1000).toFixed(1)} s |  |`),
    "",
    "## O que cada pergunta testa",
    "",
    ...gabarito.perguntas.map(p => `${p.id}. ${p.objetivo}`),
    ""
  ].join("\n");
  const planilha = ["id,pergunta,peca,esperado,resposta,encontrou,fontes,auto_ok,termos_ok,fonte_ok,tempo_s,modelo",
    ...linhas.map(({ p, r, c, ms, fontes }) => [p.id, csv(p.pergunta), p.peca || "", csv(p.esperado), csv(r.resposta), r.encontrou,
      csv(fontes), c.ok, c.termos, c.fonte, (ms / 1000).toFixed(1), r.modelo || ""].join(","))].join("\n");

  await writeFile(`${SAIDA}.md`, md, "utf8");
  await writeFile(`${SAIDA}.csv`, "﻿" + planilha, "utf8"); // BOM: o Excel abre com acentos certos
  console.log(`\nAcertos: ${acertos}/${linhas.length} · tempo médio ${tempoMedio.toFixed(1)} s`);
  console.log(`Gerado: ${SAIDA}.md e ${SAIDA}.csv`);
}

main().catch(e => { console.error("Falhou:", e.message); process.exit(1); });
