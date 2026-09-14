# -*- coding: utf-8 -*-
"""Diagrama de classes UML do projeto Java (preto e branco, alta resolução), desenhado com PIL."""
from PIL import Image, ImageDraw, ImageFont

FONTE = "C:/Windows/Fonts/consola.ttf"
FONTE_B = "C:/Windows/Fonts/arialbd.ttf"
FONTE_I = "C:/Windows/Fonts/ariali.ttf"

# nome, estereotipo, atributos, metodos  (visibilidade UML: - privado, + publico, # protegido)
CLASSES = {
    "Transcricao": ("", [
        "- id: String", "- ownerId: String", "- textoOriginal: String", "- participantes: String", "- dataReuniao: LocalDateTime",
        "- duracaoMinutos: Integer", "- cliente: String", "- vendedor: String", "- origemTranscricao: String", "- meetingId: String",
        "- uf: String", "- nomeSegmento: String", "- notaNps: Integer", "- status: StatusTranscricao", "- dataCriacao: LocalDateTime",
        "- dataProcessamento: LocalDateTime", "- analise: Analise"],
        ["+ validar(): void", "+ iniciarProcessamento(): void", "+ concluirProcessamento(Analise): void", "+ registrarErro(): void",
         "+ isProcessada(): boolean", "+ resumoCurto(int): String", "+ getters / setters"]),
    "Analise": ("", [
        "- id: String", "- ownerId: String", "- transcricaoId: String", "- sentimentoGeral: Sentimento", "- resumo: String",
        "- personaIdentificada: String", "- budgetIdentificado: String", "- scoreConfiancaVendedor: double", "- scoreSentimento: double",
        "- proximaAcao: String", "- dataAnalise: LocalDateTime", "- produtos: List<Produto>", "- insights: List<Insight>"],
        ["+ getOportunidades(): List<Oportunidade>", "+ getRiscos(): List<Risco>", "+ getConcorrentes(): List<Produto>",
         "+ calcularScoreRisco(): double", "+ definirProximaAcao(): String", "+ gerarResumo(): String", "+ getters / setters"]),
    "Insight": ("«abstract»", ["- descricao: String", "- evidencia: String"],
                ["# Insight(String, String)", "+ getCategoria(): String «abstract»", "+ calcularPeso(): double «abstract»", "+ getters / setters"]),
    "Oportunidade": ("", ["- tipo: TipoOportunidade", "- produtoRelacionado: String", "- score: double", "- valorEstimado: String"],
                     ["+ getCategoria(): String", "+ calcularPeso(): double", "+ isAlta(): boolean"]),
    "Risco": ("", ["- tipo: TipoRisco", "- severidade: Severidade", "- concorrenteMencionado: String"],
              ["+ getCategoria(): String", "+ calcularPeso(): double"]),
    "Produto": ("", ["- nome: String", "- tipo: TipoProduto", "- contexto: String"], ["+ isConcorrente(): boolean", "+ getters / setters"]),
    "Alerta": ("", ["- id: String", "- ownerId: String", "- analiseId: String", "- tipo: String", "- titulo: String", "- categoria: String",
                    "- mensagem: String", "- prioridade: Severidade", "- status: StatusAlerta", "- dataCriacao: LocalDateTime"],
               ["+ marcarVisto(): void", "+ resolver(): void", "+ isPendente(): boolean", "+ isAltaPrioridade(): boolean", "+ getters / setters"]),
    "Usuario": ("", ["- id: String", "- email: String", "- nome: String", "- senhaHash: String", "- perfil: Perfil", "- ativo: boolean", "- criadoEm: LocalDateTime"],
                ["+ validar(): void", "+ podeAcessar(Transcricao): boolean", "+ isAdministrador(): boolean", "+ getters / setters"]),
    "Feedback": ("", ["- id: String", "- ownerId: String", "- transcricaoId: String", "- analiseId: String", "- util: boolean", "- ajuste: String", "- criadoEm: LocalDateTime"],
                 ["+ getters / setters"]),
    "AnalisadorTranscricao": ("«service»", ["- PRODUTOS_TOTVS: Map<String,String>", "- CONCORRENTES: Map<String,String>"],
                              ["+ analisar(Transcricao): Analise", "- identificarProdutos(...)", "- identificarOportunidades(...)", "- identificarRiscos(...)",
                               "- classificarSentimento(...)", "~ identificarPersona(String): String", "~ identificarBudget(String): String",
                               "~ calcularConfiancaVendedor(...): double"]),
    "GeradorAlertas": ("«service»", [], ["+ gerar(Transcricao, Analise): List<Alerta>", "~ prioridadeDoRisco(Risco): Severidade"]),
    "PriorizadorContas": ("«service»", [], ["+ rankear(List<Transcricao>): List<ContaPriorizada>", "+ avaliar(Transcricao): ContaPriorizada"]),
    "CalculadoraIndicadores": ("«service»", [], ["+ calcular(Transcricao): IndicadoresConversa"]),
    "TranscricaoService": ("«service»", ["- transcricaoDAO: TranscricaoDAO", "- analiseDAO: AnaliseDAO", "- alertaDAO: AlertaDAO"],
                           ["+ cadastrar(Transcricao): Transcricao", "+ processar(Transcricao): Analise", "+ excluir(Transcricao): void"]),
    "DAO<T>": ("«interface»", [], ["+ inserir(T): void", "+ buscarPorId(String): Optional<T>", "+ listar(): List<T>", "+ atualizar(T): void", "+ excluir(String): void"]),
    "TranscricaoDAO": ("", [], ["+ listarPorCliente(String): List<Transcricao>", "+ listarPorStatus(StatusTranscricao): List<Transcricao>", "+ contar(): long"]),
    "AnaliseDAO": ("", [], ["+ buscarPorTranscricao(String): Optional<Analise>", "- inserirFilhas(Connection, Analise)", "- carregarFilhas(Connection, Analise)"]),
    "AlertaDAO": ("", [], ["+ inserirTodos(List<Alerta>): void", "+ listarPorStatus(StatusAlerta): List<Alerta>", "+ listarPorAnalise(String): List<Alerta>"]),
    "UsuarioDAO": ("", [], ["+ buscarPorEmail(String): Optional<Usuario>"]),
    "FeedbackDAO": ("", [], ["+ listarPorTranscricao(String): List<Feedback>"]),
    "ConnectionFactory": ("", ["- URL: String", "- USUARIO: String", "- SENHA: String"], ["+ getConnection(): Connection", "+ testarConexao(): boolean"]),
    "Enums": ("«enumeration»", ["Sentimento", "Severidade", "StatusAlerta", "StatusTranscricao", "TipoOportunidade", "TipoProduto", "TipoRisco", "Perfil"], []),
}

