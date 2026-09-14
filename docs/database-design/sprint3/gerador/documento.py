# -*- coding: utf-8 -*-
"""
Gera o documento da entrega (HTML) e converte em PDF com o Microsoft Edge em modo headless.
Estilo de trabalho acadêmico: Times New Roman, preto e branco, tabelas simples e numeração de página.
"""
import base64
import html
import os
import subprocess

from acentos import acentuar
from modelo import TABELAS, SEQUENCIAS, TRIGGERS, EQUIPE, SOLUCAO, INTEGRANTES, cardinalidade, fk_identificadora, fk_obrigatoria

EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

CSS = """
@page { size: A4; margin: 25mm 20mm 25mm 25mm; }
@page paisagem { size: A4 landscape; margin: 15mm 15mm 18mm 15mm; }
* { box-sizing: border-box; }
body { font-family: 'Times New Roman', Times, serif; font-size: 11.5pt; color: #000; line-height: 1.35; }
h1 { font-size: 16pt; font-weight: bold; margin: 14px 0 10px; }
h2 { font-size: 12.5pt; font-weight: bold; margin: 18px 0 8px; page-break-after: avoid; }
h3 { font-size: 11.5pt; font-weight: bold; margin: 12px 0 6px; page-break-after: avoid; }
p { margin: 0 0 8px; text-align: justify; text-indent: 1.25cm; }
p.semrecuo, li p { text-indent: 0; }
ul, ol { margin: 0 0 8px 0.9cm; padding: 0; } li { margin: 2px 0; text-align: justify; }
table { width: 100%; border-collapse: collapse; margin: 6px 0 12px; font-size: 10pt; }
th, td { border: 1px solid #000; padding: 4px 6px; vertical-align: top; text-align: left; font-weight: normal; }
th { font-weight: normal; }
tr { page-break-inside: avoid; }
td.mono, code { font-family: 'Courier New', Courier, monospace; font-size: 9.5pt; }
.cab { font-size: 11pt; line-height: 1.35; margin-bottom: 14px; }
.cab b { font-weight: bold; }
.capa { height: 235mm; display: flex; flex-direction: column; justify-content: space-between; page-break-after: always; }
.capa .inst { text-align: center; font-size: 12pt; line-height: 1.5; }
.capa .tit { text-align: center; margin-top: 55mm; }
.capa .tit .t1 { font-size: 20pt; font-weight: bold; }
.capa .tit .t2 { font-size: 13pt; margin-top: 10px; }
.capa .tit .t3 { font-size: 12pt; margin-top: 26px; }
.capa .grupo { font-size: 12pt; line-height: 1.5; }
.capa .rod { text-align: center; font-size: 12pt; line-height: 1.5; }
.quebra { page-break-before: always; }
.figura { text-align: center; margin: 6px 0 10px; page-break-inside: avoid; }
.figura img { max-width: 100%; max-height: 158mm; border: 1px solid #000; object-fit: contain; }
.figura .leg { font-size: 10pt; margin-top: 4px; text-align: center; }
.paisagem { page: paisagem; page-break-before: always; page-break-after: always; }
.nota { font-size: 10pt; text-indent: 0; }
.tabnome { font-weight: bold; margin: 12px 0 2px; page-break-after: avoid; }
.tabdesc { font-size: 10.5pt; margin: 0 0 4px; text-indent: 0; }
.chaves { font-size: 9.5pt; margin: -6px 0 10px; text-indent: 0; }
.sumario td { border: none; padding: 2px 4px; font-size: 11.5pt; }
"""


