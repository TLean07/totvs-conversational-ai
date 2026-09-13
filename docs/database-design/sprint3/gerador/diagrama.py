# -*- coding: utf-8 -*-
"""Renderiza o diagrama relacional (estilo Oracle SQL Developer Data Modeler) em PNG com PIL."""
import math
from PIL import Image, ImageDraw, ImageFont

import layout
from modelo import tabela, fk_identificadora, fk_obrigatoria

FONTE = "C:/Windows/Fonts/arial.ttf"
FONTE_B = "C:/Windows/Fonts/arialbd.ttf"
COR_BORDA = (90, 90, 120)
COR_CABECALHO = (221, 228, 247)
COR_CORPO = (250, 250, 255)
COR_TITULO = (20, 40, 120)
COR_TIPO = (0, 110, 60)
COR_TEXTO = (25, 25, 25)
COR_NN = (200, 30, 30)
COR_LINHA = (40, 40, 40)


def _fontes(escala=1.0):
    return (ImageFont.truetype(FONTE_B, int(13 * escala)), ImageFont.truetype(FONTE, int(12 * escala)),
            ImageFont.truetype(FONTE_B, int(12 * escala)))


def _medidor():
    f = ImageFont.truetype(FONTE, 12)
    return lambda s: f.getlength(s)


def calcular_layout():
    return layout.calcular(_medidor())


def _marcadores(t, c):
    m = []
    if c["nome"] in t["pk"]["colunas"]:
        m.append("P")
    if any(c["nome"] in fk["colunas"] for fk in t["fks"]):
        m.append("F")
    if any(c["nome"] in uk["colunas"] for uk in t["uks"]):
        m.append("U")
    return "".join(m)


def _desenha_tabela(d, nome, caixa, fontes, dx=0, dy=0):
    fb, fn, fnb = fontes
    t = tabela(nome)
    x, y, w, h = caixa["x"] - dx, caixa["y"] - dy, caixa["w"], caixa["h"]
    d.rectangle([x, y, x + w, y + h], fill=COR_CORPO, outline=COR_BORDA, width=2)
    d.rectangle([x, y, x + w, y + layout.ALTURA_CABECALHO], fill=COR_CABECALHO, outline=COR_BORDA, width=2)
    d.text((x + 10, y + 9), nome, font=fb, fill=COR_TITULO)
    yy = y + layout.ALTURA_CABECALHO
    pk_fim = None
    for c in t["colunas"]:
        marc = _marcadores(t, c)
        nn = "*" if not c["nulo"] else ""
        d.text((x + 8, yy + 3), marc, font=fnb, fill=COR_TITULO)
        d.text((x + 32, yy + 3), nn, font=fnb, fill=COR_NN)
        d.text((x + 44, yy + 3), c["nome"], font=fn, fill=COR_TEXTO)
        tipo = layout.tipo_curto(c)
        tw = fn.getlength(tipo)
        d.text((x + w - 8 - tw, yy + 3), tipo, font=fn, fill=COR_TIPO)
        yy += layout.ALTURA_LINHA
        if c["nome"] in t["pk"]["colunas"]:
            pk_fim = yy
    if pk_fim:
        d.line([x, pk_fim, x + w, pk_fim], fill=COR_BORDA, width=1)


