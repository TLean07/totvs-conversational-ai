# -*- coding: utf-8 -*-
"""
Pitch da Sprint 3 (Company 404) mantendo a identidade visual do deck original do Canva:
fundo preto com fumaca azul, titulos em Open Sans Bold / Garet Heavy, caixas azul-marinho com
borda branca, logo no canto superior direito e selo "By" no canto inferior direito.
Cada slide tem 1440x810 px (mesma medida do PDF original) e o PDF sai pelo Edge headless.
"""
import os
import subprocess

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.abspath(os.path.join(AQUI, ".."))
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

# retangulos dos fundos no PDF original (left, top, width, height)
FUNDOS = {
    "capa": ("fundo_capa.jpeg", -104, -177, 1653, 1102),
    "p2": ("fundo_p2.jpeg", -514, -31, 1954, 1304),
    "p3": ("fundo_p3.jpeg", -741, -494, 2181, 1455),
    "p4": ("fundo_p4.jpeg", -741, -151, 2181, 1455),
    "p5": ("fundo_p5.jpeg", 0, -502, 2278, 1520),
    "p6": ("fundo_p6.jpeg", 0, -151, 2181, 1455),
    "p8": ("fundo_p8.jpeg", -266, -494, 1955, 1304),
}

CSS = """
@font-face { font-family: 'Garet'; src: url('assets/fonts/Garet-Book.ttf'); font-weight: 400; }
@font-face { font-family: 'Garet'; src: url('assets/fonts/Garet-Heavy.ttf'); font-weight: 800; }
@font-face { font-family: 'Open Sans'; src: url('assets/fonts/OpenSans.ttf'); font-weight: 300 800; }
@page { size: 1440px 810px; margin: 0; }
* { box-sizing: border-box; margin: 0; padding: 0; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
html, body { background: #000; }
.slide { position: relative; width: 1440px; height: 810px; overflow: hidden; background: #000; page-break-after: always; color: #fff; font-family: 'Open Sans', Arial, sans-serif; }
.slide:last-child { page-break-after: auto; }
.bg { position: absolute; z-index: 0; }
.z { position: absolute; z-index: 2; }
.logo { position: absolute; left: 1306px; top: 0; width: 105px; height: 129px; z-index: 3; }
.selo { position: absolute; left: 1255px; top: 749px; width: 159px; height: 46px; border: 3px solid #fff; border-radius: 23px; background: #033A7F;
        font-family: 'Garet'; font-weight: 400; font-size: 15px; color: #86b9f5; display: flex; align-items: center; justify-content: center; z-index: 3; white-space: nowrap; }
.titulo { position: absolute; left: 93px; top: 2px; font-weight: 700; font-size: 92px; line-height: 125px; letter-spacing: -0.5px; z-index: 2; white-space: nowrap; }
.titulo small { font-size: 47px; }
.garet { font-family: 'Garet'; font-weight: 800; background: linear-gradient(180deg, #ffffff 0%, #f1f1f1 45%, #bdbdbd 100%);
         -webkit-background-clip: text; background-clip: text; color: transparent; text-shadow: none; }
.caixa { position: absolute; background: #033A7F; border: 3.5px solid #EFEFEF; border-radius: 30px; z-index: 2; }
.escura { position: absolute; background: #02193E; border: 3.5px solid #ffffff; border-radius: 30px; z-index: 2; }
.arial { font-family: Arial, Helvetica, sans-serif; }
.centro { text-align: center; }
.sub { font-size: 22px; line-height: 1.35; }
.hr { height: 1px; background: rgba(255,255,255,.85); margin: 12px 0 14px; }
.num { font-weight: 700; }
.fonte { position: absolute; font-size: 13px; color: #9fb8d8; z-index: 2; }
/* dispositivos */
.monitor { position: absolute; z-index: 3; }
.monitor .tela { background: #e9e9e9; border-radius: 12px; padding: 10px 10px 34px; box-shadow: 0 10px 30px rgba(0,0,0,.35); }
.monitor .tela img { display: block; width: 100%; border-radius: 2px; }
.monitor .pe { width: 26%; height: 44px; margin: 0 auto; background: linear-gradient(#cfcfcf, #bbbbbb); clip-path: polygon(22% 0, 78% 0, 90% 100%, 10% 100%); }
.monitor .base { width: 40%; height: 8px; margin: 0 auto; background: #cfcfcf; border-radius: 6px; }
.laptop { position: absolute; z-index: 3; }
.laptop .tela { background: #0b0b0b; border-radius: 12px 12px 0 0; padding: 12px 12px 14px; border: 2px solid #3a3a3a; }
.laptop .tela img { display: block; width: 100%; }
.laptop .base { height: 14px; margin: 0 -34px; background: linear-gradient(#e6e6e6, #b9b9b9); border-radius: 0 0 14px 14px; }
.celular { position: absolute; z-index: 3; background: #000; border: 3px solid #555; border-radius: 26px; padding: 9px 7px; box-shadow: 0 12px 30px rgba(0,0,0,.45); }
.celular img { display: block; width: 100%; border-radius: 16px; }
.legenda { position: absolute; z-index: 3; font-size: 21px; text-align: center; }
.chip { display: inline-block; border: 2px solid rgba(255,255,255,.8); border-radius: 18px; padding: 4px 14px; margin: 4px 6px 4px 0; font-size: 15px; }
.seta { position: absolute; z-index: 2; }
"""


