# -*- coding: utf-8 -*-
"""Figura da arquitetura de alto nível (preto e branco) desenhada com PIL."""
from PIL import Image, ImageDraw, ImageFont

FONTE = "C:/Windows/Fonts/arial.ttf"
FONTE_B = "C:/Windows/Fonts/arialbd.ttf"


def _caixa(d, x, y, w, h, titulo, sub, fb, fn):
    d.rectangle([x, y, x + w, y + h], outline=(0, 0, 0), width=2, fill=(245, 245, 245))
    d.text((x + w / 2 - fb.getlength(titulo) / 2, y + 12), titulo, font=fb, fill=(0, 0, 0))
    for i, s in enumerate(sub):
        d.text((x + w / 2 - fn.getlength(s) / 2, y + 40 + i * 18), s, font=fn, fill=(0, 0, 0))


def _seta(d, p0, p1, rotulo, fn, acima=True):
    d.line([p0, p1], fill=(0, 0, 0), width=2)
    x1, y1 = p1
    x0, y0 = p0
    import math
    ang = math.atan2(y1 - y0, x1 - x0)
    d.polygon([(x1, y1), (x1 - 12 * math.cos(ang - 0.4), y1 - 12 * math.sin(ang - 0.4)), (x1 - 12 * math.cos(ang + 0.4), y1 - 12 * math.sin(ang + 0.4))], fill=(0, 0, 0))
    if rotulo:
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        tw = fn.getlength(rotulo)
        d.text((mx - tw / 2, my - 20 if acima else my + 6), rotulo, font=fn, fill=(0, 0, 0))


def renderizar(caminho):
    fn, fb = ImageFont.truetype(FONTE, 14), ImageFont.truetype(FONTE_B, 16)
    img = Image.new("RGB", (1500, 470), (255, 255, 255))
    d = ImageDraw.Draw(img)
    _caixa(d, 40, 150, 300, 100, "Frontend web", ["Next.js (Vercel)", "dashboard, transcrições, alertas, usuários"], fb, fn)
    _caixa(d, 40, 300, 300, 100, "Aplicativo Android", ["Java (Retrofit)", "dashboard, reuniões, alertas, insights"], fb, fn)
    _caixa(d, 560, 150, 320, 100, "Backend", ["Java 17 + Spring Boot (Render)", "API REST, autenticação, fila de análise, cache"], fb, fn)
    _caixa(d, 1120, 150, 340, 100, "LeanIA", ["Python + FastAPI (Hugging Face Spaces)", "sentimento, produtos, riscos, oportunidades"], fb, fn)
    _caixa(d, 560, 330, 320, 100, "Oracle Database 19c", ["Instância da FIAP", "20 tabelas (usuários, transcrições, análises, alertas)"], fb, fn)
    _seta(d, (340, 190), (560, 190), "REST + JWT", fn)
    _seta(d, (340, 340), (560, 240), "REST + JWT", fn, acima=False)
    _seta(d, (880, 200), (1120, 200), "POST /analisar (token interno)", fn)
    _seta(d, (1120, 220), (880, 220), "", fn)
    d.text((910, 226), "análise (JSON)", font=fn, fill=(0, 0, 0))
    _seta(d, (720, 250), (720, 330), "JPA / Hibernate", fn)
    d.text((730, 280), "JPA / Hibernate", font=fn, fill=(0, 0, 0))
    d.text((40, 30), "Arquitetura de alto nível", font=ImageFont.truetype(FONTE_B, 20), fill=(0, 0, 0))
    d.text((40, 60), "Três serviços independentes e um banco relacional; toda requisição carrega um identificador (X-Request-ID) para rastreabilidade ponta a ponta.", font=fn, fill=(0, 0, 0))
    img.save(caminho, optimize=True)
    return caminho