def img64(caminho):
    with open(caminho, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def e(s):
    return html.escape(str(s))


def figura(caminho, legenda):
    return '<div class="figura"><img src="%s"><div class="leg">%s</div></div>' % (img64(caminho), legenda)


# ------------------------------------------------------------------------------------------
def capa():
    integrantes = "<br>".join("%s &ndash; RM %s" % (e(n), e(rm.replace("RM", ""))) for n, rm in INTEGRANTES)
    return """
<div class="capa">
  <div class="inst">FIAP &ndash; Faculdade de Informática e Administração Paulista<br>
  Engenharia de Software &ndash; 2&ordm; ano<br>
  Challenge 2026 &ndash; TOTVS</div>
  <div class="tit">
    <div class="t1">%s</div>
    <div class="t2">Plataforma de análise automatizada de reuniões comerciais com inteligência artificial</div>
    <div class="t3"><b>Database Design &ndash; Sprint 3</b><br>Implementação física do banco de dados no Oracle: modelo relacional, script DDL e script DML</div>
  </div>
  <div class="grupo"><b>Grupo %s</b><br>%s</div>
  <div class="rod">São Paulo<br>Setembro de 2026</div>
</div>
""" % (e(acentuar(SOLUCAO)), e(EQUIPE), integrantes)


def cabecalho():
    linhas = "<br>".join("%s &ndash; RM: %s" % (e(n), e(rm.replace("RM", ""))) for n, rm in INTEGRANTES)
    return """
<div class="cab"><b>Grupo %s</b><br>%s<br>Database Design &ndash; Challenge 2026 TOTVS &ndash; Sprint 3 &ndash; 2&ordm; ano</div>
<h1>Implementação física do banco de dados da solução %s</h1>
""" % (e(EQUIPE), linhas, e(acentuar(SOLUCAO)))


def sumario():
    itens = [
        ("1.", "Objetivo e escopo"), ("2.", "Principais funcionalidades da solução"),
        ("3.", "Arquitetura e papel do banco de dados"), ("4.", "Modelo relacional (Oracle SQL Developer Data Modeler)"),
        ("4.1", "Diagrama completo"), ("4.2", "Diagramas por área"), ("4.3", "Relacionamentos e cardinalidades"),
        ("4.4", "Decisões de modelagem"), ("5.", "Script DDL &ndash; estrutura do banco"),
        ("6.", "Script DML &ndash; carga, atualização e exclusão"), ("7.", "Como executar e conferir"),
        ("8.", "Dicionário de dados"), ("9.", "Arquivos entregues e documentos de referência"),
    ]
    linhas = "".join("<tr><td style='width:1.2cm'>%s</td><td>%s</td></tr>" % (n, t) for n, t in itens)
    return "<h2>Sumário</h2><table class='sumario'>%s</table>" % linhas


def apresentacao():
    return """
<h2>1. Objetivo e escopo</h2>
<p>O <b>TOTVS Inteligência Conversacional</b> transforma transcrições brutas de reuniões comerciais e de Customer Success da TOTVS
em inteligência acionável. A plataforma recebe o texto da reunião (digitado, importado dos arquivos JSON/CSV da TOTVS ou transcrito
de áudio), processa-o com a <b>LeanIA</b> (motor próprio de análise de linguagem natural, gratuito e executado localmente) e devolve,
para cada reunião: sentimento geral e por área, produtos TOTVS em uso, oportunidades de upsell, cross-sell e novo produto, riscos de
churn, concorrentes citados, persona do interlocutor, budget mencionado, próxima ação sugerida e alertas priorizados para o diretor comercial.</p>
<p>O escopo desta Sprint é a <b>implementação física do banco de dados no Oracle</b>: o modelo relacional completo desenhado no Oracle SQL
Developer Data Modeler, o script DDL com todas as tabelas, restrições, sequence, coluna identity, índices, triggers e comentários, e o
script DML que popula as tabelas com dados fictícios realistas e demonstra a evolução dos processos de negócio (ciclo de vida da
transcrição e do alerta) com UPDATE e DELETE. O mesmo schema é usado pelo backend Java (Spring Boot e JPA) da solução, publicado na
Render e conectado ao Oracle da FIAP.</p>

<h2>2. Principais funcionalidades da solução</h2>
<table>
<tr><th style="width:4.2cm">Funcionalidade</th><th>Descrição</th></tr>
<tr><td>Ingestão de transcrições</td><td>Formulário web, API REST, importação em lote dos arquivos TOTVS (JSON e CSV) e transcrição de áudio (modelo Parakeet).</td></tr>
<tr><td>Análise pela LeanIA</td><td>Sentimento (POSITIVO, NEGATIVO, NEUTRO, MISTO), produtos identificados (atual, oportunidade, concorrente), oportunidades com score e valor
estimado, riscos com severidade, persona, budget, próxima ação, insights (elogios, reclamações, dúvidas, próximos passos) e indicadores de qualidade da conversa.</td></tr>
<tr><td>Consolidação por reunião</td><td>Riscos, oportunidades, alertas e itens críticos agrupados por categoria, com ocorrências, evidências e recomendação, alimentando o dashboard sem multiplicar linhas.</td></tr>
<tr><td>Alertas acionáveis</td><td>Prioridade e ciclo NOVO &rarr; VISTO &rarr; RESOLVIDO, com histórico automático das mudanças de status.</td></tr>
<tr><td>Dashboard executivo</td><td>Aplicação web e aplicativo Android com métricas de sentimento, riscos por tipo, oportunidades, reuniões recentes e filtros por cliente.</td></tr>
<tr><td>Feedback adaptativo</td><td>O usuário avalia a análise (útil ou ajuste) e a LeanIA usa os feedbacks recentes como contexto nas próximas análises.</td></tr>
<tr><td>Segurança</td><td>Usuários com perfil ADMIN ou USER, senha com hash BCrypt, sessão com JWT e refresh token armazenado apenas como hash SHA-256.</td></tr>
</table>

<h2>3. Arquitetura e papel do banco de dados</h2>
<p>A solução é composta por três serviços: o <b>frontend</b> em Next.js (Vercel), o <b>backend</b> em Spring Boot (Render) e o
<b>ai-service</b> com a LeanIA em FastAPI (Hugging Face Spaces), além do aplicativo Android. O <b>Oracle Database 19c</b> da FIAP é a única
fonte de verdade: guarda usuários, sessões, transcrições (texto integral em CLOB), análises, listas de detalhes, alertas, feedbacks e o
histórico de status.</p>
<p>O backend acessa o banco por Spring Data JPA/Hibernate com a propriedade <code>ddl-auto=validate</code>, ou seja, a estrutura é criada
exclusivamente pelos scripts desta entrega e a aplicação apenas valida, na subida, que as tabelas e colunas existem com os tipos esperados.</p>
"""


def modelo(pasta_img):
    subviews = [
        ("subview_seguranca.png", "Segurança, transcrição e histórico de status: APP_USERS, REFRESH_TOKENS, TRANSCRICOES, TRANSCRICAO_STATUS_HIST e TRANSCRICAO_WARNINGS."),
        ("subview_transcricao.png", "Transcrição e listas consolidadas (riscos, oportunidades, alertas e itens críticos por reunião)."),
        ("subview_analise.png", "Análise da LeanIA e suas listas de detalhe (produtos, oportunidades, riscos e consolidados)."),
        ("subview_alertas.png", "Alertas, histórico de status do alerta e feedbacks do usuário, ligados à análise."),
    ]
    n_fk = sum(len(t["fks"]) for t in TABELAS)
    n_uk = sum(len(t["uks"]) for t in TABELAS)
    h = ["<h2>4. Modelo relacional (Oracle SQL Developer Data Modeler)</h2>",
         "<p>O modelo relacional foi construído no <b>Oracle SQL Developer Data Modeler 24.3</b>. O design (arquivo "
         "<code>TOTVS_Inteligencia_Conversacional.dmd</code> e pasta correspondente) acompanha a entrega na pasta <code>datamodeler/</code>. "
         "São <b>%d tabelas</b>, <b>%d chaves estrangeiras</b>, %d restrições UNIQUE, uma sequence e duas colunas IDENTITY. Todas as tabelas, "
         "atributos, chaves primárias, chaves estrangeiras, relacionamentos e cardinalidades estão representados no diagrama.</p>"
         % (len(TABELAS), n_fk, n_uk),
         "<p>A notação é a padrão do Data Modeler (Information Engineering): pé de galinha no lado N; barra no lado 1 obrigatório e círculo "
         "no lado 1 opcional; linha contínua para relacionamento identificador (a chave estrangeira faz parte da chave primária) e linha "
         "tracejada para relacionamento não identificador. Nas caixas, <b>P</b> indica chave primária, <b>F</b> chave estrangeira, "
         "<b>U</b> chave única e <b>*</b> coluna NOT NULL.</p>",
         "<h3>4.1 Diagrama completo</h3>",
         "<p>A Figura 1 é a exportação do diagrama feita pelo próprio Data Modeler (menu Arquivo &gt; Imprimir Diagrama &gt; Para Arquivo de "
         "Imagem). Como o modelo tem 20 tabelas, a Figura 1 mostra a visão geral e as Figuras 2 e 3 ampliam as duas metades. As Figuras 4 a 7 "
         "detalham cada área do mesmo modelo. As imagens em alta resolução estão na pasta <code>diagramas/</code> da entrega.</p>",
         "<div class='paisagem'>" + figura(os.path.join(pasta_img, "datamodeler_relacional_completo.png"),
                                           "Figura 1 &ndash; Modelo relacional completo exportado do Oracle SQL Developer Data Modeler.") + "</div>",
         "<div class='paisagem'>" + figura(os.path.join(pasta_img, "datamodeler_relacional_esquerda.png"),
                                           "Figura 2 &ndash; Metade esquerda do diagrama: segurança, transcrições e listas consolidadas.") + "</div>",
         "<div class='paisagem'>" + figura(os.path.join(pasta_img, "datamodeler_relacional_direita.png"),
                                           "Figura 3 &ndash; Metade direita do diagrama: análises, detalhes, alertas, histórico e feedbacks.") + "</div>",
         ]
    for i, (arq, leg) in enumerate(subviews):
        titulo = "<h3 style='margin-top:0'>4.2 Diagramas por área</h3>" if i == 0 else ""
        h.append("<div class='paisagem'>" + titulo + figura(os.path.join(pasta_img, arq), "Figura %d &ndash; %s" % (i + 4, leg)) + "</div>")

    h.append("<h3>4.3 Relacionamentos e cardinalidades</h3>")
    h.append("<p class='semrecuo'>A tabela abaixo lista cada chave estrangeira do modelo com a cardinalidade, o tipo do relacionamento, "
             "a obrigatoriedade e a regra de exclusão.</p>")
    h.append("<table><tr><th>Constraint</th><th>Tabela filha (N)</th><th>Coluna</th><th>Tabela pai (1)</th><th>Card.</th><th>Tipo</th><th>Obrig.</th><th>ON DELETE</th></tr>")
    for t in TABELAS:
        for fk in t["fks"]:
            h.append("<tr><td class='mono'>%s</td><td>%s</td><td class='mono'>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>" % (
                e(fk["nome"]), e(t["nome"]), e(", ".join(fk["colunas"])), e(fk["ref"]), cardinalidade(t, fk),
                "identificador" if fk_identificadora(t, fk) else "não identificador",
                "sim" if fk_obrigatoria(t, fk) else "não", e(fk["on_delete"] or "NO ACTION")))
    h.append("</table>")
    h.append("""
<p>Um usuário (APP_USERS) possui N sessões, N transcrições, N análises, N alertas e N feedbacks. Uma transcrição possui N avisos,
N riscos, oportunidades, alertas e itens consolidados e N registros de histórico, e tem <b>no máximo uma análise atual</b>
(relacionamento 1:1 garantido pela restrição UK_TRANSCRICOES_ANALISE sobre a coluna TRANSCRICOES.ANALISE_ID); a análise, por sua vez,
referencia a transcrição analisada. Uma análise possui N produtos, oportunidades, riscos, consolidados, itens críticos e N alertas;
um alerta possui N registros de histórico.</p>
<h3>4.4 Decisões de modelagem</h3>
<table>
<tr><th style="width:4.2cm">Decisão</th><th>Justificativa</th></tr>
<tr><td>Chaves primárias UUID</td><td>As entidades de negócio usam VARCHAR2(36 CHAR) gerado pela aplicação, sem ida ao banco, compatível com a migração feita a partir
do MongoDB e com as URLs da API. Para atender à numeração sequencial, TRANSCRICOES ganhou a coluna NUM_PROTOCOLO, preenchida por DEFAULT com a sequence
SEQ_TRANSCRICAO_PROTOCOLO e protegida por UNIQUE; as tabelas de histórico usam coluna IDENTITY (autonumeração nativa do Oracle).</td></tr>
<tr><td>Listas ordenadas em tabelas filhas</td><td>As tabelas TRANSCRICAO_* e ANALISE_* têm chave primária composta (POSICAO e ID do pai) e chave estrangeira com ON DELETE CASCADE;
POSICAO preserva a ordem gerada pela LeanIA.</td></tr>
<tr><td>Restrições contra duplicidade</td><td>E-mail único em APP_USERS, hash único em REFRESH_TOKENS, MEETING_ID único em TRANSCRICOES (o mesmo arquivo TOTVS não é importado
duas vezes), NUM_PROTOCOLO único e ANALISE_ID único (uma análise atual por reunião).</td></tr>
<tr><td>Domínios com CHECK</td><td>Status, sentimento, severidade, prioridade, tipo de produto, de oportunidade e de risco e os indicadores 0/1 são validados no banco,
espelhando as enumerações da aplicação.</td></tr>
<tr><td>Integridade referencial com semântica de negócio</td><td>Apagar uma transcrição remove em cascata avisos, consolidados, histórico, análise e alertas; apagar uma análise
zera a referência na transcrição e no feedback (ON DELETE SET NULL), o que permite reprocessar sem perder a reunião nem a avaliação do usuário.</td></tr>
<tr><td>Textos longos em CLOB</td><td>Transcrição, resumos, descrições e o JSON de insights e indicadores ficam em CLOB, pois VARCHAR2 é limitado a 4000 bytes no Oracle da FIAP.</td></tr>
<tr><td>Histórico por trigger</td><td>TRG_TRANSCRICOES_STATUS_HIST e TRG_ALERTAS_STATUS_HIST gravam toda mudança de status, o que dá auditoria e rastreabilidade ao processo
sem depender da aplicação.</td></tr>
<tr><td>Índices</td><td>Colunas de filtro do dashboard (dono, status, cliente, data, segmento, UF, prioridade) e todas as chaves estrangeiras consultadas.</td></tr>
<tr><td>Comentários sem acentuação</td><td>Os textos gravados com COMMENT ON não usam acentos para que os scripts funcionem em qualquer codificação de cliente
(SQL*Plus, SQLcl ou SQL Developer). O dicionário de dados da seção 8 reproduz os mesmos textos com a grafia completa.</td></tr>
</table>
""")
    return "\n".join(h)


def scripts():
    trig = "".join("<tr><td class='mono'>%s</td><td>%s</td></tr>" % (e(t["nome"]), e(acentuar(t["comentario"]))) for t in TRIGGERS)
    n_col = sum(len(t["colunas"]) for t in TABELAS)
    n_idx = sum(len(t["indices"]) for t in TABELAS)
    return """
<h2 class='quebra'>5. Script DDL &ndash; estrutura do banco</h2>
<p>O arquivo <code>01_DDL_COMPANY404_TOTVS.sql</code> cria toda a estrutura e foi executado do zero em um banco Oracle limpo para validação.
As tabelas são criadas do pai para o filho; a chave estrangeira circular entre TRANSCRICOES e ANALISES (relacionamento 1:1) é adicionada com
ALTER TABLE ao final da seção de tabelas, pois só pode existir depois que as duas tabelas existem.</p>
<table>
<tr><th style="width:4.2cm">Conteúdo</th><th>Detalhe</th></tr>
<tr><td>Sequence</td><td>SEQ_TRANSCRICAO_PROTOCOLO, usada como DEFAULT da coluna TRANSCRICOES.NUM_PROTOCOLO.</td></tr>
<tr><td>Tabelas</td><td>%d instruções CREATE TABLE com PRIMARY KEY, FOREIGN KEY, UNIQUE, NOT NULL e CHECK declarados como restrições nomeadas; duas colunas IDENTITY nas tabelas de histórico.</td></tr>
<tr><td>ALTER TABLE</td><td>Chave estrangeira circular FK_TRANSCRICOES_ANALISE (TRANSCRICOES.ANALISE_ID &rarr; ANALISES.ID, ON DELETE SET NULL).</td></tr>
<tr><td>Índices</td><td>%d índices secundários para as consultas do dashboard.</td></tr>
<tr><td>Triggers</td><td><table style="margin:0">%s</table></td></tr>
<tr><td>Comentários</td><td>COMMENT ON em todas as %d tabelas e em todas as %d colunas (dicionário de dados na seção 8).</td></tr>
<tr><td>Conferência e limpeza</td><td>Consulta de contagem de objetos ao final e seção de limpeza (DROP) comentada para recriar o schema do zero.</td></tr>
</table>

<h2>6. Script DML &ndash; carga, atualização e exclusão</h2>
<p>O arquivo <code>02_DML_COMPANY404_TOTVS.sql</code> usa identificadores UUID fixos para que o script seja legível e reexecutável. Os dados são
fictícios, porém realistas: seguem o vocabulário da solução (produtos TOTVS Protheus, RM, Fluig, Datasul e Techfin; concorrentes Senior, Oracle
NetSuite e Bling; tipos de alerta e categorias geradas pela LeanIA).</p>
<h3>6.1 INSERT</h3>
<p>Todas as tabelas recebem no mínimo cinco registros: 5 usuários, 6 sessões, 9 reuniões de 9 clientes de segmentos diferentes (6 processadas,
1 pendente, 1 em processamento e 1 com erro), 6 análises com produtos, oportunidades, riscos e consolidados, 8 alertas, 6 feedbacks e os registros
de criação nos históricos. As reuniões são inseridas com ANALISE_ID nulo e vinculadas à análise depois da inserção em ANALISES, por causa da chave
estrangeira circular. NUM_PROTOCOLO não aparece nos INSERTs: é preenchido pelo DEFAULT da sequence.</p>
<h3>6.2 UPDATE &ndash; evolução dos processos</h3>
<table>
<tr><th style="width:4.2cm">Processo</th><th>Evolução demonstrada</th></tr>
<tr><td>Transcrição "Construtora Alvorada"</td><td>PENDENTE &rarr; PROCESSANDO &rarr; PROCESSADA. A fila pega a reunião, a LeanIA devolve a análise (INSERT em ANALISES e nos detalhes),
a transcrição recebe a análise atual, as métricas e o resumo executivo, e um alerta é gerado. O trigger registra as duas transições em TRANSCRICAO_STATUS_HIST.</td></tr>
<tr><td>Transcrição "Agro Serra Verde"</td><td>PROCESSANDO &rarr; ERRO (LeanIA indisponível) e UPDATE da observação no histórico.</td></tr>
<tr><td>Alerta de churn</td><td>NOVO &rarr; VISTO &rarr; RESOLVIDO, com observação do tratamento; outros alertas NOVO &rarr; VISTO.</td></tr>
<tr><td>Demais registros</td><td>Feedback revisado (não útil &rarr; útil), sessão revogada no logout, usuário reativado e NPS atualizado após nova pesquisa.</td></tr>
</table>
<h3>6.3 DELETE &ndash; exclusões com integridade referencial</h3>
<ul>
<li>Sessão expirada removida (limpeza de refresh tokens) e feedback enviado por engano removido.</li>
<li>Transcrição com ERRO e texto vazio removida: os avisos e o histórico saem em cascata (o script mostra a contagem antes e depois).</li>
<li>Análise apagada para reprocessamento: TRANSCRICOES.ANALISE_ID e ANALISE_FEEDBACKS.ANALISE_ID ficam NULL (SET NULL) e os detalhes saem em cascata.</li>
</ul>
<h3>6.4 Resultado da execução</h3>
<p class='semrecuo'>Contagem por tabela ao final do script (após INSERT, UPDATE e DELETE), obtida em um Oracle limpo:</p>
<table style="width:75%%">
<tr><th>Tabela</th><th style="width:2cm">Registros</th><th>Tabela</th><th style="width:2cm">Registros</th></tr>
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

<h2>7. Como executar e conferir</h2>
<ol>
<li>Abrir o SQL Developer conectado ao schema (por exemplo, RM561344 em oracle.fiap.com.br:1521/ORCL) ou o SQLcl.</li>
<li>Executar <code>01_DDL_COMPANY404_TOTVS.sql</code> como script (F5). Se o schema já tiver as tabelas, descomentar e executar antes a seção LIMPEZA, no fim do arquivo.</li>
<li>Executar <code>02_DML_COMPANY404_TOTVS.sql</code> como script (F5). As consultas de conferência mostram o histórico de status, o efeito da cascata e a contagem por tabela.</li>
<li>Abrir <code>datamodeler/TOTVS_Inteligencia_Conversacional.dmd</code> no Oracle SQL Developer Data Modeler para navegar no modelo relacional.</li>
</ol>
<p>Os dois scripts foram validados do zero em um Oracle Database Free 23ai (container Docker), sem erros, usando apenas sintaxe compatível
com o Oracle 19c da FIAP.</p>
""" % (len(TABELAS), n_idx, trig, len(TABELAS), n_col)


def dicionario():
    h = ["<h2 class='quebra'>8. Dicionário de dados</h2>",
         "<p class='semrecuo'>Todas as tabelas e colunas com tipo, obrigatoriedade, chaves e a descrição registrada no banco com COMMENT ON.</p>"]
    for t in TABELAS:
        h.append("<div class='tabnome'>%s</div><p class='tabdesc'>%s</p>" % (e(t["nome"]), e(acentuar(t["comentario"]))))
        h.append("<table><tr><th style='width:24%'>Coluna</th><th style='width:19%'>Tipo</th><th style='width:7%'>Nulo</th><th style='width:8%'>Chave</th><th>Descrição</th></tr>")
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
                e(c["nome"]), e(tipo), "não" if not c["nulo"] else "sim", " ".join(chaves), e(acentuar(c["comentario"]))))
        h.append("</table>")
        extras = ["PK %s" % e(t["pk"]["nome"])]
        extras += ["UK %s" % e(uk["nome"]) for uk in t["uks"]]
        extras += ["FK %s &rarr; %s" % (e(fk["nome"]), e(fk["ref"])) for fk in t["fks"]]
        extras += ["IDX %s" % e(i["nome"]) for i in t["indices"]]
        h.append("<p class='chaves'>Restrições e índices: %s.</p>" % "; ".join(extras))
    return "\n".join(h)