_GRAD = [0]


def garet(texto, x, base, tamanho, largura):
    """Titulo em Garet Heavy com degrade branco -> cinza, desenhado em SVG (base = linha de base do texto)."""
    _GRAD[0] += 1
    gid = "g%d" % _GRAD[0]
    altura = int(tamanho * 1.3)
    topo = base - int(tamanho * 0.98)
    return ('<svg class="z" style="left:%dpx;top:%dpx;overflow:visible" width="%d" height="%d">'
            '<defs><linearGradient id="%s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffffff"/>'
            '<stop offset="0.5" stop-color="#f0f0f0"/><stop offset="1" stop-color="#b9b9b9"/></linearGradient></defs>'
            '<text x="0" y="%d" font-family="Garet" font-weight="800" font-size="%d" fill="url(#%s)">%s</text></svg>') % (
        x, topo, largura, altura, gid, int(tamanho * 0.98), tamanho, gid, texto)


def fundo(chave):
    arq, l, t, w, h = FUNDOS[chave]
    return '<img class="bg" src="assets/%s" style="left:%dpx;top:%dpx;width:%dpx;height:%dpx">' % (arq, l, t, w, h)


def moldura(conteudo, chave="p3", logo=True):
    return '<section class="slide">%s%s%s<div class="selo">By Company 404</div></section>' % (
        fundo(chave), conteudo, '<img class="logo" src="assets/logo_alpha.png">' if logo else "")


def seta_h(x, y, w, cor="#ffffff"):
    return ('<svg class="seta" style="left:%dpx;top:%dpx" width="%d" height="30" viewBox="0 0 %d 30">'
            '<line x1="0" y1="15" x2="%d" y2="15" stroke="%s" stroke-width="4"/>'
            '<polygon points="%d,4 %d,15 %d,26" fill="%s"/></svg>') % (x, y, w, w, w - 14, cor, w - 16, w, w - 16, cor)


def seta_v(x, y, h, cor="#ffffff"):
    return ('<svg class="seta" style="left:%dpx;top:%dpx" width="30" height="%d" viewBox="0 0 30 %d">'
            '<line x1="15" y1="0" x2="15" y2="%d" stroke="%s" stroke-width="4"/>'
            '<polygon points="4,%d 15,%d 26,%d" fill="%s"/></svg>') % (x, y, h, h, h - 12, cor, h - 14, h, h - 14, cor)


# ------------------------------------------------------------------------------------------ slides
def s01_capa():
    return ('<section class="slide">%s'
            '<img class="z" src="assets/logo_alpha.png" style="left:23px;top:-22px;width:238px;height:291px">'
            '%s'
            '<div class="z" style="left:438px;top:228px;font-size:28px;font-weight:700">Conversational Intelligence</div>'
            '<div class="z" style="left:440px;top:282px;font-size:20px;color:#cfe0f7;width:470px;line-height:1.4">'
            'Inteligência comercial a partir das reuniões de Vendas e Customer Success</div>'
            '<div class="selo">By Company 404</div></section>') % (fundo("capa"), garet("TOTVS", 250, 202, 148, 560))