# posicoes (x, y) por classe; largura calculada
POS = {
    "Usuario": (40, 60), "Transcricao": (40, 420), "Feedback": (40, 1100),
    "Analise": (520, 420), "Produto": (520, 1000), "Insight": (1100, 60), "Oportunidade": (1000, 320), "Risco": (1270, 320),
    "Alerta": (1000, 640), "Enums": (1000, 1000),
    "AnalisadorTranscricao": (1580, 60), "GeradorAlertas": (1580, 470), "PriorizadorContas": (1580, 620), "CalculadoraIndicadores": (1580, 770),
    "TranscricaoService": (1580, 920),
    "DAO<T>": (2440, 60), "TranscricaoDAO": (2120, 330), "AnaliseDAO": (2120, 520), "AlertaDAO": (2120, 710), "UsuarioDAO": (2120, 900),
    "FeedbackDAO": (2120, 1040), "ConnectionFactory": (2440, 1180),
}

# relacoes: (origem, destino, tipo, rotulo)  tipos: heranca, realiza, assoc, dep, comp
RELACOES = [
    ("Oportunidade", "Insight", "heranca", ""), ("Risco", "Insight", "heranca", ""),
    ("Transcricao", "Analise", "assoc", "0..1"), ("Analise", "Insight", "comp", "0..*"), ("Analise", "Produto", "comp", "0..*"),
    ("Alerta", "Analise", "assoc", "analiseId"), ("Feedback", "Transcricao", "assoc", "transcricaoId"), ("Transcricao", "Usuario", "assoc", "ownerId"),
    ("TranscricaoDAO", "DAO<T>", "realiza", ""), ("AnaliseDAO", "DAO<T>", "realiza", ""), ("AlertaDAO", "DAO<T>", "realiza", ""),
    ("UsuarioDAO", "DAO<T>", "realiza", ""), ("FeedbackDAO", "DAO<T>", "realiza", ""),
    ("AnalisadorTranscricao", "Analise", "dep", "cria"), ("GeradorAlertas", "Alerta", "dep", "cria"),
    ("TranscricaoService", "TranscricaoDAO", "dep", "usa"), ("TranscricaoService", "AnaliseDAO", "dep", "usa"), ("TranscricaoService", "AlertaDAO", "dep", "usa"),
    ("TranscricaoDAO", "ConnectionFactory", "dep", ""), ("AnaliseDAO", "ConnectionFactory", "dep", ""), ("AlertaDAO", "ConnectionFactory", "dep", ""),
]

