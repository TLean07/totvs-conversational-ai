# -*- coding: utf-8 -*-
"""Gera o documento (HTML) da entrega e converte em PDF com o Microsoft Edge em modo headless."""
import base64
import html
import os
import subprocess

from modelo import TABELAS, SEQUENCIAS, TRIGGERS, EQUIPE, SOLUCAO, INTEGRANTES, cardinalidade, fk_identificadora, fk_obrigatoria, tabela

EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

CSS = """
@page { size: A4; margin: 18mm 16mm 18mm 16mm; }
@page paisagem { size: A4 landscape; margin: 12mm; }
* { box-sizing: border-box; }
body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 10.5pt; color: #1f2937; line-height: 1.45; }
h1 { font-size: 20pt; color: #0b3d91; margin: 0 0 6px 0; }
h2 { font-size: 15pt; color: #0b3d91; border-bottom: 2px solid #0b3d91; padding-bottom: 3px; margin: 22px 0 10px; page-break-after: avoid; }
h3 { font-size: 12pt; color: #1e3a8a; margin: 16px 0 6px; page-break-after: avoid; }
p { margin: 6px 0; text-align: justify; }
.capa { height: 250mm; display: flex; flex-direction: column; justify-content: space-between; page-break-after: always; }
.capa .topo { border-left: 10px solid #0b3d91; padding-left: 16px; margin-top: 30mm; }
.capa .titulo { font-size: 30pt; font-weight: 700; color: #0b3d91; line-height: 1.15; }
.capa .sub { font-size: 15pt; color: #374151; margin-top: 10px; }
.capa .disciplina { font-size: 13pt; color: #6b7280; margin-top: 30px; }
.capa table { width: 100%; border-collapse: collapse; margin-top: 12px; }
.capa th, .capa td { border-bottom: 1px solid #d1d5db; padding: 6px 8px; text-align: left; font-size: 11pt; }
.capa .rodape { color: #6b7280; font-size: 10pt; }
.quebra { page-break-before: always; }
table.dic { width: 100%; border-collapse: collapse; margin: 6px 0 12px; font-size: 8.6pt; page-break-inside: auto; }
table.dic th { background: #e5ecf9; color: #0b3d91; text-align: left; padding: 4px 6px; border: 1px solid #c7d2e8; }
table.dic td { padding: 3px 6px; border: 1px solid #d9e0ee; vertical-align: top; }
table.dic tr { page-break-inside: avoid; }
table.dic td.mono, code { font-family: Consolas, 'Courier New', monospace; font-size: 8.4pt; }
.tabela-titulo { background: #0b3d91; color: #fff; padding: 4px 8px; font-weight: 600; margin-top: 12px; page-break-after: avoid; font-size: 10pt; }
.tabela-desc { font-size: 9pt; color: #374151; margin: 2px 0 4px; }
.figura { text-align: center; margin: 8px 0; page-break-inside: avoid; }
.figura img { max-width: 100%; max-height: 160mm; border: 1px solid #cbd5e1; object-fit: contain; }
.paisagem { page: paisagem; page-break-before: always; page-break-after: always; }
.figura .legenda { font-size: 9pt; color: #4b5563; margin-top: 4px; }
ul { margin: 4px 0 6px 18px; } li { margin: 2px 0; }
.sumario li { margin: 3px 0; }
.caixa { border: 1px solid #c7d2e8; background: #f5f8ff; padding: 8px 12px; margin: 8px 0; border-radius: 4px; }
pre { font-family: Consolas, 'Courier New', monospace; font-size: 8.4pt; background: #f8fafc; border: 1px solid #e2e8f0; padding: 8px; white-space: pre-wrap; }
.chip { display: inline-block; background: #e5ecf9; color: #0b3d91; border-radius: 3px; padding: 0 5px; font-size: 8.5pt; margin-right: 3px; }
"""