def s02_totvs():
    c = (garet("A TOTVS", 81, 152, 114, 560) +
         '<div class="z arial" style="left:273px;top:238px;width:880px;font-size:30px;line-height:1.35">'
         'A maior empresa de software de gestão do Brasil, com <b>mais de 70 mil clientes</b> em todos os segmentos da economia.</div>'
         '<div class="z arial" style="left:273px;top:352px;width:880px;font-size:23px;line-height:1.4;color:#e3ecf8">'
         'Suas equipes de Vendas e Customer Success conduzem milhares de reuniões por dia. Cada conversa traz sinais de compra, '
         'de insatisfação e de concorrência que hoje ficam presos em transcrições.</div>')
    cards = [("+70 mil", "clientes ativos"), ("Milhares", "de reuniões por dia"), ("1 conversa", "= dados de venda e retenção")]
    for i, (n, t) in enumerate(cards):
        x = 273 + i * 300
        c += ('<div class="escura" style="left:%dpx;top:520px;width:270px;height:150px;padding:22px 20px">'
              '<div style="font-size:40px;font-weight:700;line-height:1.1">%s</div><div style="font-size:19px;margin-top:8px;color:#d6e3f5">%s</div></div>') % (x, n, t)
    c += '<div class="fonte" style="left:273px;top:690px">Fonte: TOTVS (totvs.com/sobre)</div>'
    return moldura(c, "p2")


def s03_problema():
    blocos = [
        (61, 181, 169, 224, 203, 288, "1", "Identificação Tardia de Riscos",
         "Sem análise automatizada, sinais sutis de insatisfação passam despercebidos. A empresa só percebe o risco de cancelamento quando o cliente já declara o descontentamento."),
        (722, 381, 828, 424, 864, 489, "2", "Perda de Oportunidades de Vendas",
         "Gatilhos de compra, módulos de interesse e orçamento ficam “ocultos” em textos não estruturados e não viram proposta."),
        (88, 552, 193, 595, 231, 660, "3", "Falta de Visibilidade Gerencial",
         "A liderança recebe o que aconteceu nas reuniões com dias ou semanas de atraso, tarde demais para agir."),
    ]
    c = '<div class="titulo" style="left:101px;top:28px;font-size:107px;line-height:146px">O Problema</div>'
    for nx, ny, tx, ty, px, py, n, t, p in blocos:
        c += '<div class="z" style="left:%dpx;top:%dpx;font-size:130px;font-weight:700;line-height:176px">%s</div>' % (nx, ny - 8, n)
        c += '<div class="z" style="left:%dpx;top:%dpx;font-size:28.6px;font-weight:700;white-space:nowrap">%s</div>' % (tx, ty - 6, t)
        c += '<div class="z arial" style="left:%dpx;top:%dpx;width:520px;font-size:18px;line-height:24.5px">%s</div>' % (px, py - 4, p)
    # setas curvas como no original
    c += ('<svg class="seta" style="left:760px;top:240px" width="180" height="140" viewBox="0 0 180 140">'
          '<path d="M20,20 C120,10 160,60 140,120" fill="none" stroke="#1c4f98" stroke-width="7" stroke-linecap="round"/>'
          '<polygon points="122,108 140,134 156,106" fill="#1c4f98"/></svg>')
    c += ('<svg class="seta" style="left:780px;top:600px" width="160" height="150" viewBox="0 0 160 150">'
          '<path d="M120,14 C150,90 110,130 30,126" fill="none" stroke="#1c4f98" stroke-width="7" stroke-linecap="round"/>'
          '<polygon points="42,110 14,126 42,142" fill="#1c4f98"/></svg>')
    c += ('<div class="caixa" style="left:960px;top:620px;width:400px;height:112px;padding:16px 24px;font-size:18px;line-height:1.4">'
          '<b>Resultado:</b> receita que escapa e clientes que saem sem que a liderança tenha tido a chance de agir.</div>')
    return moldura(c, "p3")