def _tracejada(d, p0, p1, cor, largura, tracejado):
    if not tracejado:
        d.line([p0, p1], fill=cor, width=largura)
        return
    (x0, y0), (x1, y1) = p0, p1
    comp = math.hypot(x1 - x0, y1 - y0)
    if comp == 0:
        return
    n = int(comp // 10)
    for i in range(n + 1):
        a = i / max(n, 1)
        b = min(1.0, a + 5 / comp)
        d.line([(x0 + (x1 - x0) * a, y0 + (y1 - y0) * a), (x0 + (x1 - x0) * b, y0 + (y1 - y0) * b)], fill=cor, width=largura)


def _pe_de_galinha(d, ponto, lado, cor):
    """Simbolo N (crow's foot) na ponta do filho."""
    x, y = ponto
    s = 12
    if lado == "esq":      # entra pela esquerda da caixa: o pe aponta para a direita
        d.line([(x - s, y), (x, y - 7)], fill=cor, width=2)
        d.line([(x - s, y), (x, y)], fill=cor, width=2)
        d.line([(x - s, y), (x, y + 7)], fill=cor, width=2)
    elif lado == "topo":
        d.line([(x, y - s), (x - 7, y)], fill=cor, width=2)
        d.line([(x, y - s), (x, y)], fill=cor, width=2)
        d.line([(x, y - s), (x + 7, y)], fill=cor, width=2)


def _fim_pai(d, ponto, lado, obrigatoria, cor):
    """Barra (1 obrigatorio) ou circulo (0..1 opcional) na ponta do pai."""
    x, y = ponto
    if lado == "dir":
        if obrigatoria:
            d.line([(x + 8, y - 7), (x + 8, y + 7)], fill=cor, width=2)
        else:
            d.ellipse([x + 4, y - 5, x + 14, y + 5], outline=cor, width=2, fill=(255, 255, 255))
    elif lado in ("base", "topo"):
        yy = y + 8 if lado == "base" else y - 8
        if obrigatoria:
            d.line([(x - 7, yy), (x + 7, yy)], fill=cor, width=2)
        else:
            d.ellipse([x - 5, yy - 5, x + 5, yy + 5], outline=cor, width=2, fill=(255, 255, 255))


def _desenha_conector(d, con, dx=0, dy=0, fonte_rotulo=None):
    filho = tabela(con["filho"])
    fk = con["fk"]
    tracejado = not fk_identificadora(filho, fk)
    obrig = fk_obrigatoria(filho, fk)
    pts = [(x - dx, y - dy) for x, y in con["pontos"]]
    for a, b in zip(pts, pts[1:]):
        _tracejada(d, a, b, COR_LINHA, 2, tracejado)
    _pe_de_galinha(d, pts[0], con["fim_filho"], COR_LINHA)
    _fim_pai(d, pts[-1], con["fim_pai"], obrig, COR_LINHA)
    if fonte_rotulo:
        # rotulo com o nome da FK no meio do primeiro segmento (acima da linha)
        (x0, y0), (x1, y1) = pts[0], pts[1]
        tw = fonte_rotulo.getlength(fk["nome"])
        if con["fim_filho"] == "esq":
            pos = ((x0 + x1) / 2 - tw / 2, min(y0, y1) - 18 if abs(y1 - y0) < 24 else (y0 + y1) / 2 - 20)
        else:
            pos = (x0 + 8, (y0 + y1) / 2 - 7)
        d.rectangle([pos[0] - 2, pos[1] - 1, pos[0] + tw + 2, pos[1] + 13], fill=(255, 255, 255))
        d.text(pos, fk["nome"], font=fonte_rotulo, fill=(90, 90, 90))


def _legenda(d, x, y, fontes):
    fb, fn, fnb = fontes
    d.rectangle([x, y, x + 470, y + 118], fill=(255, 255, 250), outline=COR_BORDA, width=1)
    d.text((x + 10, y + 6), "Legenda (notacao do Data Modeler / IE)", font=fb, fill=COR_TITULO)
    itens = [
        ("P", "chave primaria    F  chave estrangeira    U  unica    *  NOT NULL"),
        ("", "linha continua = relacionamento identificador (FK faz parte da PK)"),
        ("", "linha tracejada = relacionamento nao identificador"),
        ("", "pe de galinha = lado N (muitos)   |  barra = lado 1 obrigatorio   o = lado 1 opcional"),
    ]
    yy = y + 30
    for m, txt in itens:
        d.text((x + 10, yy), m, font=fnb, fill=COR_TITULO)
        d.text((x + 28, yy), txt, font=fn, fill=COR_TEXTO)
        yy += 20


def renderizar_completo(caminho, escala=1.0, rotulos=False):
    lay = calcular_layout()
    fontes = _fontes()
    W, H = lay["largura"], lay["altura"] + 140
    img = Image.new("RGB", (W, H), (255, 255, 255))
    d = ImageDraw.Draw(img)
    for con in lay["conectores"]:
        _desenha_conector(d, con, fonte_rotulo=fontes[1] if rotulos else None)
    for nome, caixa in lay["caixas"].items():
        _desenha_tabela(d, nome, caixa, fontes)
    _legenda(d, 30, lay["altura"] + 5, fontes)
    if escala != 1.0:
        img = img.resize((int(W * escala), int(H * escala)), Image.LANCZOS)
    img.save(caminho, optimize=True)
    return lay


def renderizar_subview(caminho, nomes, rotulos=True):
    lay = calcular_layout()
    sub = layout.subview(lay, nomes)
    fontes = _fontes()
    img = Image.new("RGB", (sub["largura"], sub["altura"]), (255, 255, 255))
    d = ImageDraw.Draw(img)
    for con in sub["conectores"]:
        _desenha_conector(d, con, sub["x0"], sub["y0"], fonte_rotulo=fontes[1] if rotulos else None)
    for nome, caixa in sub["caixas"].items():
        _desenha_tabela(d, nome, caixa, fontes, sub["x0"], sub["y0"])
    img.save(caminho, optimize=True)
    return sub
