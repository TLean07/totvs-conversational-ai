# -*- coding: utf-8 -*-
"""Diagrama de casos de uso (UML) do TOTVS Inteligência Conversacional, desenhado com PIL em preto e branco."""
import math
from PIL import Image, ImageDraw, ImageFont

FONTE = "C:/Windows/Fonts/arial.ttf"
FONTE_B = "C:/Windows/Fonts/arialbd.ttf"
FONTE_I = "C:/Windows/Fonts/ariali.ttf"

# Pacotes (título, coluna, casos de uso) — cada UC: (id, texto)
PACOTES = [
    ("Autenticação e administração", [("UC01", "Autenticar-se\n(login / logout)"), ("UC02", "Renovar sessão\n(refresh token)"), ("UC03", "Gerenciar usuários\ne perfis")]),
    ("Ingestão e gestão de transcrições", [("UC04", "Submeter transcrição\n(texto)"), ("UC06", "Enviar áudio para\ntranscrição"), ("UC05", "Importar em lote\n(JSON / CSV TOTVS)"),
                                           ("UC07", "Listar e filtrar\ntranscrições"), ("UC08", "Visualizar detalhe\nda análise"), ("UC09", "Excluir transcrição"), ("UC10", "Reprocessar análise")]),
    ("Central de alertas", [("UC11", "Listar e filtrar\nalertas"), ("UC12", "Marcar alerta como\nvisto / resolvido")]),
    ("Dashboard e insights", [("UC13", "Visualizar dashboard\nexecutivo"), ("UC14", "Consultar insights de\nriscos e oportunidades")]),
    ("Feedback adaptativo", [("UC15", "Avaliar análise\n(útil / ajuste)"), ("UC16", "Ajustar contexto\nadaptativo da LeanIA")]),
    ("Motor de IA (LeanIA)", [("UC17", "Analisar transcrição"), ("UC18", "Classificar sentimento"), ("UC19", "Identificar produtos\ne concorrentes"),
                             ("UC20", "Detectar oportunidades\ne riscos"), ("UC21", "Gerar resumo, persona,\nbudget e próxima ação"), ("UC22", "Gerar alertas")]),
]

# Relações include/extend: (origem, destino, tipo)
RELACOES = [
    ("UC01", "UC02", "include"), ("UC04", "UC17", "include"), ("UC05", "UC17", "include"), ("UC10", "UC17", "include"),
    ("UC06", "UC04", "extend"), ("UC17", "UC18", "include"), ("UC17", "UC19", "include"), ("UC17", "UC20", "include"),
    ("UC17", "UC21", "include"), ("UC20", "UC22", "include"), ("UC15", "UC16", "include"), ("UC16", "UC17", "extend"),
    ("UC13", "UC14", "extend"),
]

# Atores: (nome, lista de UCs associados). "Usuário autenticado" generaliza os perfis.
ATORES = [
    ("Usuário\nautenticado", ["UC01", "UC07", "UC08", "UC13"]),
    ("Diretor\nComercial", ["UC11", "UC12"]),
    ("Gerente de\nVendas", ["UC04", "UC09", "UC10"]),
    ("Account\nExecutive", ["UC04", "UC06", "UC15"]),
    ("CS Manager", ["UC11", "UC12", "UC14"]),
    ("Analista\nde BI", ["UC05", "UC14"]),
    ("Administrador", ["UC03"]),
]
ATORES_SEC = [("LeanIA\n(motor de IA)", ["UC17", "UC22"]), ("Serviço de\ntranscrição\nde áudio", ["UC06"])]


def _fontes():
    return ImageFont.truetype(FONTE, 15), ImageFont.truetype(FONTE_B, 16), ImageFont.truetype(FONTE_I, 13), ImageFont.truetype(FONTE_B, 22)


def _ator(d, x, y, nome, fn, cor=(0, 0, 0)):
    d.ellipse([x - 12, y - 12, x + 12, y + 12], outline=cor, width=2, fill=(255, 255, 255))
    d.line([x, y + 12, x, y + 48], fill=cor, width=2)
    d.line([x - 22, y + 24, x + 22, y + 24], fill=cor, width=2)
    d.line([x, y + 48, x - 18, y + 76], fill=cor, width=2)
    d.line([x, y + 48, x + 18, y + 76], fill=cor, width=2)
    for i, linha in enumerate(nome.split("\n")):
        w = fn.getlength(linha)
        d.text((x - w / 2, y + 82 + i * 17), linha, font=fn, fill=cor)


def _elipse(d, cx, cy, w, h, uid, texto, fn, fi):
    d.ellipse([cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2], outline=(0, 0, 0), width=2, fill=(255, 255, 255))
    linhas = texto.split("\n")
    total = 15 + 16 * len(linhas)
    y = cy - total / 2
    d.text((cx - fi.getlength(uid) / 2, y), uid, font=fi, fill=(70, 70, 70))
    y += 15
    for linha in linhas:
        d.text((cx - fn.getlength(linha) / 2, y), linha, font=fn, fill=(0, 0, 0))
        y += 16