def s04_proposta():
    c = ('<div class="titulo" style="left:40px;top:29px;font-size:110px;line-height:150px">Proposta de Valor</div>'
         '<div class="z arial" style="left:134px;top:196px;width:840px;font-size:23px;line-height:30.5px">'
         'Transformar cada interação com o cliente em inteligência de negócios, eliminando a análise manual e '
         'capacitando a liderança comercial a agir de forma preditiva na retenção de contas e na expansão de receita.</div>')
    caixas = [
        (134, 339, "Agilidade<br>Estratégica", "Do fim da reunião ao alerta em minutos, não em dias. A liderança não depende de relatórios manuais para agir."),
        (510, 401, "Atuação Preditiva &amp;<br>Foco na Retenção", "Sentimento, insatisfação e concorrentes citados são mapeados para neutralizar o risco de cancelamento antes que ele se concretize."),
        (885, 465, "Maximização do Valor<br>do Cliente", "Gatilhos de compra, módulos de interesse e orçamento viram oportunidades claras de upsell e cross-sell, com a próxima ação sugerida."),
    ]
    for x, y, t, p in caixas:
        c += ('<div class="escura" style="left:%dpx;top:%dpx;width:346px;height:265px;padding:18px 26px">'
              '<div class="centro" style="font-size:19px;line-height:26px">%s</div><div class="hr"></div>'
              '<div class="arial" style="font-size:15.6px;line-height:21px">%s</div></div>') % (x, y, t, p)
    return moldura(c, "p4")


def s05_solucao():
    feats_e = [("Detecção Preditiva", "Churn e insatisfação antes do cancelamento"), ("Análise de Sentimento", "Geral e por área, com negação e intensidade"),
               ("Qualificação Automática", "Persona do interlocutor, budget e próxima ação"), ("Inteligência Competitiva", "Concorrentes citados e em negociação")]
    feats_d = [("Alertas em Tempo Real", "Priorizados, com ciclo novo, visto e resolvido"), ("Feedback Adaptativo", "O usuário avalia e a LeanIA aprende"),
               ("Evolução Contínua", "Regras e catálogo TOTVS refinados com dados reais"), ("Flexibilidade de Ingestão", "Texto, JSON e CSV da TOTVS em lote, ou áudio")]

    def lista(feats):
        return "".join('<div style="margin:0 0 14px 0"><div style="font-size:25px;font-weight:700;line-height:1.15">&bull; %s</div>'
                       '<div style="font-size:16px;color:#d6e3f5;margin-left:18px">%s</div></div>' % (t, d) for t, d in feats)
    c = ('<img class="z" src="assets/icone_p5_alpha.png" style="left:-4px;top:-6px;width:174px;height:174px">'
         '<div class="z" style="left:175px;top:40px;font-size:56px;font-weight:700;line-height:80px">TOTVS Conversational Intelligence</div>'
         '<div class="caixa" style="left:106px;top:161px;width:1193px;height:568px"></div>'
         '<div class="z centro" style="left:130px;top:178px;width:1145px;font-size:22px">Para solucionar os gargalos na extração de dados comerciais, apresentamos:</div>'
         '<div class="z" style="left:450px;top:212px;display:flex;align-items:center;gap:14px"><img src="assets/icone_p5_alpha.png" style="width:56px;height:56px">'
         '<span style="font-size:24px;font-weight:700">TOTVS Conversational Intelligence</span></div>'
         '<div class="z centro" style="left:190px;top:274px;width:1025px;font-size:17.5px;line-height:1.45">'
         'A solução processa as reuniões comerciais (texto, arquivos JSON e CSV da TOTVS ou áudio) e extrai inteligência acionável '
         'automaticamente, sem análise manual das transcrições e sem depender de API paga de IA.</div>'
         '<div class="escura" style="left:135px;top:362px;width:540px;height:340px;padding:26px 34px">%s</div>'
         '<div class="escura" style="left:731px;top:362px;width:540px;height:340px;padding:26px 34px">%s</div>') % (lista(feats_e), lista(feats_d))
    return moldura(c, "p5")


