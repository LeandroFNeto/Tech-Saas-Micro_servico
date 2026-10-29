"""Gera docs/painel-projeto.html: onde o projeto está, o que falta, paleta e fontes."""

from __future__ import annotations

import html
import json
import re
import subprocess
from datetime import datetime, timedelta, timezone
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
SAIDA = Path(__file__).with_name("painel-projeto.html")
OPENSPEC = RAIZ / "openspec"
DESIGN = RAIZ / "especificacao" / "front" / "telas.design.json"
PAPEIS = RAIZ / ".cursor" / "rules" / "papeis-ia.mdc"

ETAPAS_FLUXO = ["Pesquisa", "Proposta", "Programação", "Testes", "Qualidade", "Segurança", "Revisão", "Deploy"]

USO_CORES = {
    ("brand", "50"): "fundo do painel",
    ("brand", "200"): "bordas",
    ("brand", "600"): "dicas e rótulos",
    ("brand", "700"): "texto secundário",
    ("brand", "800"): "faixa do login",
    ("brand", "900"): "texto principal e topo",
    ("accent", "400"): "hover e foco",
    ("accent", "500"): "botão principal",
}


def ler(caminho: Path) -> str:
    return caminho.read_text(encoding="utf-8") if caminho.exists() else ""


def inline(texto: str) -> str:
    seguro = html.escape(texto, quote=False)
    seguro = re.sub(r"`([^`]+)`", r"<code>\1</code>", seguro)
    seguro = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", seguro)
    seguro = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", seguro)
    return seguro


def tabela_md(texto: str, cabecalho_inicia: str) -> list[list[str]]:
    linhas = texto.splitlines()
    for i, linha in enumerate(linhas):
        if linha.startswith("|") and cabecalho_inicia in linha:
            corpo = []
            for item in linhas[i + 2:]:
                if not item.startswith("|"):
                    break
                corpo.append([c.strip() for c in item.strip().strip("|").split("|")])
            return corpo
    return []


def secao_md(texto: str, titulo: str) -> str:
    m = re.search(rf"^## {re.escape(titulo)}\s*\n(.*?)(?=^## |\Z)", texto, re.S | re.M)
    return m.group(1).strip() if m else ""