def anexo_arquivos():
    return """
<h2>9. Arquivos entregues e documentos de referência</h2>
<table>
<tr><th style="width:6.5cm">Arquivo</th><th>Conteúdo</th></tr>
<tr><td class='mono'>Company404_DatabaseDesign_Sprint3.pdf</td><td>Este documento (itens 1 e 2 da entrega: apresentação do projeto e diagrama do modelo relacional).</td></tr>
<tr><td class='mono'>01_DDL_COMPANY404_TOTVS.sql</td><td>Item 3: estrutura do banco (sequence, tabelas, restrições, identity, índices, triggers e comentários).</td></tr>
<tr><td class='mono'>02_DML_COMPANY404_TOTVS.sql</td><td>Item 4: carga (INSERT), evolução dos processos (UPDATE), exclusões (DELETE) e conferências.</td></tr>
<tr><td class='mono'>datamodeler/</td><td>Design do Oracle SQL Developer Data Modeler (arquivo .dmd e pasta) com o modelo relacional e o diagrama.</td></tr>
<tr><td class='mono'>diagramas/</td><td>Imagens do diagrama completo e das subviews em alta resolução.</td></tr>
</table>
<p class='semrecuo'><b>Documentos utilizados</b></p>
<ol>
<li>FIAP. Challenge 2026 &ndash; Engenharia de Software &ndash; TOTVS: Sprint 3 e Sprint 4. Guia de projeto, entregas, disciplinas e prazos. Orientações da disciplina Database Design para a Sprint 3.</li>
<li>Grupo Company 404. TOTVS Inteligência Conversacional &ndash; Visão do Produto e Requisitos de Software, versão 1.0, abril de 2026.</li>
<li>Oracle. SQL Developer Data Modeler 24.3 &ndash; User's Guide; Oracle Database 19c SQL Language Reference.</li>
</ol>
<p class='nota'>Repositório da solução: github.com/TLean07/totvs-conversational-ai (branch oracle). Os scripts desta entrega são os mesmos usados pelo backend.</p>
"""