def s06_como_funciona():
    etapas = [("1", "Reunião", "Texto digitado, arquivo JSON ou CSV da TOTVS (em lote) ou áudio da reunião."),
              ("2", "LeanIA analisa", "Sentimento, produtos TOTVS, concorrentes, oportunidades, riscos, persona e budget."),
              ("3", "Oracle guarda", "Análises, alertas e histórico em banco relacional corporativo (Oracle 19c)."),
              ("4", "Time age", "Dashboard web e Android, alertas priorizados e próxima ação sugerida.")]
    c = '<div class="titulo">Como Funciona</div>'
    for i, (n, t, d) in enumerate(etapas):
        x = 60 + i * 335
        c += ('<div class="escura" style="left:%dpx;top:175px;width:290px;height:250px;padding:20px 22px">'
              '<div style="display:flex;align-items:center;gap:12px"><div style="width:46px;height:46px;border-radius:50%%;background:#033A7F;border:2px solid #fff;'
              'display:flex;align-items:center;justify-content:center;font-weight:700;font-size:24px">%s</div>'
              '<div style="font-size:26px;font-weight:700">%s</div></div><div class="hr"></div>'
              '<div style="font-size:17px;line-height:1.45">%s</div></div>') % (x, n, t, d)
        if i < 3:
            c += seta_h(x + 296, 285, 36)
    difs = [("Sem custo de API", "Motor de IA próprio (LeanIA): nenhum dado de cliente é enviado a serviços pagos de terceiros."),
            ("Conhecimento TOTVS embutido", "Protheus, RM, Fluig, Datasul, Techfin e os concorrentes do mercado brasileiro de ERP."),
            ("Já em produção", "Next.js (Vercel), Spring Boot (Render), LeanIA em Python (Hugging Face), Oracle 19c e app Android.")]
    c += '<div class="caixa" style="left:60px;top:462px;width:1240px;height:260px"></div>'
    c += '<div class="z" style="left:96px;top:480px;font-size:28px;font-weight:700">Diferenciais</div>'
    for i, (t, d) in enumerate(difs):
        x = 96 + i * 405
        c += ('<div class="z" style="left:%dpx;top:535px;width:370px"><div style="font-size:22px;font-weight:700">%s</div>'
              '<div style="font-size:17px;line-height:1.45;margin-top:8px;color:#e3ecf8">%s</div></div>') % (x, t, d)
    return moldura(c, "p6")


def s07_na_pratica():
    c = ('<div class="titulo">Na Prática</div>'
         '<div class="z" style="left:97px;top:122px;font-size:21px;color:#d6e3f5">Reunião real da base anonimizada da TOTVS (cliente do segmento Saúde)</div>')
    cards = [
        (175, "O cliente disse", '<i>“Hoje, por N motivos, ele está com alguns problemas e a gente pensou, então, em trocar.”</i><br>'
                                 '<i>“Eu não posso trazer um portal que gere prejuízos para a operação.”</i>'),
        (375, "A LeanIA identificou", '<b>Oportunidade:</b> TOTVS Fluig (Workflow e Documentos), 90%<br><b>Risco:</b> insatisfação com processos (médio)<br>'
                                      '<b>Interlocutor:</b> Gestor Operacional &middot; <b>Budget:</b> R$ 10.000,00'),
        (575, "Próxima ação sugerida", 'Enviar proposta ou agendar demonstração com os decisores nos próximos dias.'),
    ]
    for y, t, p in cards:
        c += ('<div class="escura" style="left:60px;top:%dpx;width:660px;height:170px;padding:16px 26px">'
              '<div style="font-size:23px;font-weight:700">%s</div><div class="hr" style="margin:8px 0 10px"></div>'
              '<div style="font-size:17px;line-height:1.5">%s</div></div>') % (y, t, p)
    c += seta_v(376, 347, 28) + seta_v(376, 547, 28)
    c += ('<div class="monitor" style="left:770px;top:180px;width:600px"><div class="tela"><img src="assets/web/analise_resumo.png"></div>'
          '<div class="pe"></div><div class="base"></div></div>'
          '<div class="legenda" style="left:770px;top:712px;width:600px">Detalhe da análise na plataforma</div>')
    return moldura(c, "p4")