def _seta_tracejada(d, p0, p1, rotulo, fi, encurta=0):
    (x0, y0), (x1, y1) = p0, p1
    comp = math.hypot(x1 - x0, y1 - y0)
    if comp == 0:
        return
    ux, uy = (x1 - x0) / comp, (y1 - y0) / comp
    x1, y1 = x1 - ux * encurta, y1 - uy * encurta
    x0, y0 = x0 + ux * encurta, y0 + uy * encurta
    comp = math.hypot(x1 - x0, y1 - y0)
    n = int(comp // 12)
    for i in range(n + 1):
        a = i / max(n, 1)
        b = min(1.0, a + 6 / comp)
        d.line([(x0 + (x1 - x0) * a, y0 + (y1 - y0) * a), (x0 + (x1 - x0) * b, y0 + (y1 - y0) * b)], fill=(0, 0, 0), width=2)
    # ponta aberta
    ang = math.atan2(y1 - y0, x1 - x0)
    for s in (+0.5, -0.5):
        d.line([(x1, y1), (x1 - 14 * math.cos(ang + s), y1 - 14 * math.sin(ang + s))], fill=(0, 0, 0), width=2)
    if not rotulo:
        return
    mx, my = (x0 + x1) / 2, (y0 + y1) / 2
    # desloca o rotulo para o lado da linha (perpendicular)
    mx, my = mx - uy * 13, my + ux * 13
    tw = fi.getlength(rotulo)
    d.rectangle([mx - tw / 2 - 2, my - 8, mx + tw / 2 + 2, my + 8], fill=(255, 255, 255))
    d.text((mx - tw / 2, my - 7), rotulo, font=fi, fill=(0, 0, 0))


def _borda_elipse(cx, cy, w, h, para):
    """Ponto na borda da elipse na direção do ponto 'para'."""
    dx, dy = para[0] - cx, para[1] - cy
    if dx == 0 and dy == 0:
        return cx, cy
    t = 1 / math.sqrt((dx / (w / 2)) ** 2 + (dy / (h / 2)) ** 2)
    return cx + dx * t, cy + dy * t


def renderizar(caminho):
    fn, fb, fi, ft = _fontes()
    W, H = 2200, 1540
    img = Image.new("RGB", (W, H), (255, 255, 255))
    d = ImageDraw.Draw(img)

    sx0, sy0, sx1, sy1 = 330, 70, 1850, 1480
    d.rectangle([sx0, sy0, sx1, sy1], outline=(0, 0, 0), width=2)
    titulo = "Sistema: TOTVS Inteligência Conversacional"
    d.text((sx0 + (sx1 - sx0) / 2 - ft.getlength(titulo) / 2, sy0 + 10), titulo, font=ft, fill=(0, 0, 0))

    EW, EH = 212, 78
    LIN = EH + 30
    pos = {}

    def pacote(nome, ucs, px, py, largura, por_linha):
        nlin = math.ceil(len(ucs) / por_linha)
        alt = 44 + nlin * LIN
        for x in range(px, px + largura, 10):
            d.line([x, py, min(x + 5, px + largura), py], fill=(0, 0, 0), width=1)
            d.line([x, py + alt, min(x + 5, px + largura), py + alt], fill=(0, 0, 0), width=1)
        for y in range(py, py + alt, 10):
            d.line([px, y, px, min(y + 5, py + alt)], fill=(0, 0, 0), width=1)
            d.line([px + largura, y, px + largura, min(y + 5, py + alt)], fill=(0, 0, 0), width=1)
        d.text((px + 12, py + 8), nome, font=fb, fill=(0, 0, 0))
        for i, (uid, txt) in enumerate(ucs):
            c, r = i % por_linha, i // por_linha
            espaco = largura / por_linha
            pos[uid] = (px + espaco * (c + 0.5), py + 44 + r * LIN + LIN / 2)
        return alt

    pac = dict(PACOTES)
    # coluna esquerda: pacotes na ordem dos atores; coluna direita: LeanIA
    lx, lw = sx0 + 30, 800
    y = sy0 + 60
    for nome in ["Autenticação e administração", "Ingestão e gestão de transcrições", "Dashboard e insights", "Central de alertas", "Feedback adaptativo"]:
        y += pacote(nome, pac[nome], lx, y, lw, 3) + 26
    rx, rw = sx0 + 870, 620
    # LeanIA: orquestrador no topo, tres casos incluidos na 2a linha e dois na 3a (evita setas por cima de elipses)
    py = sy0 + 330
    alt = 44 + 3 * LIN
    for x in range(rx, rx + rw, 10):
        d.line([x, py, min(x + 5, rx + rw), py], fill=(0, 0, 0), width=1)
        d.line([x, py + alt, min(x + 5, rx + rw), py + alt], fill=(0, 0, 0), width=1)
    for yy in range(py, py + alt, 10):
        d.line([rx, yy, rx, min(yy + 5, py + alt)], fill=(0, 0, 0), width=1)
        d.line([rx + rw, yy, rx + rw, min(yy + 5, py + alt)], fill=(0, 0, 0), width=1)
    d.text((rx + 12, py + 8), "Motor de IA (LeanIA)", font=fb, fill=(0, 0, 0))
    pos["UC17"] = (rx + rw / 2, py + 44 + LIN / 2)
    for i, uid in enumerate(["UC18", "UC19", "UC20"]):
        pos[uid] = (rx + rw / 3 * (i + 0.5), py + 44 + LIN + LIN / 2)
    for i, uid in enumerate(["UC21", "UC22"]):
        pos[uid] = (rx + rw / 2 * (i + 0.5), py + 44 + 2 * LIN + LIN / 2)

    for uid, (cx, cy) in pos.items():
        txt = next(t for _, ucs in PACOTES for u, t in ucs if u == uid)
        _elipse(d, cx, cy, EW, EH, uid, txt, fn, fi)

    for a, b, tipo in RELACOES:
        pa, pb = pos[a], pos[b]
        p0 = _borda_elipse(pa[0], pa[1], EW, EH, pb)
        p1 = _borda_elipse(pb[0], pb[1], EW, EH, pa)
        _seta_tracejada(d, p0, p1, "«%s»" % tipo, fi)

    # atores primarios (esquerda), na ordem dos pacotes
    ax = 140
    ordem = ["Usuário\nautenticado", "Administrador", "Gerente de\nVendas", "Account\nExecutive", "Analista\nde BI", "CS Manager", "Diretor\nComercial"]
    atores = dict(ATORES)
    ys = [110, 310, 500, 690, 880, 1070, 1260]
    for nome, ay in zip(ordem, ys):
        _ator(d, ax, ay, nome, fn)
        for uid in atores[nome]:
            cx, cy = pos[uid]
            p1 = _borda_elipse(cx, cy, EW, EH, (ax + 26, ay + 40))
            d.line([(ax + 26, ay + 40), p1], fill=(110, 110, 110), width=1)
    # generalizacao dos perfis para "Usuario autenticado"
    gx = ax + 105
    d.line([(gx, ys[1] + 40), (gx, ys[0] + 100)], fill=(0, 0, 0), width=1)
    d.line([(gx, ys[1] + 40), (gx, ys[-1] + 40)], fill=(0, 0, 0), width=1)
    for ay in ys[1:]:
        d.line([(ax + 26, ay + 40), (gx, ay + 40)], fill=(0, 0, 0), width=1)
    d.polygon([(gx, ys[0] + 84), (gx - 9, ys[0] + 100), (gx + 9, ys[0] + 100)], outline=(0, 0, 0), fill=(255, 255, 255))
    d.line([(gx, ys[0] + 84), (ax + 26, ys[0] + 84)], fill=(0, 0, 0), width=1)

    # atores secundarios (direita)
    ax2 = 2040
    for (nome, ucs), ay in zip(ATORES_SEC, [720, 150]):
        _ator(d, ax2, ay, nome, fn)
        for uid in ucs:
            cx, cy = pos[uid]
            p1 = _borda_elipse(cx, cy, EW, EH, (ax2 - 26, ay + 40))
            d.line([(ax2 - 26, ay + 40), p1], fill=(110, 110, 110), width=1)

    # legenda
    lx2, ly = rx, sy1 - 150
    d.rectangle([lx2, ly, lx2 + rw, ly + 120], outline=(0, 0, 0), width=1)
    d.text((lx2 + 10, ly + 6), "Legenda", font=fb, fill=(0, 0, 0))
    d.line([lx2 + 10, ly + 42, lx2 + 70, ly + 42], fill=(110, 110, 110), width=1)
    d.text((lx2 + 78, ly + 34), "associação ator – caso de uso", font=fn, fill=(0, 0, 0))
    _seta_tracejada(d, (lx2 + 10, ly + 72), (lx2 + 70, ly + 72), "", fi)
    d.text((lx2 + 78, ly + 64), "«include» / «extend»", font=fn, fill=(0, 0, 0))
    d.text((lx2 + 10, ly + 94), "triângulo vazio: generalização (perfis herdam os casos de uso do usuário autenticado)", font=fi, fill=(0, 0, 0))

    img.save(caminho, optimize=True)
    return caminho


if __name__ == "__main__":
    renderizar("casos_de_uso.png")