PACOTES = [("model", 20, 20, 1520, 1340), ("service", 1560, 20, 520, 1340), ("dao / connection", 2100, 20, 640, 1340)]


def _fontes():
    return ImageFont.truetype(FONTE, 13), ImageFont.truetype(FONTE_B, 15), ImageFont.truetype(FONTE_I, 13)


def _caixa(d, nome, x, y, fn, fb, fi):
    est, attrs, mets = CLASSES[nome]
    largura = max([fb.getlength(nome) + 30] + [fn.getlength(t) + 22 for t in attrs + mets] + [200])
    if est:
        largura = max(largura, fi.getlength(est) + 30)
    cab = 24 + (16 if est else 0)
    h_attr = max(1, len(attrs)) * 17 + 8 if attrs or not mets else 10
    h_met = max(1, len(mets)) * 17 + 8 if mets else 10
    h = cab + h_attr + h_met
    d.rectangle([x, y, x + largura, y + h], outline=(0, 0, 0), width=2, fill=(255, 255, 255))
    yy = y + 4
    if est:
        d.text((x + largura / 2 - fi.getlength(est) / 2, yy), est, font=fi, fill=(0, 0, 0))
        yy += 16
    d.text((x + largura / 2 - fb.getlength(nome) / 2, yy), nome, font=fb, fill=(0, 0, 0))
    y1 = y + cab
    d.line([x, y1, x + largura, y1], fill=(0, 0, 0), width=1)
    yy = y1 + 4
    for a in attrs:
        d.text((x + 10, yy), a, font=fn, fill=(0, 0, 0))
        yy += 17
    y2 = y1 + h_attr
    d.line([x, y2, x + largura, y2], fill=(0, 0, 0), width=1)
    yy = y2 + 4
    for m in mets:
        d.text((x + 10, yy), m, font=fn, fill=(0, 0, 0))
        yy += 17
    return (x, y, x + largura, y + h)


def _ancora(box, alvo):
    """Ponto na borda da caixa mais proximo do centro do alvo."""
    x0, y0, x1, y1 = box
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    tx, ty = alvo
    dx, dy = tx - cx, ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    sx = (x1 - x0) / 2 / abs(dx) if dx else float("inf")
    sy = (y1 - y0) / 2 / abs(dy) if dy else float("inf")
    t = min(sx, sy)
    return cx + dx * t, cy + dy * t