def s08_resultados():
    c = ('<div class="titulo">Resultados Reais</div>'
         '<div class="z" style="left:97px;top:122px;font-size:21px;color:#d6e3f5">Base anonimizada da TOTVS importada e analisada em produção, com os dados no Oracle Database 19c</div>'
         '<div class="caixa" style="left:60px;top:175px;width:1320px;height:170px;display:flex;align-items:center;padding:0 44px;gap:40px">'
         '<div><div style="font-size:78px;font-weight:700;line-height:1">623</div><div style="font-size:20px">reuniões reais analisadas</div></div>'
         '<div style="font-size:20px;line-height:1.5;max-width:760px">Importadas do arquivo da TOTVS (1.174 registros) sem digitação manual, '
         '<b>100% processadas</b> pela LeanIA. Dashboard e listas respondendo em <b>menos de 1 segundo</b> com cache.</div></div>')
    stats = [("960", "Oportunidades", "671 novo produto &middot; 289 upsell"), ("579", "Riscos", "325 insatisfação &middot; 150 churn<br>63 concorrência &middot; 41 perda de confiança"),
             ("244", "Alertas", "consolidados para a liderança")]
    for i, (n, t, d) in enumerate(stats):
        x = 60 + i * 300
        c += ('<div class="escura" style="left:%dpx;top:375px;width:280px;height:330px;padding:24px 24px">'
              '<div style="font-size:64px;font-weight:700;line-height:1">%s</div><div style="font-size:24px;font-weight:700;margin-top:8px">%s</div>'
              '<div class="hr"></div><div style="font-size:17px;line-height:1.5;color:#d6e3f5">%s</div></div>') % (x, n, t, d)
    # sentimento
    total = 293 + 260 + 54 + 16
    partes = [(293, "Misto", "#f59e0b"), (260, "Negativo", "#f43f5e"), (54, "Neutro", "#94a3b8"), (16, "Positivo", "#10b981")]
    barra = "".join('<div style="width:%.2f%%;background:%s"></div>' % (100.0 * v / total, cor) for v, _, cor in partes)
    leg = "".join('<div style="display:flex;align-items:center;gap:10px;margin:10px 0;font-size:19px"><span style="width:16px;height:16px;border-radius:4px;background:%s;display:inline-block"></span>'
                  '<span style="width:110px">%s</span><b>%d</b><span style="color:#9fb8d8;margin-left:8px">%.0f%%</span></div>' % (cor, nome, v, 100.0 * v / total) for v, nome, cor in partes)
    c += ('<div class="escura" style="left:960px;top:375px;width:420px;height:330px;padding:24px 28px">'
          '<div style="font-size:24px;font-weight:700">Sentimento das reuniões</div>'
          '<div style="display:flex;height:26px;border-radius:13px;overflow:hidden;margin:18px 0 12px">%s</div>%s'
          '<div style="font-size:15px;color:#9fb8d8;margin-top:8px">A maioria das conversas mistura elogio e reclamação: é onde está o risco escondido.</div></div>') % (barra, leg)
    return moldura(c, "p5")


LINK_PLATAFORMA = "https://totvs-conversational-ai-oracle.vercel.app/login"


def link_plataforma(x, y, largura, tamanho=17, rotulo="Acesse a plataforma"):
    """Selo clicavel com o link da aplicacao em producao (o Edge preserva o hiperlink no PDF)."""
    return ('<a class="z" href="%s" style="left:%dpx;top:%dpx;width:%dpx;display:flex;align-items:center;gap:12px;'
            'background:#033A7F;border:3px solid #fff;border-radius:26px;padding:9px 20px;color:#fff;text-decoration:none">'
            '<span style="font-size:%dpx;font-weight:700;white-space:nowrap">%s</span>'
            '<span style="font-size:%dpx;color:#86b9f5;white-space:nowrap;text-decoration:underline">%s</span></a>') % (
        LINK_PLATAFORMA, x, y, largura, tamanho, rotulo, tamanho, LINK_PLATAFORMA.replace("https://", ""))


