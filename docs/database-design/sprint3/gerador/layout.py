# -*- coding: utf-8 -*-
"""
Layout do diagrama relacional, compartilhado pelo renderizador PNG e pelo design do Data Modeler.

Cada tabela vira uma caixa (x, y, largura, altura). As FKs viram conectores com pontos
(criancas a direita/abaixo do pai ligam direto; ligacoes que cruzariam outras colunas sobem
por uma "faixa" superior e descem no pai).
"""
from modelo import TABELAS, tabela, todas_fks

ALTURA_CABECALHO = 34
ALTURA_LINHA = 20
MARGEM_INFERIOR = 8
LARGURA_MIN = 300
LARGURA_MAX = 470
TOPO = 110          # y da primeira tabela; acima fica a faixa de conectores
GAP = 34
FAIXA_Y0 = 16
FAIXA_PASSO = 13

COLUNAS = [
    (30, ["APP_USERS", "REFRESH_TOKENS"]),
    (520, ["TRANSCRICOES"]),
    (1040, ["TRANSCRICAO_STATUS_HIST", "TRANSCRICAO_WARNINGS", "TRANSCRICAO_RISCOS_CONS", "TRANSCRICAO_OPORT_CONS",
            "TRANSCRICAO_ALERTAS_CONS", "TRANSCRICAO_ITENS_CRITICOS"]),
    (1560, ["ANALISES"]),
    (2080, ["ANALISE_PRODUTOS", "ANALISE_OPORTUNIDADES", "ANALISE_RISCOS", "ANALISE_RISCOS_CONS", "ANALISE_OPORT_CONS",
            "ANALISE_ALERTAS_CONS", "ANALISE_ITENS_CRITICOS"]),
    (2600, ["ALERTAS", "ALERTA_STATUS_HIST", "ANALISE_FEEDBACKS"]),
]

# Ligacoes que nao sao entre colunas vizinhas (ou pai abaixo do filho) sobem pela faixa superior
SUBVIEWS = {
    "Seguranca e transcricoes": ["APP_USERS", "REFRESH_TOKENS", "TRANSCRICOES", "TRANSCRICAO_STATUS_HIST", "TRANSCRICAO_WARNINGS"],
    "Transcricao e listas consolidadas": ["TRANSCRICOES", "TRANSCRICAO_RISCOS_CONS", "TRANSCRICAO_OPORT_CONS",
                                         "TRANSCRICAO_ALERTAS_CONS", "TRANSCRICAO_ITENS_CRITICOS"],
    "Analise da LeanIA": ["ANALISES", "ANALISE_PRODUTOS", "ANALISE_OPORTUNIDADES", "ANALISE_RISCOS", "ANALISE_RISCOS_CONS",
                          "ANALISE_OPORT_CONS", "ANALISE_ALERTAS_CONS", "ANALISE_ITENS_CRITICOS"],
    "Alertas, feedback e historico": ["ANALISES", "ALERTAS", "ALERTA_STATUS_HIST", "ANALISE_FEEDBACKS"],
}


def texto_coluna(c):
    return "%s %s" % (c["nome"], tipo_curto(c))


def tipo_curto(c):
    if c["identity"]:
        return "NUMBER (19) IDENTITY"
    t = c["tipo"]
    return t.replace("VARCHAR2(", "VARCHAR2 (").replace("NUMBER(", "NUMBER (").replace("TIMESTAMP(", "TIMESTAMP (").replace("FLOAT(", "FLOAT (")


def largura_tabela(t, medir):
    """medir(texto) devolve a largura em px do texto no font usado."""
    w = max([medir(t["nome"]) + 40] + [medir(texto_coluna(c)) + 70 for c in t["colunas"]])
    return int(min(LARGURA_MAX, max(LARGURA_MIN, w)))


def altura_tabela(t):
    return ALTURA_CABECALHO + ALTURA_LINHA * len(t["colunas"]) + MARGEM_INFERIOR