def _seta(d, p0, p1, tipo, rotulo, fi):
    import math
    (x0, y0), (x1, y1) = p0, p1
    if tipo in ("dep", "realiza"):
        comp = math.hypot(x1 - x0, y1 - y0)
        n = int(comp // 12)
        for i in range(n + 1):
            a = i / max(n, 1)
            b = min(1.0, a + 6 / comp)
            d.line([(x0 + (x1 - x0) * a, y0 + (y1 - y0) * a), (x0 + (x1 - x0) * b, y0 + (y1 - y0) * b)], fill=(0, 0, 0), width=2)
    else:
        d.line([p0, p1], fill=(0, 0, 0), width=2)
    ang = math.atan2(y1 - y0, x1 - x0)
    if tipo in ("heranca", "realiza"):
        pts = [(x1, y1), (x1 - 18 * math.cos(ang - 0.45), y1 - 18 * math.sin(ang - 0.45)), (x1 - 18 * math.cos(ang + 0.45), y1 - 18 * math.sin(ang + 0.45))]
        d.polygon(pts, outline=(0, 0, 0), fill=(255, 255, 255))
    elif tipo == "comp":
        # losango preenchido na origem (todo)
        pts = [(x0, y0), (x0 + 12 * math.cos(ang - 0.5), y0 + 12 * math.sin(ang - 0.5)), (x0 + 22 * math.cos(ang), y0 + 22 * math.sin(ang)),
               (x0 + 12 * math.cos(ang + 0.5), y0 + 12 * math.sin(ang + 0.5))]
        d.polygon(pts, outline=(0, 0, 0), fill=(0, 0, 0))
        for s in (0.5, -0.5):
            d.line([(x1, y1), (x1 - 12 * math.cos(ang + s), y1 - 12 * math.sin(ang + s))], fill=(0, 0, 0), width=2)
    else:
        for s in (0.5, -0.5):
            d.line([(x1, y1), (x1 - 12 * math.cos(ang + s), y1 - 12 * math.sin(ang + s))], fill=(0, 0, 0), width=2)
    if rotulo:
        mx, my = (x0 + x1) / 2, (y0 + y1) / 2
        tw = fi.getlength(rotulo)
        d.rectangle([mx - tw / 2 - 2, my - 8, mx + tw / 2 + 2, my + 8], fill=(255, 255, 255))
        d.text((mx - tw / 2, my - 7), rotulo, font=fi, fill=(0, 0, 0))


def renderizar(caminho):
    fn, fb, fi = _fontes()
    W, H = 2780, 1500
    img = Image.new("RGB", (W, H), (255, 255, 255))
    d = ImageDraw.Draw(img)
    fp = ImageFont.truetype(FONTE_B, 18)
    for nome, x, y, w, h in PACOTES:
        for xx in range(x, x + w, 12):
            d.line([xx, y, min(xx + 6, x + w), y], fill=(0, 0, 0), width=1)
            d.line([xx, y + h, min(xx + 6, x + w), y + h], fill=(0, 0, 0), width=1)
        for yy in range(y, y + h, 12):
            d.line([x, yy, x, min(yy + 6, y + h)], fill=(0, 0, 0), width=1)
            d.line([x + w, yy, x + w, min(yy + 6, y + h)], fill=(0, 0, 0), width=1)
        d.text((x + 10, y + 6), "pacote " + nome, font=fp, fill=(0, 0, 0))
    caixas = {}
    for nome, (x, y) in POS.items():
        caixas[nome] = _caixa(d, nome, x, y, fn, fb, fi)
    # realizacoes da interface DAO<T> e dependencias da ConnectionFactory desenhadas como "barramentos"
    # verticais, para nao cruzarem as caixas dos DAOs
    dao_box = caixas["DAO<T>"]
    cf_box = caixas["ConnectionFactory"]
    bus_x = dao_box[0] + 40
    topo_bus = dao_box[3]
    impls = [a for a, b, t, _ in RELACOES if t == "realiza"]
    for nome in impls:
        c = caixas[nome]
        y = (c[1] + c[3]) / 2
        _seta(d, (c[2], y), (bus_x, y), "dep", "", fi)
    d.line([(bus_x, topo_bus + 18), (bus_x, (caixas[impls[-1]][1] + caixas[impls[-1]][3]) / 2)], fill=(0, 0, 0), width=2)
    d.polygon([(bus_x, topo_bus), (bus_x - 10, topo_bus + 18), (bus_x + 10, topo_bus + 18)], outline=(0, 0, 0), fill=(255, 255, 255))
    deps_cf = [a for a, b, t, _ in RELACOES if b == "ConnectionFactory"]
    bus2 = cf_box[0] + 120
    for nome in deps_cf:
        c = caixas[nome]
        y = (c[1] + c[3]) / 2 + 12
        d.line([(c[2], y), (bus2, y)], fill=(0, 0, 0), width=1)
    d.line([(bus2, (caixas[deps_cf[0]][1] + caixas[deps_cf[0]][3]) / 2 + 12), (bus2, cf_box[1])], fill=(0, 0, 0), width=1)
    _seta(d, (bus2, cf_box[1] - 30), (bus2, cf_box[1]), "dep", "usa", fi)
    for a, b, tipo, rot in RELACOES:
        if tipo == "realiza" or b == "ConnectionFactory":
            continue
        ca, cb = caixas[a], caixas[b]
        centro_b = ((cb[0] + cb[2]) / 2, (cb[1] + cb[3]) / 2)
        centro_a = ((ca[0] + ca[2]) / 2, (ca[1] + ca[3]) / 2)
        _seta(d, _ancora(ca, centro_b), _ancora(cb, centro_a), tipo, rot, fi)
    # legenda
    lx, ly = 40, H - 110
    d.rectangle([lx, ly, lx + 900, ly + 70], outline=(0, 0, 0), width=1)
    d.text((lx + 10, ly + 6), "Legenda: visibilidade  - privado  + público  # protegido  ~ pacote  |  seta com triângulo vazio: herança / realização de interface  |  losango preenchido: composição  |  tracejada: dependência", font=fi, fill=(0, 0, 0))
    d.text((lx + 10, ly + 30), "Os tipos são os do código Java; getters e setters foram resumidos em uma linha por classe. Enumerações agrupadas na caixa Enums.", font=fi, fill=(0, 0, 0))
    img.save(caminho, optimize=True)
    return caminho


if __name__ == "__main__":
    renderizar("diagrama_classes.png")