def s09_web():
    c = ('<div class="titulo">Visibilidade <small>(WEB)</small></div>' + link_plataforma(790, 40, 500, 15, "Acesse:") +
         '<div class="caixa" style="left:22px;top:139px;width:1314px;height:600px"></div>'
         '<div class="escura" style="left:22px;top:139px;width:1314px;height:150px;display:flex;align-items:center;justify-content:center;padding:0 60px">'
         '<div class="centro" style="font-size:25px;line-height:1.45">Métricas essenciais na primeira tela e componentes consistentes: o time de vendas '
         'domina a ferramenta <b>sem curva de aprendizado</b>. As telas abaixo mostram os dados reais em produção.</div></div>')
    c += ('<div class="monitor" style="left:62px;top:330px;width:390px"><div class="tela"><img src="assets/web/02_dashboard.png"></div><div class="pe"></div><div class="base"></div></div>'
          '<div class="legenda" style="left:62px;top:690px;width:390px">Dashboard executivo</div>'
          '<div class="laptop" style="left:505px;top:352px;width:350px"><div class="tela"><img src="assets/web/07_oportunidades.png"></div><div class="base"></div></div>'
          '<div class="legenda" style="left:485px;top:690px;width:390px">Oportunidades (960)</div>'
          '<div class="monitor" style="left:908px;top:330px;width:390px"><div class="tela"><img src="assets/web/06_riscos.png"></div><div class="pe"></div><div class="base"></div></div>'
          '<div class="legenda" style="left:908px;top:690px;width:390px">Riscos (579)</div>')
    return moldura(c, "p6")


def s10_mobile():
    c = ('<div class="titulo">Visibilidade <small>(Mobile)</small></div>'
         '<div class="caixa" style="left:22px;top:139px;width:1314px;height:600px"></div>'
         '<div class="escura" style="left:805px;top:139px;width:531px;height:600px;padding:40px 36px">'
         '<div class="centro" style="font-size:25px;font-weight:700;line-height:1.4">Não é só um reflexo do sistema web: é um co-piloto de bolso.</div>'
         '<div style="margin-top:40px;font-size:21px;line-height:1.45">'
         '<div style="margin-bottom:26px">&bull; Painel com os mesmos números da web: 623 reuniões, 960 oportunidades, 579 riscos.</div>'
         '<div style="margin-bottom:26px">&bull; Oportunidades e riscos com a evidência da conversa e a ação recomendada.</div>'
         '<div>&bull; Alertas com filtro por prioridade para agir entre uma reunião e outra.</div></div></div>')
    telas = ["Screenshot_20260913_155216_TOTVS_Mobile.png", "Screenshot_20260913_155227_TOTVS_Mobile.png",
             "Screenshot_20260913_155237_TOTVS_Mobile.png", "Screenshot_20260913_155249_TOTVS_Mobile.png"]
    nomes = ["Painel", "Riscos e oportunidades", "Oportunidades", "Alertas"]
    for i, t in enumerate(telas):
        x = 42 + i * 190
        c += '<div class="celular" style="left:%dpx;top:190px;width:180px"><img src="assets/mobile/%s"></div>' % (x, t)
        c += '<div class="legenda" style="left:%dpx;top:600px;width:180px;font-size:17px;line-height:1.3">%s</div>' % (x, nomes[i])
    return moldura(c, "p6")


def s11_proximos():
    itens = [("Modelo supervisionado na LeanIA", "Classificador treinado com as reuniões reais (Data Science), comparado às regras atuais."),
             ("Filtros no dashboard", "Período, cliente e segmento para gestores e analistas de BI."),
             ("Escala garantida", "Teste de carga da importação em lote antes de cada deploy."),
             ("Alertas em linguagem de negócio", "Rótulos claros no lugar dos códigos técnicos de tipo."),
             ("MVP em vídeo e visão de negócio", "Demonstração do fluxo completo e TCO do produto.")]
    c = ('<div class="titulo">Próximos Passos</div>'
         '<div class="z" style="left:97px;top:122px;font-size:21px;color:#d6e3f5">Sprint 4 &middot; entrega em 14/10/2026</div>')
    for i, (t, d) in enumerate(itens):
        col, lin = i % 3, i // 3
        x = 60 + col * 440 + (220 if lin == 1 else 0)
        y = 185 + lin * 270
        c += ('<div class="escura" style="left:%dpx;top:%dpx;width:410px;height:240px;padding:24px 28px">'
              '<div style="display:flex;align-items:center;gap:14px"><div style="min-width:44px;height:44px;border-radius:50%%;background:#033A7F;border:2px solid #fff;'
              'display:flex;align-items:center;justify-content:center;font-weight:700;font-size:22px">%d</div>'
              '<div style="font-size:23px;font-weight:700;line-height:1.2">%s</div></div><div class="hr"></div>'
              '<div style="font-size:18px;line-height:1.5">%s</div></div>') % (x, y, i + 1, t, d)
    return moldura(c, "p3")