def tarefas(caminho: Path) -> list[dict]:
    itens = []
    for linha in ler(caminho).splitlines():
        m = re.match(r"^(\s*)- \[( |x|X)\] (.+)$", linha)
        if m:
            itens.append({"nivel": len(m.group(1)) // 2, "feito": m.group(2) != " ", "texto": m.group(3)})
    return itens


def entra(proposta: str) -> list[str]:
    m = re.search(r"^### Entra\s*\n(.*?)(?=^### |^## |\Z)", proposta, re.S | re.M)
    itens = []
    for linha in (m.group(1) if m else "").splitlines():
        achou = re.match(r"^- (.+)$", linha)
        if achou:
            itens.append(re.split(r"(?<=[.:])\s", achou.group(1), maxsplit=1)[0])
    return itens


def arquivos_design(design: str) -> dict[str, list[str]]:
    grupos: dict[str, list[str]] = {}
    atual = None
    for linha in secao_md(design, "Arquivos").splitlines():
        cabeca = re.match(r"^- ([^:]+):\s*(.*)$", linha)
        if cabeca:
            atual = cabeca.group(1).strip()
            grupos[atual] = []
            if cabeca.group(2):
                grupos[atual].append(cabeca.group(2))
            continue
        item = re.match(r"^\s+- (.+)$", linha)
        if item and atual:
            grupos[atual].append(item.group(1))
    return {nome: itens for nome, itens in grupos.items() if itens}


def ordem(proposta: str) -> str:
    for paragrafo in proposta.split("\n\n"):
        if re.search(r"Vem (depois|antes) de|Depende de|Não depende", paragrafo):
            return paragrafo.strip()
    return ""


def central() -> dict[str, dict]:
    linhas = tabela_md(ler(OPENSPEC / "central-tarefas.md"), "| Tarefa")
    saida = {}
    for c in linhas:
        if len(c) < 6:
            continue
        nome = re.sub(r"\[([^\]]+)\].*", r"\1", c[0])
        saida[nome] = {"etapa": c[1], "ia": c[2].strip("`"), "local": c[3], "voltas": c[4], "estado": c[5]}
    return saida


def indice_etapa(etapa: str) -> int:
    texto = etapa.lower()
    for i, nome in enumerate(ETAPAS_FLUXO):
        if texto.startswith(nome.lower()):
            return i
    return 1


def classe_estado(estado: str) -> str:
    e = estado.lower()
    if e == "aprovado":
        return "ok"
    if e in ("erro", "reprovado"):
        return "ruim"
    if e == "em andamento":
        return "andamento"
    return "espera"


def changes_abertas(quadro: dict[str, dict]) -> list[dict]:
    lista = []
    for pasta in sorted((OPENSPEC / "changes").iterdir()):
        if not pasta.is_dir():
            continue
        proposta = ler(pasta / "proposal.md")
        problema = secao_md(proposta, "Problema").split("\n\n")[0]
        itens = tarefas(pasta / "tasks.md")
        linha = quadro.get(pasta.name)
        if linha:
            etapa, estado, ia, voltas = linha["etapa"], linha["estado"], linha["ia"], linha["voltas"]
        else:
            etapa, estado, ia, voltas = "proposta", "aguardando aprovado", "", "0"
        lista.append({
            "nome": pasta.name,
            "problema": problema,
            "itens": itens,
            "etapa": etapa,
            "estado": estado,
            "ia": ia,
            "voltas": voltas,
            "depende": "Depende de" in proposta or "Vem depois de" in proposta,
            "entra": entra(proposta),
            "arquivos": arquivos_design(ler(pasta / "design.md")),
            "ordem": ordem(proposta),
        })
    return lista


def arquivadas() -> list[str]:
    pasta = OPENSPEC / "archive"
    return sorted(p.name for p in pasta.iterdir() if p.is_dir()) if pasta.exists() else []


def requisitos() -> list[dict]:
    dominios = []
    for spec in sorted((OPENSPEC / "specs").glob("*/spec.md")):
        ids = re.findall(r"^### ([A-Z]+-\d+) (.+)$", ler(spec), re.M)
        dominios.append({"dominio": spec.parent.name, "ids": ids})
    return dominios


def modelos() -> list[list[str]]:
    return [c for c in tabela_md(ler(PAPEIS), "| Etapa") if len(c) >= 3]


def commit_atual() -> str:
    try:
        return subprocess.run(
            ["git", "log", "-1", "--format=%h %s"], cwd=RAIZ, capture_output=True, text=True, check=True
        ).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return ""


def ramo_atual() -> str:
    try:
        return subprocess.run(
            ["git", "branch", "--show-current"], cwd=RAIZ, capture_output=True, text=True, check=True
        ).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return ""


def contraste(hexa: str) -> str:
    r, g, b = (int(hexa[i:i + 2], 16) for i in (1, 3, 5))
    return "#1A0B2E" if (0.299 * r + 0.587 * g + 0.114 * b) > 150 else "#ffffff"


def html_trilha(indice: int) -> str:
    partes = []
    for i, nome in enumerate(ETAPAS_FLUXO):
        estado = "feita" if i < indice else "atual" if i == indice else "futura"
        partes.append(f'<li class="passo {estado}"><span class="bolinha"></span><span class="rotulo-passo">{nome}</span></li>')
    return f'<ol class="trilha" aria-label="Etapas da equipe">{"".join(partes)}</ol>'


def html_change(c: dict) -> str:
    total = len(c["itens"])
    feitos = sum(1 for i in c["itens"] if i["feito"])
    pct = round(100 * feitos / total) if total else 0
    proxima = next((i for i in c["itens"] if not i["feito"]), None)
    lista = "".join(
        f'<li class="tarefa nivel-{i["nivel"]}{" feita" if i["feito"] else ""}">'
        f'<span class="caixa" aria-hidden="true">{"✓" if i["feito"] else ""}</span>'
        f'<span>{inline(i["texto"])}</span></li>'
        for i in c["itens"]
    )
    ia = f'<span class="meta">IA: <code>{html.escape(c["ia"])}</code></span>' if c["ia"] else ""
    depende = '<span class="meta aviso">tem pré-requisito</span>' if c["depende"] else ""
    proximo = f'<p class="proximo"><span class="eyebrow">Próximo passo</span>{inline(proxima["texto"])}</p>' if proxima else ""
    ordem_html = f'<p class="ordem"><span class="eyebrow">Ordem</span>{inline(c["ordem"])}</p>' if c["ordem"] else ""
    entra_html = ""
    if c["entra"]:
        itens = "".join(f"<li>{inline(t)}</li>" for t in c["entra"])
        entra_html = f'<details open><summary>O que entra ({len(c["entra"])})</summary><ul class="entra">{itens}</ul></details>'
    arquivos_html = ""
    if c["arquivos"]:
        total_arq = sum(len(v) for v in c["arquivos"].values())
        blocos = "".join(
            f'<div class="grupo-arquivos"><h4>{html.escape(nome)}</h4><ul>'
            + "".join(f"<li>{inline(i)}</li>" for i in itens)
            + "</ul></div>"
            for nome, itens in c["arquivos"].items()
        )
        arquivos_html = f'<details><summary>Arquivos que a change toca ({total_arq})</summary><div class="arquivos-change">{blocos}</div></details>'
    return f"""
<article class="change" id="{html.escape(c['nome'])}">
  <header class="change-topo">
    <div class="change-titulo">
      <h3>{html.escape(c['nome'])}</h3>
      <div class="metas">
        <span class="pill {classe_estado(c['estado'])}">{html.escape(c['estado'])}</span>
        <span class="meta">Etapa: {html.escape(c['etapa'])}</span>
        <span class="meta">Voltas: {html.escape(c['voltas'])}/2</span>
        {ia}{depende}
      </div>
    </div>
    <div class="progresso" aria-label="{feitos} de {total} tarefas feitas">
      <span class="numero">{feitos}<small>/{total}</small></span>
      <span class="barra"><span style="width:{pct}%"></span></span>
    </div>
  </header>
  {html_trilha(indice_etapa(c['etapa']))}
  <p class="problema">{inline(c['problema'])}</p>
  {proximo}
  {ordem_html}
  {entra_html}
  {arquivos_html}
  <details>
    <summary>Ver todas as tarefas</summary>
    <ul class="tarefas">{lista}</ul>
  </details>
</article>"""


def html_quadro(quadro: dict[str, dict], abertas: list[dict]) -> str:
    nomes = list(quadro.keys()) + [c["nome"] for c in abertas if c["nome"] not in quadro]
    linhas = []
    for nome in nomes:
        q = quadro.get(nome)
        if q:
            etapa, ia, voltas, estado = q["etapa"], q["ia"], q["voltas"], q["estado"]
        else:
            etapa, ia, voltas, estado = "proposta", "—", "0", "aguardando aprovado"
        linhas.append(
            f"<tr><td><strong>{html.escape(nome)}</strong></td><td>{html.escape(etapa)}</td>"
            f"<td><code>{html.escape(ia)}</code></td><td class='num'>{html.escape(voltas)}</td>"
            f"<td><span class='pill {classe_estado(estado)}'>{html.escape(estado)}</span></td></tr>"
        )
    return "".join(linhas)


def achatar_tons(tons: dict):
    for tom, valor in tons.items():
        if isinstance(valor, dict):
            for sub, hexa in valor.items():
                yield f"{tom}-{sub}", hexa
        else:
            yield tom, valor


def html_paleta(cores: dict) -> str:
    blocos = []
    for familia, tons in cores.items():
        amostras = []
        for tom, hexa in achatar_tons(tons):
            uso = USO_CORES.get((familia, tom), "")
            amostras.append(
                f'<li class="amostra"><span class="cor" style="background:{hexa};color:{contraste(hexa)}">{tom}</span>'
                f'<span class="hex">{hexa.upper()}</span>'
                f'<span class="token">{familia}-{tom}</span>'
                f'<span class="uso">{uso}</span></li>'
            )
        blocos.append(f'<div class="familia"><h3>{familia}</h3><ul class="amostras">{"".join(amostras)}</ul></div>')
    return "".join(blocos)


def html_fontes(fontes: dict) -> str:
    papeis = {
        "sans": ("Texto, botões, campos e títulos das telas", "font-sans", "Reserva confirmada para sábado, 14/11. Diária R$ 500,00."),
        "display": ("Marca gamb e títulos de destaque", "font-display", "O gamb responde e reserva"),
    }
    cards = []
    for chave, pilha in fontes.items():
        uso, classe, exemplo = papeis.get(chave, ("", f"font-{chave}", "Exemplo"))
        familia = pilha[0]
        cards.append(
            f'<div class="fonte"><p class="eyebrow">{classe}</p>'
            f'<p class="fonte-nome" style="font-family:\'{familia}\',{",".join(pilha[1:])}">{familia}</p>'
            f'<p class="fonte-exemplo" style="font-family:\'{familia}\',{",".join(pilha[1:])}">{html.escape(exemplo)}</p>'
            f'<p class="uso">{uso}</p><p class="pilha"><code>{html.escape(", ".join(pilha))}</code></p></div>'
        )
    return "".join(cards)


def html_componentes(componentes: dict) -> str:
    return "".join(
        f"<tr><td><strong>{html.escape(nome)}</strong></td><td><code>{html.escape(classe)}</code></td></tr>"
        for nome, classe in componentes.items()
    )


def html_modelos(linhas: list[list[str]]) -> str:
    return "".join(
        f"<tr><td>{inline(c[0])}</td><td>{inline(c[2])}</td><td>{inline(c[1])}</td></tr>" for c in linhas
    )


def html_requisitos(dominios: list[dict]) -> str:
    blocos = []
    for d in dominios:
        itens = "".join(f'<li><code>{i}</code> {html.escape(t)}</li>' for i, t in d["ids"])
        blocos.append(f'<div class="dominio"><h3>{d["dominio"]} <small>{len(d["ids"])}</small></h3><ul>{itens}</ul></div>')
    return "".join(blocos)


def montar() -> str:
    design = json.loads(ler(DESIGN) or "{}")
    tema = design.get("tema", {})
    quadro = central()
    abertas = changes_abertas(quadro)
    arquivo = arquivadas()
    reqs = requisitos()
    agora = datetime.now(timezone(timedelta(hours=-3))).strftime("%d/%m/%Y às %H:%M")
    total_reqs = sum(len(d["ids"]) for d in reqs)
    aguardando = sum(1 for c in abertas if c["estado"] == "aguardando aprovado")
    andamento = sum(1 for c in abertas if c["estado"] == "em andamento")

    resumo = [
        (str(andamento), "em andamento"),
        (str(aguardando), "aguardando seu aprovado"),
        (str(len(arquivo)), "changes arquivadas"),
        (str(total_reqs), "requisitos nas specs"),
    ]
    html_resumo = "".join(f'<li><span class="numero">{n}</span><span>{t}</span></li>' for n, t in resumo)
    html_arquivo = "".join(f"<li><code>{html.escape(n)}</code></li>" for n in arquivo)

    return MODELO.format(
        agora=agora,
        commit=html.escape(commit_atual()),
        ramo=html.escape(ramo_atual()),
        resumo=html_resumo,
        quadro=html_quadro(quadro, abertas),
        changes="".join(html_change(c) for c in abertas) or "<p>Nenhuma change aberta.</p>",
        arquivo=html_arquivo,
        modelos=html_modelos(modelos()),
        paleta=html_paleta(tema.get("cores", {})),
        fontes=html_fontes(tema.get("fontes", {})),
        componentes=html_componentes(tema.get("componentes", {})),
        requisitos=html_requisitos(reqs),
    )


MODELO = """<title>Painel do gamb</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap">
<style>
:root {{
  --fundo: #f6f0fb;
  --superficie: #ffffff;
  --superficie-2: #ebe0f5;
  --texto: #1A0B2E;
  --texto-2: #4e2473;
  --texto-3: #652d90;
  --borda: #d4bce8;
  --marca: #2E1A47;
  --marca-fundo: #1A0B2E;
  --laranja: #F7931E;
  --laranja-forte: #c4620d;
  --ok: #1f7a4a;
  --ok-fundo: #e2f4ea;
  --andamento: #8a4b00;
  --andamento-fundo: #ffe4d1;
  --espera: #4e2473;
  --espera-fundo: #ebe0f5;
  --ruim: #b42318;
  --ruim-fundo: #fde7e5;
  --fonte-display: 'Cinzel', Georgia, serif;
  --fonte-texto: 'Inter', ui-sans-serif, system-ui, sans-serif;
  --fonte-mono: 'JetBrains Mono', ui-monospace, Consolas, monospace;
}}
@media (prefers-color-scheme: dark) {{
  :root:not([data-theme="light"]) {{
    --fundo: #140823; --superficie: #1f1033; --superficie-2: #2E1A47; --texto: #f6f0fb; --texto-2: #d4bce8;
    --texto-3: #b889d4; --borda: #3d2560; --marca: #2E1A47; --marca-fundo: #0f0720; --laranja: #FF8C42;
    --laranja-forte: #ffab70; --ok: #7ad9a4; --ok-fundo: #12331f; --andamento: #ffc9a3; --andamento-fundo: #3a2210;
    --espera: #d4bce8; --espera-fundo: #2E1A47; --ruim: #ffb4ab; --ruim-fundo: #3d1512; color-scheme: dark;
  }}
}}
:root[data-theme="dark"] {{
  --fundo: #140823; --superficie: #1f1033; --superficie-2: #2E1A47; --texto: #f6f0fb; --texto-2: #d4bce8;
  --texto-3: #b889d4; --borda: #3d2560; --marca: #2E1A47; --marca-fundo: #0f0720; --laranja: #FF8C42;
  --laranja-forte: #ffab70; --ok: #7ad9a4; --ok-fundo: #12331f; --andamento: #ffc9a3; --andamento-fundo: #3a2210;
  --espera: #d4bce8; --espera-fundo: #2E1A47; --ruim: #ffb4ab; --ruim-fundo: #3d1512; color-scheme: dark;
}}
* {{ box-sizing: border-box; }}
body {{ margin: 0; background: var(--fundo); color: var(--texto); font: 15px/1.6 var(--fonte-texto); -webkit-font-smoothing: antialiased; }}
code {{ font-family: var(--fonte-mono); font-size: 0.86em; background: var(--superficie-2); color: var(--texto-2); padding: 1px 5px; border-radius: 4px; overflow-wrap: anywhere; }}
h1, h2, h3 {{ text-wrap: balance; margin: 0; }}
.faixa {{ background: var(--marca-fundo); background-image: radial-gradient(circle at 85% 0%, rgba(247,147,30,0.30), transparent 55%); color: #f6f0fb; }}
.conteiner {{ max-width: 1080px; margin: 0 auto; padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); }}
.topo {{ padding-block: 36px 32px; display: grid; gap: 20px; }}
.marca {{ font-family: var(--fonte-display); font-weight: 700; letter-spacing: 0.18em; text-transform: uppercase; color: #FF8C42; font-size: 15px; }}
.topo h1 {{ font-size: clamp(28px, 5vw, 40px); font-weight: 700; line-height: 1.15; }}
.topo .sub {{ color: #d4bce8; margin: 0; font-size: 14px; }}
.topo .sub code {{ background: rgba(255,255,255,0.1); color: #f6f0fb; }}
.resumo {{ list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr)); gap: 12px; }}
.resumo li {{ display: flex; align-items: baseline; gap: 10px; border-top: 1px solid rgba(255,255,255,0.18); padding-top: 10px; color: #d4bce8; font-size: 14px; }}
.resumo .numero {{ font-size: 30px; font-weight: 700; color: #ffffff; font-variant-numeric: tabular-nums; }}
nav.indice {{ position: sticky; top: env(safe-area-inset-top, 0px); z-index: 5; background: var(--fundo); border-bottom: 1px solid var(--borda); }}
body::before {{ content: ""; position: fixed; inset: 0 0 auto; height: env(safe-area-inset-top, 0px); z-index: 10; background: var(--fundo); }}
nav.indice ul {{ list-style: none; margin: 0; padding-block: 10px; display: flex; gap: 6px; overflow-x: auto; }}
nav.indice a {{ display: inline-block; white-space: nowrap; padding: 6px 12px; border-radius: 999px; color: var(--texto-2); text-decoration: none; font-weight: 500; font-size: 14px; }}
nav.indice a:hover, nav.indice a:focus-visible {{ background: var(--superficie-2); color: var(--texto); outline: none; }}
main section {{ padding-block: 40px 8px; scroll-margin-top: 64px; }}
.secao-topo {{ display: grid; gap: 4px; margin-bottom: 20px; }}
.eyebrow {{ display: block; font-size: 12px; font-weight: 600; letter-spacing: 0.12em; text-transform: uppercase; color: var(--texto-3); margin: 0; }}
h2 {{ font-size: clamp(22px, 3.4vw, 28px); font-weight: 700; line-height: 1.2; }}
.secao-topo p {{ margin: 4px 0 0; color: var(--texto-2); max-width: 65ch; }}
.rolagem {{ overflow-x: auto; border: 1px solid var(--borda); border-radius: 12px; background: var(--superficie); }}
table {{ width: 100%; border-collapse: collapse; font-size: 14px; }}
th, td {{ text-align: left; padding: 11px 14px; border-bottom: 1px solid var(--borda); vertical-align: top; }}
tr:last-child td {{ border-bottom: 0; }}
th {{ font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--texto-3); font-weight: 600; background: var(--superficie-2); }}
td.num {{ font-variant-numeric: tabular-nums; }}
.pill {{ display: inline-flex; align-items: center; white-space: nowrap; border-radius: 999px; padding: 2px 10px; font-size: 12px; font-weight: 600; }}
.pill.ok {{ background: var(--ok-fundo); color: var(--ok); }}
.pill.andamento {{ background: var(--andamento-fundo); color: var(--andamento); }}
.pill.espera {{ background: var(--espera-fundo); color: var(--espera); }}
.pill.ruim {{ background: var(--ruim-fundo); color: var(--ruim); }}
.changes {{ display: grid; gap: 16px; }}
.change {{ background: var(--superficie); border: 1px solid var(--borda); border-radius: 14px; padding: 20px; display: grid; gap: 14px; min-width: 0; }}
.change-topo {{ display: flex; flex-wrap: wrap; justify-content: space-between; gap: 16px; }}
.change-titulo {{ min-width: 0; flex: 1 1 320px; display: grid; gap: 8px; }}
.change h3 {{ font-family: var(--fonte-mono); font-size: 17px; font-weight: 500; overflow-wrap: anywhere; }}
.metas {{ display: flex; flex-wrap: wrap; gap: 8px 14px; align-items: center; }}
.meta {{ font-size: 13px; color: var(--texto-2); }}
.meta.aviso {{ color: var(--laranja-forte); font-weight: 600; }}
.progresso {{ flex: 0 0 160px; display: grid; gap: 6px; align-content: start; }}
.progresso .numero {{ font-size: 26px; font-weight: 700; font-variant-numeric: tabular-nums; line-height: 1; }}
.progresso small {{ font-size: 15px; color: var(--texto-3); font-weight: 500; }}
.barra {{ display: block; height: 6px; border-radius: 99px; background: var(--superficie-2); overflow: hidden; }}
.barra span {{ display: block; height: 100%; background: var(--laranja); }}
.trilha {{ list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(8, minmax(0, 1fr)); gap: 4px; }}
.passo {{ display: grid; gap: 6px; font-size: 12px; color: var(--texto-3); min-width: 0; }}
.passo .bolinha {{ height: 6px; border-radius: 99px; background: var(--superficie-2); }}
.passo.feita .bolinha {{ background: var(--texto-3); }}
.passo.atual .bolinha {{ background: var(--laranja); }}
.passo.atual {{ color: var(--texto); font-weight: 700; }}
.rotulo-passo {{ overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }}
.problema {{ margin: 0; color: var(--texto-2); max-width: 75ch; }}
.proximo {{ margin: 0; padding: 12px 14px; border-radius: 10px; background: var(--superficie-2); display: grid; gap: 4px; }}
.ordem {{ margin: 0; font-size: 14px; color: var(--texto-2); display: grid; gap: 4px; }}
.entra {{ margin: 10px 0 0; padding-left: 20px; display: grid; gap: 6px; font-size: 14px; color: var(--texto-2); }}
.arquivos-change {{ margin-top: 10px; display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; }}
.grupo-arquivos {{ min-width: 0; background: var(--superficie-2); border-radius: 10px; padding: 12px 14px; }}
.grupo-arquivos h4 {{ margin: 0 0 8px; font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--texto-3); }}
.grupo-arquivos ul {{ margin: 0; padding-left: 16px; display: grid; gap: 6px; font-size: 13px; color: var(--texto-2); overflow-wrap: anywhere; }}
details summary {{ cursor: pointer; color: var(--texto-2); font-weight: 600; font-size: 14px; padding-block: 4px; }}
details summary:focus-visible {{ outline: 2px solid var(--laranja); outline-offset: 2px; border-radius: 4px; }}
.tarefas {{ list-style: none; margin: 10px 0 0; padding: 0; display: grid; gap: 8px; font-size: 14px; }}
.tarefa {{ display: flex; gap: 10px; align-items: flex-start; min-width: 0; }}
.tarefa > span:last-child {{ min-width: 0; }}
.tarefa.nivel-1 {{ padding-left: 28px; }}
.tarefa.feita > span:last-child {{ color: var(--texto-3); text-decoration: line-through; }}
.caixa {{ flex: 0 0 18px; height: 18px; margin-top: 2px; border: 1.5px solid var(--borda); border-radius: 5px; display: grid; place-items: center; font-size: 12px; color: var(--ok); }}
.feita .caixa {{ border-color: var(--ok); background: var(--ok-fundo); }}
.arquivo {{ list-style: none; margin: 16px 0 0; padding: 0; display: flex; flex-wrap: wrap; gap: 8px; }}
.familia + .familia {{ margin-top: 24px; }}
.familia h3 {{ font-size: 15px; text-transform: capitalize; margin-bottom: 10px; }}
.amostras {{ list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(118px, 1fr)); gap: 10px; }}
.amostra {{ display: grid; gap: 2px; font-size: 12px; }}
.cor {{ height: 64px; border-radius: 10px; display: flex; align-items: flex-end; padding: 6px 8px; font-weight: 600; border: 1px solid var(--borda); }}
.hex {{ font-family: var(--fonte-mono); font-weight: 500; margin-top: 4px; }}
.token {{ color: var(--texto-3); font-family: var(--fonte-mono); }}
.uso {{ color: var(--texto-2); }}
.fontes {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 320px), 1fr)); gap: 16px; }}
.fonte {{ border-top: 2px solid var(--texto); padding-top: 14px; display: grid; gap: 6px; min-width: 0; }}
.fonte p {{ margin: 0; }}
.fonte-nome {{ font-size: 34px; font-weight: 700; line-height: 1.1; }}
.fonte-exemplo {{ font-size: 18px; color: var(--texto-2); }}
.pilha code {{ font-size: 12px; }}
.dominios {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr)); gap: 20px 24px; }}
.dominio h3 {{ font-size: 15px; display: flex; gap: 8px; align-items: baseline; text-transform: capitalize; }}
.dominio small {{ color: var(--texto-3); font-weight: 500; font-variant-numeric: tabular-nums; }}
.dominio ul {{ margin: 8px 0 0; padding: 0; list-style: none; display: grid; gap: 4px; font-size: 14px; color: var(--texto-2); }}
footer {{ padding-block: 40px; color: var(--texto-3); font-size: 13px; }}
footer p {{ margin: 0; }}
@media (max-width: 640px) {{
  .trilha {{ grid-template-columns: repeat(4, minmax(0, 1fr)); row-gap: 10px; }}
  .progresso {{ flex-basis: 100%; }}
}}
@media (prefers-reduced-motion: reduce) {{ html {{ scroll-behavior: auto; }} }}
</style>

<header class="faixa">
  <div class="conteiner topo">
    <span class="marca">gamb</span>
    <h1>Painel do projeto</h1>
    <p class="sub">Gerado em {agora} · ramo <code>{ramo}</code> · último commit <code>{commit}</code></p>
    <ul class="resumo">{resumo}</ul>
  </div>
</header>

<nav class="indice" aria-label="Seções">
  <div class="conteiner">
    <ul>
      <li><a href="#quadro">Onde estamos</a></li>
      <li><a href="#changes">O que falta</a></li>
      <li><a href="#equipe">Equipe</a></li>
      <li><a href="#paleta">Paleta</a></li>
      <li><a href="#fontes">Fontes</a></li>
      <li><a href="#componentes">Componentes</a></li>
      <li><a href="#specs">Specs</a></li>
    </ul>
  </div>
</nav>

<main class="conteiner">
  <section id="quadro">
    <div class="secao-topo">
      <p class="eyebrow">openspec/central-tarefas.md</p>
      <h2>Onde estamos</h2>
      <p>Cada tarefa, em que etapa da equipe ela está e quem está com ela. Change sem linha na central ainda espera o seu aprovado.</p>
    </div>
    <div class="rolagem">
      <table>
        <thead><tr><th>Tarefa</th><th>Etapa</th><th>IA</th><th>Voltas</th><th>Estado</th></tr></thead>
        <tbody>{quadro}</tbody>
      </table>
    </div>
  </section>

  <section id="changes">
    <div class="secao-topo">
      <p class="eyebrow">openspec/changes</p>
      <h2>O que falta fazer</h2>
      <p>Uma change por funcionalidade. A trilha mostra a etapa atual; o próximo passo é a primeira tarefa sem marcar no <code>tasks.md</code>.</p>
    </div>
    <div class="changes">{changes}</div>
    <ul class="arquivo" aria-label="Changes arquivadas"><li class="eyebrow" style="align-self:center">Arquivadas</li>{arquivo}</ul>
  </section>

  <section id="equipe">
    <div class="secao-topo">
      <p class="eyebrow">.cursor/rules/papeis-ia.mdc</p>
      <h2>Equipe e modelos</h2>
      <p>A ordem das etapas e o modelo de cada uma nas duas ferramentas. Programação só começa com aprovado na central; no máximo 2 voltas.</p>
    </div>
    <div class="rolagem">
      <table>
        <thead><tr><th>Etapa</th><th>Claude</th><th>Cursor</th></tr></thead>
        <tbody>{modelos}</tbody>
      </table>
    </div>
  </section>

  <section id="paleta">
    <div class="secao-topo">
      <p class="eyebrow">especificacao/front/telas.design.json</p>
      <h2>Paleta</h2>
      <p>Roxo da marca para fundo, texto e bordas; laranja só para ação e foco. No Tailwind a classe é o token, por exemplo <code>bg-brand-900</code> ou <code>text-accent-500</code>.</p>
    </div>
    {paleta}
  </section>

  <section id="fontes">
    <div class="secao-topo">
      <p class="eyebrow">Google Fonts em frontend/src/index.html</p>
      <h2>Fontes</h2>
      <p>Campo de digitação com no mínimo 16 px, para o iPhone não dar zoom.</p>
    </div>
    <div class="fontes">{fontes}</div>
  </section>

  <section id="componentes">
    <div class="secao-topo">
      <p class="eyebrow">tema.componentes</p>
      <h2>Componentes</h2>
      <p>Classes prontas em <code>frontend/src/styles.css</code>. Use estas antes de escrever Tailwind solto.</p>
    </div>
    <div class="rolagem">
      <table>
        <thead><tr><th>Peça</th><th>Classe</th></tr></thead>
        <tbody>{componentes}</tbody>
      </table>
    </div>
  </section>

  <section id="specs">
    <div class="secao-topo">
      <p class="eyebrow">openspec/specs</p>
      <h2>Requisitos já valendo</h2>
      <p>O que o sistema faz hoje, por domínio. Requisito novo só entra aqui depois que a change é aplicada e arquivada.</p>
    </div>
    <div class="dominios">{requisitos}</div>
  </section>
</main>

<footer class="conteiner">
  <p>Gerado por <code>python docs/gerar-painel-projeto.py</code>. Para atualizar, rode de novo.</p>
</footer>
"""


if __name__ == "__main__":
    SAIDA.write_text(montar(), encoding="utf-8")
    print(SAIDA)