def gerar_html(pasta_img):
    corpo = capa() + cabecalho() + sumario() + apresentacao() + modelo(pasta_img) + scripts() + dicionario() + anexo_arquivos()
    return "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='utf-8'><title>%s - Database Design Sprint 3</title><style>%s</style></head><body>%s</body></html>" % (
        e(SOLUCAO), CSS, corpo)


def gerar_pdf(html_path, pdf_path):
    cmd = [EDGE, "--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--print-to-pdf=%s" % pdf_path,
           "--no-first-run", "--disable-extensions", "--user-data-dir=%s" % os.path.join(os.environ.get("TEMP", "."), "edge-pdf-profile"),
           "file:///" + html_path.replace("\\", "/")]
    subprocess.run(cmd, check=True, timeout=180, capture_output=True)
    numerar_paginas(pdf_path)
    return pdf_path


def numerar_paginas(pdf_path):
    """Numera as páginas no canto inferior direito (a partir da capa), como em trabalho acadêmico."""
    import fitz
    doc = fitz.open(pdf_path)
    for i, page in enumerate(doc):
        r = page.rect
        page.insert_text((r.width - 45, r.height - 28), str(i + 1), fontsize=10, fontname="tiro", color=(0, 0, 0))
    tmp = pdf_path + ".tmp"
    doc.save(tmp)
    doc.close()
    os.replace(tmp, pdf_path)