def img64(caminho):
    with open(caminho, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def e(s):
    return html.escape(str(s))


def capa():
    linhas = "".join("<tr><td>%s</td><td>%s</td></tr>" % (e(n), e(rm)) for n, rm in INTEGRANTES)
    return """
<div class="capa">
  <div class="topo">
    <div class="titulo">%s</div>
    <div class="sub">Plataforma de analise automatizada de reunioes comerciais com IA</div>
    <div class="disciplina">Challenge FIAP x TOTVS 2026 &middot; Engenharia de Software &middot; 2&ordm; ano<br>
    <b>Database Design &mdash; Sprint 3</b>: implementacao fisica (DDL) e carga de dados (DML) no Oracle</div>
  </div>
  <div>
    <div style="font-size:13pt;color:#0b3d91;font-weight:600">Equipe %s</div>
    <table><tr><th>Integrante</th><th>RM</th></tr>%s</table>
  </div>
  <div class="rodape">Entrega: 14/09/2026 &middot; Oracle Database 19c (oracle.fiap.com.br) &middot; Diagrama no Oracle SQL Developer Data Modeler 24.3</div>
</div>
""" % (e(SOLUCAO), e(EQUIPE), linhas)


def sumario():
    itens = [
        "1. Apresentacao do projeto", "&nbsp;&nbsp;1.1 Objetivo e escopo", "&nbsp;&nbsp;1.2 Principais funcionalidades",
        "&nbsp;&nbsp;1.3 Arquitetura e papel do banco de dados",
        "2. Modelo relacional (Oracle SQL Developer Data Modeler)", "&nbsp;&nbsp;2.1 Diagrama completo",
        "&nbsp;&nbsp;2.2 Diagramas por area (subviews)", "&nbsp;&nbsp;2.3 Relacionamentos e cardinalidades",
        "&nbsp;&nbsp;2.4 Decisoes de modelagem",
        "3. Script DDL &mdash; estrutura do banco", "4. Script DML &mdash; carga, atualizacao e exclusao",
        "5. Como executar e conferir", "Anexo A &mdash; Dicionario de dados (todas as tabelas e colunas)",
        "Anexo B &mdash; Arquivos entregues",
    ]
    return "<h2>Sumario</h2><ul class='sumario' style='list-style:none;margin-left:0'>%s</ul>" % "".join("<li>%s</li>" % i for i in itens)


def apresentacao():
    return """
<h2>1. Apresentacao do projeto</h2>
<h3>1.1 Objetivo e escopo</h3>
<p>O <b>TOTVS Inteligencia Conversacional</b> transforma transcricoes brutas de reunioes comerciais e de Customer Success da TOTVS em inteligencia acionavel.
A plataforma recebe o texto da reuniao (digitado, importado dos arquivos JSON/CSV da TOTVS ou transcrito de audio), processa-o com a <b>LeanIA</b> (motor de
analise de linguagem natural proprio, gratuito e executado localmente) e devolve, para cada reuniao: sentimento geral e por area, produtos TOTVS em uso,
oportunidades de upsell/cross-sell/novo produto, riscos de churn, concorrentes citados, persona do interlocutor, budget mencionado, proxima acao sugerida
e alertas priorizados para o diretor comercial.</p>
<p>O escopo desta Sprint e a <b>implementacao fisica do banco de dados no Oracle</b>: o modelo relacional completo desenhado no Oracle SQL Developer Data Modeler,
o script DDL com todas as tabelas, constraints, sequence, coluna identity, indices, triggers e comentarios, e o script DML que popula as tabelas com dados
ficticios realistas e demonstra a evolucao dos processos de negocio (ciclo de vida da transcricao e do alerta) com UPDATE e DELETE.
O mesmo schema e usado em producao pelo backend Java (Spring Boot + JPA) da solucao, publicado na Render e conectado ao Oracle da FIAP.</p>
<h3>1.2 Principais funcionalidades</h3>
<ul>
<li><b>Ingestao de transcricoes</b> por formulario web, API REST, importacao em lote dos arquivos TOTVS (JSON e CSV) e transcricao de audio (modelo Parakeet).</li>
<li><b>Analise pela LeanIA</b>: sentimento (POSITIVO, NEGATIVO, NEUTRO, MISTO), produtos identificados (atual, oportunidade, concorrente), oportunidades com score e valor estimado,
riscos com severidade, persona, budget, proxima acao, insights (elogios, reclamacoes, duvidas, proximos passos) e indicadores de qualidade da conversa.</li>
<li><b>Consolidacao por reuniao</b>: riscos, oportunidades, alertas e itens criticos agrupados por categoria com ocorrencias, evidencias e recomendacao, alimentando o dashboard sem multiplicar linhas.</li>
<li><b>Alertas acionaveis</b> com prioridade e ciclo NOVO &rarr; VISTO &rarr; RESOLVIDO, e historico automatico de mudancas de status.</li>
<li><b>Dashboard executivo</b> (web e aplicativo Android) com metricas de sentimento, riscos por tipo, oportunidades, reunioes recentes e filtros por cliente.</li>
<li><b>Feedback adaptativo</b>: o usuario avalia a analise (util / ajuste) e a LeanIA usa os feedbacks recentes como contexto nas proximas analises.</li>
<li><b>Seguranca</b>: usuarios com perfil ADMIN/USER, senha com hash BCrypt, sessao com JWT e refresh token armazenado apenas como hash SHA-256.</li>
</ul>
<h3>1.3 Arquitetura e papel do banco de dados</h3>
<p>A solucao e composta por tres servicos: <b>frontend</b> Next.js (Vercel), <b>backend</b> Spring Boot (Render) e <b>ai-service</b> LeanIA em FastAPI (Hugging Face Spaces),
alem do aplicativo Android. O <b>Oracle Database 19c</b> da FIAP e a unica fonte de verdade: guarda usuarios, sessoes, transcricoes (texto integral em CLOB), analises,
listas de detalhes, alertas, feedbacks e o historico de status. O backend acessa o banco por Spring Data JPA/Hibernate com <code>ddl-auto=validate</code>,
ou seja, a estrutura e criada exclusivamente pelos scripts desta entrega e o aplicativo apenas valida que as tabelas e colunas existem com os tipos esperados.</p>
"""


def figura(caminho, legenda, largura=None):
    estilo = ' style="max-width:%s"' % largura if largura else ""
    return '<div class="figura"><img src="%s"%s><div class="legenda">%s</div></div>' % (img64(caminho), estilo, legenda)


def modelo(pasta_img):
    subviews = [
        ("subview_seguranca.png", "Subview 1 - Seguranca, transcricao e historico de status: APP_USERS, REFRESH_TOKENS, TRANSCRICOES, TRANSCRICAO_STATUS_HIST e TRANSCRICAO_WARNINGS."),
        ("subview_transcricao.png", "Subview 2 - Transcricao e listas consolidadas (riscos, oportunidades, alertas e itens criticos por reuniao)."),
        ("subview_analise.png", "Subview 3 - Analise da LeanIA e suas listas de detalhe (produtos, oportunidades, riscos e consolidados)."),
        ("subview_alertas.png", "Subview 4 - Alertas, historico de status do alerta e feedbacks do usuario, ligados a analise."),
    ]
    h = ["<h2 class='quebra'>2. Modelo relacional (Oracle SQL Developer Data Modeler)</h2>",
         "<p>O modelo relacional foi construido no <b>Oracle SQL Developer Data Modeler 24.3</b> (design <code>TOTVS_Inteligencia_Conversacional.dmd</code>, "
         "entregue no ZIP na pasta <code>datamodeler/</code>). Sao <b>%d tabelas</b>, <b>%d chaves estrangeiras</b>, %d restricoes UNIQUE, 1 sequence e 2 colunas IDENTITY. "
         "Todas as tabelas, atributos, chaves primarias, chaves estrangeiras, relacionamentos e cardinalidades estao representados no diagrama; "
         "a notacao e a padrao do Data Modeler (Information Engineering): pe de galinha no lado N, barra no lado 1 obrigatorio e circulo no lado 1 opcional; "
         "linha continua para relacionamento identificador (a FK faz parte da PK) e tracejada para nao identificador.</p>"
         % (len(TABELAS), sum(len(t["fks"]) for t in TABELAS), sum(len(t["uks"]) for t in TABELAS)),
         "<h3>2.1 Diagrama completo</h3>",
         "<p>A Figura 1 e a exportacao do diagrama diretamente do Data Modeler (Arquivo &gt; Imprimir Diagrama &gt; Para Arquivo de Imagem), em pagina paisagem. "
         "As Figuras 2 a 5 ampliam cada area do mesmo modelo para leitura dos atributos e tipos; as imagens em alta resolucao acompanham o ZIP (pasta <code>diagramas/</code>).</p>",
         "<div class='paisagem'>" + figura(os.path.join(pasta_img, "datamodeler_relacional_completo.png"),
                                           "Figura 1 - Modelo relacional completo (20 tabelas, 24 FKs) exportado do Oracle SQL Developer Data Modeler.", "255mm") + "</div>",
         "<div class='paisagem'>" + figura(os.path.join(pasta_img, "datamodeler_relacional_esquerda.png"),
                                           "Figura 1a - Metade esquerda do diagrama do Data Modeler (seguranca, transcricoes e listas consolidadas).", "255mm") + "</div>",
         "<div class='paisagem'>" + figura(os.path.join(pasta_img, "datamodeler_relacional_direita.png"),
                                           "Figura 1b - Metade direita do diagrama do Data Modeler (analises, detalhes, alertas, historico e feedbacks).", "255mm") + "</div>",
         ]
    for i, (arq, leg) in enumerate(subviews):
        titulo = "<h3 style='margin-top:0'>2.2 Diagramas por area (subviews)</h3>" if i == 0 else ""
        h.append("<div class='paisagem'>" + titulo + figura(os.path.join(pasta_img, arq), "Figura %d - %s" % (i + 2, leg), "255mm") + "</div>")
    h.append("<h3 class='quebra'>2.3 Relacionamentos e cardinalidades</h3>")
    h.append("<table class='dic'><tr><th>Constraint</th><th>Tabela filha (N)</th><th>Coluna(s)</th><th>Tabela pai (1)</th><th>Cardinalidade</th><th>Tipo</th><th>Obrigatoria</th><th>ON DELETE</th></tr>")
    for t in TABELAS:
        for fk in t["fks"]:
            h.append("<tr><td class='mono'>%s</td><td>%s</td><td class='mono'>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>" % (
                e(fk["nome"]), e(t["nome"]), e(", ".join(fk["colunas"])), e(fk["ref"]), cardinalidade(t, fk),
                "identificador" if fk_identificadora(t, fk) else "nao identificador",
                "sim" if fk_obrigatoria(t, fk) else "nao (opcional)", e(fk["on_delete"] or "NO ACTION")))
    h.append("</table>")
    h.append("""
<p><b>Leitura das cardinalidades.</b> Um usuario (APP_USERS) possui N sessoes, N transcricoes, N analises, N alertas e N feedbacks.
Uma transcricao possui N avisos, N riscos/oportunidades/alertas/itens consolidados e N registros de historico, e tem <b>no maximo uma analise atual</b>
(relacionamento 1:1 garantido por UK_TRANSCRICOES_ANALISE sobre TRANSCRICOES.ANALISE_ID); a analise, por sua vez, referencia a transcricao analisada.
Uma analise possui N produtos, oportunidades, riscos, consolidados, itens criticos e N alertas; um alerta possui N registros de historico.</p>
<h3>2.4 Decisoes de modelagem</h3>
<ul>
<li><b>Chaves primarias UUID</b> (VARCHAR2(36 CHAR)) nas entidades de negocio: geradas pela aplicacao, sem ida ao banco, compativeis com a migracao feita a partir do MongoDB e com as URLs da API.
Para atender a numeracao sequencial, TRANSCRICOES ganhou <b>NUM_PROTOCOLO</b>, preenchido por DEFAULT com a sequence <code>SEQ_TRANSCRICAO_PROTOCOLO</code> e protegido por UNIQUE;
as tabelas de historico usam <b>coluna IDENTITY</b> (autonumeracao nativa do Oracle).</li>
<li><b>Listas ordenadas em tabelas filhas</b> (TRANSCRICAO_* e ANALISE_*): chave primaria composta (POSICAO, ID do pai), FK com ON DELETE CASCADE; POSICAO preserva a ordem gerada pela LeanIA.</li>
<li><b>Restricoes contra duplicidade</b>: e-mail unico em APP_USERS, hash unico em REFRESH_TOKENS, MEETING_ID unico em TRANSCRICOES (o mesmo arquivo TOTVS nao e importado duas vezes),
NUM_PROTOCOLO unico e ANALISE_ID unico (uma analise atual por reuniao).</li>
<li><b>Dominios com CHECK</b>: status, sentimento, severidade, prioridade, tipo de produto/oportunidade/risco e flags 0/1 sao validados no banco, espelhando os enums da aplicacao.</li>
<li><b>Integridade referencial com semantica de negocio</b>: apagar uma transcricao remove em cascata avisos, consolidados, historico, analise e alertas; apagar uma analise
zera a referencia na transcricao e no feedback (ON DELETE SET NULL), permitindo reprocessar sem perder a reuniao nem a avaliacao do usuario.</li>
<li><b>Textos longos em CLOB</b> (transcricao, resumos, descricoes, JSON de insights/indicadores), pois VARCHAR2 e limitado a 4000 bytes no Oracle da FIAP.</li>
<li><b>Historico por trigger</b>: TRG_TRANSCRICOES_STATUS_HIST e TRG_ALERTAS_STATUS_HIST gravam toda mudanca de status, dando auditoria e rastreabilidade ao processo sem depender da aplicacao.</li>
<li><b>Indices</b> nas colunas de filtro do dashboard (dono, status, cliente, data, segmento, UF, prioridade) e em todas as FKs consultadas.</li>
</ul>
""")
    return "\n".join(h)


def scripts():
    trig = "".join("<li><code>%s</code> - %s</li>" % (e(t["nome"]), e(t["comentario"])) for t in TRIGGERS)
    return """
<h2 class='quebra'>3. Script DDL &mdash; estrutura do banco</h2>
<p>Arquivo <code>01_DDL_COMPANY404_TOTVS.sql</code>, gerado a partir do modelo e executado do zero em um Oracle limpo para validacao. Conteudo:</p>
<ul>
<li><b>1 sequence</b> (<code>SEQ_TRANSCRICAO_PROTOCOLO</code>) usada como DEFAULT de TRANSCRICOES.NUM_PROTOCOLO.</li>
<li><b>%d CREATE TABLE</b> com PK, FK, UNIQUE, NOT NULL e CHECK declarados como constraints nomeadas; <b>2 colunas IDENTITY</b> (historicos).</li>
<li><b>1 ALTER TABLE</b> para a FK circular 1:1 TRANSCRICOES.ANALISE_ID &rarr; ANALISES (so pode existir depois das duas tabelas).</li>
<li><b>%d indices</b> secundarios para as consultas do dashboard.</li>
<li><b>2 triggers</b> de historico: <ul>%s</ul></li>
<li><b>COMMENT ON</b> em todas as %d tabelas e em todas as %d colunas (dicionario de dados no Anexo A).</li>
<li>Secao de conferencia (contagem de objetos) e secao de limpeza comentada para recriar do zero.</li>
</ul>
<h2>4. Script DML &mdash; carga, atualizacao e exclusao</h2>
<p>Arquivo <code>02_DML_COMPANY404_TOTVS.sql</code>. Os identificadores sao UUIDs fixos para o script ser legivel e reexecutavel; os dados sao ficticios porem realistas,
seguindo o vocabulario da solucao (produtos TOTVS Protheus, RM, Fluig, Datasul, Techfin; concorrentes Senior, Oracle NetSuite, Bling; tipos de alerta e categorias da LeanIA).</p>
<h3>4.1 INSERT</h3>
<p>Todas as tabelas recebem no minimo 5 registros: 5 usuarios, 6 sessoes, 9 reunioes de 9 clientes de segmentos diferentes (6 processadas, 1 pendente, 1 em processamento e 1 com erro),
6 analises com produtos, oportunidades, riscos e consolidados, 8 alertas, 6 feedbacks e os registros de criacao nos historicos. As reunioes sao inseridas com ANALISE_ID nulo e vinculadas
a analise apos a insercao de ANALISES (FK circular). NUM_PROTOCOLO nao aparece nos INSERTs: e preenchido pelo DEFAULT da sequence.</p>
<h3>4.2 UPDATE &mdash; evolucao dos processos</h3>
<ul>
<li><b>Transcricao "Construtora Alvorada"</b>: PENDENTE &rarr; PROCESSANDO &rarr; PROCESSADA. A fila pega a reuniao, a LeanIA devolve a analise (INSERT em ANALISES e detalhes),
a transcricao recebe a analise atual, as metricas e o resumo executivo, e um alerta e gerado. O trigger registra as duas transicoes em TRANSCRICAO_STATUS_HIST.</li>
<li><b>Transcricao "Agro Serra Verde"</b>: PROCESSANDO &rarr; ERRO (LeanIA indisponivel) e UPDATE da observacao no historico.</li>
<li><b>Alerta de churn</b>: NOVO &rarr; VISTO &rarr; RESOLVIDO com observacao do tratamento; outros alertas NOVO &rarr; VISTO.</li>
<li><b>Feedback</b> revisado (nao util &rarr; util), <b>sessao</b> revogada no logout, <b>usuario</b> reativado e <b>NPS</b> atualizado apos nova pesquisa.</li>
</ul>
<h3>4.3 DELETE &mdash; exclusoes com integridade referencial</h3>
<ul>
<li>Sessao expirada removida (limpeza de refresh tokens) e feedback enviado por engano removido.</li>
<li>Transcricao com ERRO e texto vazio removida: os avisos e o historico saem em cascata (contagem antes e depois no script).</li>
<li>Analise apagada para reprocessamento: TRANSCRICOES.ANALISE_ID e ANALISE_FEEDBACKS.ANALISE_ID ficam NULL (SET NULL) e os detalhes saem em cascata.</li>
</ul>
<h3>4.4 Resultado da execucao (Oracle limpo)</h3>
<p>Contagem por tabela ao final do script (apos INSERT, UPDATE e DELETE):</p>
<table class='dic'><tr><th>Tabela</th><th>Registros</th><th>Tabela</th><th>Registros</th></tr>
<tr><td>ALERTAS</td><td>9</td><td>ANALISE_RISCOS_CONS</td><td>5</td></tr>
<tr><td>ALERTA_STATUS_HIST</td><td>12</td><td>APP_USERS</td><td>5</td></tr>
<tr><td>ANALISES</td><td>6</td><td>REFRESH_TOKENS</td><td>5</td></tr>
<tr><td>ANALISE_ALERTAS_CONS</td><td>7</td><td>TRANSCRICAO_ALERTAS_CONS</td><td>7</td></tr>
<tr><td>ANALISE_FEEDBACKS</td><td>5</td><td>TRANSCRICAO_ITENS_CRITICOS</td><td>8</td></tr>
<tr><td>ANALISE_ITENS_CRITICOS</td><td>8</td><td>TRANSCRICAO_OPORT_CONS</td><td>7</td></tr>
<tr><td>ANALISE_OPORTUNIDADES</td><td>6</td><td>TRANSCRICAO_RISCOS_CONS</td><td>6</td></tr>
<tr><td>ANALISE_OPORT_CONS</td><td>6</td><td>TRANSCRICAO_STATUS_HIST</td><td>11</td></tr>
<tr><td>ANALISE_PRODUTOS</td><td>11</td><td>TRANSCRICAO_WARNINGS</td><td>6</td></tr>
<tr><td>ANALISE_RISCOS</td><td>6</td><td>TRANSCRICOES</td><td>8</td></tr>
</table>
<h2>5. Como executar e conferir</h2>
<ol>
<li>Abrir o SQL Developer conectado ao schema (ex.: RM561344 em oracle.fiap.com.br:1521/ORCL) ou o SQLcl.</li>
<li>Executar <code>01_DDL_COMPANY404_TOTVS.sql</code> como script (F5). Se o schema ja tiver as tabelas, descomentar e rodar antes a secao LIMPEZA no fim do arquivo.</li>
<li>Executar <code>02_DML_COMPANY404_TOTVS.sql</code> como script (F5). As consultas de conferencia mostram o historico de status, o efeito da cascata e a contagem por tabela.</li>
<li>Abrir <code>datamodeler/TOTVS_Inteligencia_Conversacional.dmd</code> no Oracle SQL Developer Data Modeler para navegar no modelo relacional.</li>
</ol>
<p>Os dois scripts foram validados do zero em um Oracle Database Free 23ai (container Docker), sem erros, usando apenas sintaxe compativel com o Oracle 19c da FIAP (sem tipos ou clausulas exclusivas do 23ai).</p>
""" % (len(TABELAS), sum(len(t["indices"]) for t in TABELAS), trig, len(TABELAS), sum(len(t["colunas"]) for t in TABELAS))


def dicionario():
    h = ["<h2 class='quebra'>Anexo A &mdash; Dicionario de dados</h2>",
         "<p>Todas as tabelas e colunas com tipo, obrigatoriedade, chaves e o comentario registrado no banco (COMMENT ON).</p>"]
    for t in TABELAS:
        h.append("<div class='tabela-titulo'>%s</div><div class='tabela-desc'>%s</div>" % (e(t["nome"]), e(t["comentario"])))
        h.append("<table class='dic'><tr><th style='width:26%'>Coluna</th><th style='width:17%'>Tipo</th><th style='width:9%'>Nulo</th><th style='width:8%'>Chave</th><th>Descricao</th></tr>")
        for c in t["colunas"]:
            chaves = []
            if c["nome"] in t["pk"]["colunas"]:
                chaves.append("PK")
            if any(c["nome"] in fk["colunas"] for fk in t["fks"]):
                chaves.append("FK")
            if any(c["nome"] in uk["colunas"] for uk in t["uks"]):
                chaves.append("UK")
            tipo = "NUMBER(19) IDENTITY" if c["identity"] else c["tipo"]
            if c["default"] and not c["identity"]:
                tipo += " DEFAULT " + c["default"]
            h.append("<tr><td class='mono'>%s</td><td class='mono'>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>" % (
                e(c["nome"]), e(tipo), "nao" if not c["nulo"] else "sim", " ".join(chaves), e(c["comentario"])))
        h.append("</table>")
        extras = ["<span class='chip'>PK %s</span>" % e(t["pk"]["nome"])]
        extras += ["<span class='chip'>UK %s</span>" % e(uk["nome"]) for uk in t["uks"]]
        extras += ["<span class='chip'>FK %s &rarr; %s</span>" % (e(fk["nome"]), e(fk["ref"])) for fk in t["fks"]]
        extras += ["<span class='chip'>IDX %s</span>" % e(i["nome"]) for i in t["indices"]]
        h.append("<p style='margin-top:-6px'>%s</p>" % " ".join(extras))
    return "\n".join(h)


def anexo_arquivos():
    return """
<h2>Anexo B &mdash; Arquivos entregues</h2>
<table class='dic'><tr><th>Arquivo</th><th>Conteudo</th></tr>
<tr><td class='mono'>Company404_DatabaseDesign_Sprint3.pdf</td><td>Este documento (itens 1 e 2 da entrega).</td></tr>
<tr><td class='mono'>01_DDL_COMPANY404_TOTVS.sql</td><td>Item 3 - estrutura do banco: sequence, tabelas, constraints, identity, indices, triggers e comentarios.</td></tr>
<tr><td class='mono'>02_DML_COMPANY404_TOTVS.sql</td><td>Item 4 - carga (INSERT), evolucao dos processos (UPDATE), exclusoes (DELETE) e conferencias.</td></tr>
<tr><td class='mono'>datamodeler/TOTVS_Inteligencia_Conversacional.dmd (+ pasta)</td><td>Design do Oracle SQL Developer Data Modeler com o modelo relacional e o diagrama.</td></tr>
<tr><td class='mono'>diagramas/*.png</td><td>Imagens do diagrama completo e das subviews em alta resolucao.</td></tr>
</table>
<p style='color:#6b7280;font-size:9pt'>Repositorio da solucao: github.com/TLean07/totvs-conversational-ai (branch oracle) &middot; os scripts desta entrega sao os mesmos usados pelo backend em producao.</p>
"""


def gerar_html(pasta_img):
    corpo = capa() + sumario() + apresentacao() + modelo(pasta_img) + scripts() + dicionario() + anexo_arquivos()
    return "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='utf-8'><title>%s - Database Design Sprint 3</title><style>%s</style></head><body>%s</body></html>" % (
        e(SOLUCAO), CSS, corpo)


def gerar_pdf(html_path, pdf_path):
    cmd = [EDGE, "--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--print-to-pdf=%s" % pdf_path,
           "--no-first-run", "--disable-extensions", "--user-data-dir=%s" % os.path.join(os.environ.get("TEMP", "."), "edge-pdf-profile"),
           "file:///" + html_path.replace("\\", "/")]
    subprocess.run(cmd, check=True, timeout=180, capture_output=True)
    return pdf_path