def s12_equipe():
    membros = [("Bruno Gama", "Masseiras", "562526"), ("Kauai Rosa de", "Assis Rocha", "563256"), ("Lucas Gilmar", "Pedro da Silva", "566231"),
               ("Leandro Afonso", "Silva Santos Junior", "561344"), ("Murilo da", "Silva Basto", "565445")]
    c = ('<div class="titulo">Nossa Equipe</div>'
         '<div class="z" style="left:97px;top:122px;font-size:21px;color:#d6e3f5">Company 404 &middot; Engenharia de Software &middot; FIAP &middot; Challenge TOTVS 2026</div>')
    for i, (n1, n2, rm) in enumerate(membros):
        x = 60 + i * 268
        iniciais = n1[0] + n2.split()[-1][0] if not n1.startswith("Kauai") else "KR"
        if n1.startswith("Leandro"):
            iniciais = "LA"
        c += ('<div class="escura" style="left:%dpx;top:215px;width:248px;height:430px;padding:40px 18px;text-align:center">'
              '<div style="width:132px;height:152px;margin:0 auto;background:#fff;clip-path:polygon(50%% 0,100%% 24%%,100%% 76%%,50%% 100%%,0 76%%,0 24%%);'
              'display:flex;align-items:center;justify-content:center;font-family:Garet;font-weight:800;font-size:46px;color:#02193E">%s</div>'
              '<div style="height:96px;display:flex;align-items:center;justify-content:center;font-size:23px;font-weight:700;line-height:1.25;margin-top:26px">%s<br>%s</div>'
              '<div class="hr" style="margin:14px 20px"></div>'
              '<div style="font-size:20px;color:#86b9f5;font-family:Garet">RM %s</div></div>') % (x, iniciais, n1, n2, rm)
    return moldura(c, "p2")


def s13_obrigado():
    c = ('<section class="slide">%s'
         '%s%s%s%s'
         '<img class="logo" src="assets/logo_alpha.png"><div class="selo">By Company 404</div></section>') % (
        fundo("p8"), garet("Obrigado", 33, 270, 151, 760), garet("Pela", 476, 450, 151, 360), garet("Atenção", 566, 656, 151, 680),
        link_plataforma(40, 728, 700, 19))
    return c


SLIDES = [s01_capa, s02_totvs, s03_problema, s04_proposta, s05_solucao, s06_como_funciona, s07_na_pratica,
          s08_resultados, s09_web, s10_mobile, s11_proximos, s12_equipe, s13_obrigado]


def gerar():
    html = "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='utf-8'><title>TOTVS Conversational Intelligence - Pitch Sprint 3</title><style>%s</style></head><body>%s</body></html>" % (
        CSS, "".join(s() for s in SLIDES))
    caminho_html = os.path.join(RAIZ, "Company404_Pitch_Sprint3.html")
    with open(caminho_html, "w", encoding="utf-8") as f:
        f.write(html)
    caminho_pdf = os.path.join(RAIZ, "Company404_Pitch_Sprint3.pdf")
    subprocess.run([EDGE, "--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--print-to-pdf=%s" % caminho_pdf,
                    "--no-first-run", "--disable-extensions", "--virtual-time-budget=8000",
                    "--user-data-dir=%s" % os.path.join(os.environ.get("TEMP", "."), "edge-pdf-profile"),
                    "file:///" + caminho_html.replace("\\", "/")], check=True, timeout=240, capture_output=True)
    return caminho_pdf


if __name__ == "__main__":
    print(gerar())