def altura_tabela_dm(t):
    """Altura da caixa no Data Modeler (fonte menor + secoes de chaves abaixo das colunas)."""
    chaves = 1 + len(t["uks"]) + len(t["fks"])
    return int(22 + 14 * len(t["colunas"]) + 8 + 14 * chaves + 14)


def calcular(medir, dm=False):
    caixas = {}
    coluna_de = {}
    for ci, (x, nomes) in enumerate(COLUNAS):
        y = TOPO
        for nome in nomes:
            t = tabela(nome)
            w = largura_tabela(t, medir)
            h = altura_tabela_dm(t) if dm else altura_tabela(t)
            caixas[nome] = {"x": x, "y": y, "w": w, "h": h, "col": ci}
            coluna_de[nome] = ci
            y += h + GAP
    conectores = _conectores(caixas, coluna_de, dm)
    largura = max(c["x"] + c["w"] for c in caixas.values()) + 30
    altura = max(c["y"] + c["h"] for c in caixas.values()) + 30
    return {"caixas": caixas, "conectores": conectores, "largura": largura, "altura": altura}


def _linha_y(caixa, t, nome_col, dm=False):
    """y central da linha da coluna dentro da caixa."""
    idx = [c["nome"] for c in t["colunas"]].index(nome_col)
    if dm:
        return int(caixa["y"] + 22 + 14 * idx + 7)
    return caixa["y"] + ALTURA_CABECALHO + ALTURA_LINHA * idx + ALTURA_LINHA // 2


def _conectores(caixas, coluna_de, dm=False):
    out = []
    faixa = 0
    portas_topo = {n: 0 for n in caixas}
    for filho, fk in todas_fks():
        pai = tabela(fk["ref"])
        cf, cp = caixas[filho["nome"]], caixas[pai["nome"]]
        col_f, col_p = coluna_de[filho["nome"]], coluna_de[pai["nome"]]
        y_f = _linha_y(cf, filho, fk["colunas"][0], dm)
        y_p = _linha_y(cp, pai, fk["ref_colunas"][0], dm)
        item = {"filho": filho["nome"], "pai": pai["nome"], "fk": fk, "pontos": []}
        if col_f == col_p + 1:
            item["pontos"] = [(cf["x"], y_f), (cp["x"] + cp["w"], y_p)]
            item["fim_filho"], item["fim_pai"] = "esq", "dir"
        elif col_f == col_p:
            # mesma coluna: o filho fica abaixo do pai
            x = cf["x"] + 40 + 30 * portas_topo[filho["nome"]]
            portas_topo[filho["nome"]] += 1
            item["pontos"] = [(x, cf["y"]), (x, cp["y"] + cp["h"])]
            item["fim_filho"], item["fim_pai"] = "topo", "base"
        else:
            # cruza colunas: sobe pela faixa superior
            y_lane = FAIXA_Y0 + FAIXA_PASSO * faixa
            faixa += 1
            xf = cf["x"] + 40 + 30 * portas_topo[filho["nome"]]
            portas_topo[filho["nome"]] += 1
            xp = cp["x"] + 40 + 30 * portas_topo[pai["nome"]]
            portas_topo[pai["nome"]] += 1
            item["pontos"] = [(xf, cf["y"]), (xf, y_lane), (xp, y_lane), (xp, cp["y"])]
            item["fim_filho"], item["fim_pai"] = "topo", "topo"
        out.append(item)
    return out


def subview(layout, nomes):
    """Recorta o layout para as tabelas informadas (para as imagens de detalhe)."""
    caixas = {n: layout["caixas"][n] for n in nomes}
    conectores = [c for c in layout["conectores"] if c["filho"] in nomes and c["pai"] in nomes]
    x0 = min(c["x"] for c in caixas.values()) - 30
    y0 = min([c["y"] for c in caixas.values()] + [p[1] for c in conectores for p in c["pontos"]]) - 20
    x1 = max(c["x"] + c["w"] for c in caixas.values()) + 30
    y1 = max(c["y"] + c["h"] for c in caixas.values()) + 30
    return {"caixas": caixas, "conectores": conectores, "x0": x0, "y0": y0, "largura": x1 - x0, "altura": y1 - y0}
