# -*- coding: utf-8 -*-
"""
Entrega de Agile Methodology with Squad Framework - Sprint 3 (Challenge 2026 TOTVS).
Gera o HTML no mesmo estilo acadêmico da entrega de Database Design e converte em PDF (Edge headless).
"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, "..", "..", "..", "database-design", "sprint3", "gerador"))
import documento as base  # CSS, gerar_pdf e numeração de páginas  # noqa: E402

EQUIPE = "Company 404"
SOLUCAO = "TOTVS Inteligência Conversacional"
INTEGRANTES = [
    ("Bruno Gama Masseiras", "562526"), ("Kauai Rosa de Assis Rocha", "563256"), ("Lucas Gilmar Pedro da Silva", "566231"),
    ("Leandro Afonso Silva Santos Junior", "561344"), ("Murilo da Silva Basto", "565445"),
]

CSS_EXTRA = """
.figura img { max-height: 150mm; }
table.bk { font-size: 9pt; }
table.bk td, table.bk th { padding: 3px 4px; }
table.bk td.mono { font-size: 8.5pt; white-space: nowrap; }
td.c { text-align: center; }
.mvp { font-weight: bold; }
table.retro td { width: 25%; font-size: 9.5pt; }
table.retro ul { margin: 0 0 0 0.45cm; } table.retro li { text-align: left; }
.ux td:first-child { width: 4.3cm; }
"""

e = base.e
figura = base.figura


# ------------------------------------------------------------------------------------------ dados
# Backlog: (id, épico, história, UC, MoSCoW, SP, MVP, situação)
BACKLOG = [
    ("US-01", "Autenticação", "Como usuário, quero fazer login com e-mail e senha para acessar a plataforma com segurança.", "UC01", "Must", 5, True, "Entregue (Sprint 2)"),
    ("US-02", "Autenticação", "Como usuário, quero que minha sessão seja renovada automaticamente (refresh token) sem novo login.", "UC02", "Must", 5, True, "Entregue (Sprint 2)"),
    ("US-03", "Autenticação", "Como administrador, quero cadastrar usuários, definir perfis (ADMIN/USER) e ativar ou desativar contas.", "UC03", "Should", 8, False, "Entregue (Sprint 3)"),
    ("US-04", "Transcrições", "Como Account Executive, quero submeter o texto de uma reunião para que seja analisado.", "UC04", "Must", 3, True, "Entregue (Sprint 1)"),
    ("US-05", "Transcrições", "Como Gerente de Vendas, quero listar e filtrar transcrições por cliente, vendedor, sentimento e status.", "UC07", "Must", 5, True, "Entregue (Sprint 1)"),
    ("US-06", "Transcrições", "Como usuário, quero visualizar o detalhe completo da análise de uma transcrição.", "UC08", "Must", 5, True, "Entregue (Sprint 1)"),
    ("US-07", "Transcrições", "Como usuário, quero excluir uma transcrição incorreta e sua análise associada.", "UC09", "Should", 2, False, "Entregue (Sprint 2)"),
    ("US-08", "Transcrições", "Como usuário, quero ver o status de processamento (pendente, processando, processada, erro) na listagem.", "UC07", "Must", 3, True, "Entregue (Sprint 1)"),
    ("US-09", "Transcrições", "Como Analista de BI, quero importar em lote os arquivos JSON e CSV da TOTVS, sem duplicar reuniões.", "UC05", "Must", 13, True, "Entregue (Sprint 3)"),
    ("US-10", "Transcrições", "Como Account Executive, quero enviar um áudio e receber a transcrição automática.", "UC06", "Could", 13, False, "Entregue (Sprint 3)"),
    ("US-11", "Transcrições", "Como usuário, quero reprocessar a análise de uma transcrição já existente.", "UC10", "Should", 5, False, "Entregue (Sprint 3)"),
    ("US-12", "Transcrições", "Como usuário, quero exportar a análise de uma reunião em PDF ou CSV.", "UC08", "Should", 3, False, "Entregue (Sprint 3)"),
    ("US-13", "Motor LeanIA", "Como sistema, quero classificar o sentimento geral da conversa (positivo, negativo, neutro, misto).", "UC18", "Must", 8, True, "Entregue (Sprint 1)"),
    ("US-14", "Motor LeanIA", "Como sistema, quero identificar produtos TOTVS em uso, oportunidades e concorrentes citados.", "UC19", "Must", 8, True, "Entregue (Sprint 2)"),
    ("US-15", "Motor LeanIA", "Como sistema, quero detectar oportunidades de venda com score e riscos com severidade.", "UC20", "Must", 8, True, "Entregue (Sprint 2)"),
    ("US-16", "Motor LeanIA", "Como usuário, quero ver a persona (cargo e papel) do interlocutor identificada automaticamente.", "UC21", "Should", 5, False, "Entregue (Sprint 3)"),
    ("US-17", "Motor LeanIA", "Como usuário, quero ver os valores de orçamento mencionados extraídos automaticamente.", "UC21", "Should", 3, False, "Entregue (Sprint 3)"),
    ("US-18", "Motor LeanIA", "Como CS Manager, quero um indicador de confiança no vendedor calculado a partir da conversa.", "UC21", "Should", 5, False, "Entregue (Sprint 3)"),
    ("US-19", "Motor LeanIA", "Como usuário, quero ler um resumo automático com a próxima ação comercial sugerida.", "UC21", "Should", 5, False, "Entregue (Sprint 3)"),
    ("US-20", "Motor LeanIA", "Como usuário, quero os insights além do óbvio (elogios, reclamações, dúvidas, próximos passos) e indicadores de qualidade da conversa.", "UC21", "Should", 8, False, "Entregue (Sprint 3)"),
    ("US-21", "Motor LeanIA", "Como sistema, quero processar a análise de forma assíncrona, com fila, retentativas e recuperação de falhas.", "UC17", "Must", 8, True, "Entregue (Sprint 3)"),
    ("US-22", "Alertas", "Como sistema, quero gerar alertas automáticos para riscos e oportunidades relevantes.", "UC22", "Must", 5, True, "Entregue (Sprint 2)"),
    ("US-23", "Alertas", "Como Diretor Comercial, quero listar e filtrar alertas por severidade e status.", "UC11", "Must", 3, True, "Entregue (Sprint 2)"),
    ("US-24", "Alertas", "Como usuário, quero marcar um alerta como visto ou resolvido.", "UC12", "Must", 3, True, "Entregue (Sprint 2)"),
    ("US-25", "Dashboard", "Como Diretor Comercial, quero ver métricas consolidadas (transcrições, oportunidades, riscos, alertas) em um só painel.", "UC13", "Must", 5, True, "Entregue (Sprint 2)"),
    ("US-26", "Dashboard", "Como Analista de BI, quero gráficos de sentimento, riscos por tipo e oportunidades por tipo.", "UC13", "Must", 8, True, "Entregue (Sprint 2)"),
    ("US-27", "Dashboard", "Como usuário, quero ver as transcrições recentes e os alertas recentes no dashboard.", "UC13", "Should", 3, False, "Entregue (Sprint 2)"),
    ("US-28", "Dashboard", "Como CS Manager, quero consultar as páginas de riscos e de oportunidades com evidências e recomendações.", "UC14", "Should", 5, False, "Entregue (Sprint 3)"),
    ("US-29", "Dashboard", "Como Analista de BI, quero filtrar o dashboard por período, cliente e segmento.", "UC13", "Could", 5, False, "Planejado (Sprint 4)"),
    ("US-30", "Feedback adaptativo", "Como Account Executive, quero avaliar se a análise foi útil e registrar um ajuste.", "UC15", "Should", 5, False, "Entregue (Sprint 3)"),
    ("US-31", "Feedback adaptativo", "Como sistema, quero usar o histórico de feedback como contexto nas próximas análises.", "UC16", "Could", 8, False, "Entregue (Sprint 3)"),
    ("US-32", "Mobile", "Como gestor, quero acompanhar dashboard, reuniões, alertas e insights no aplicativo Android.", "UC13", "Could", 13, False, "Entregue (Sprint 3)"),
    ("US-33", "Plataforma", "Como equipe, queremos migrar a persistência para Oracle Database com modelo relacional versionado.", "—", "Must", 13, True, "Entregue (Sprint 3)"),
    ("US-34", "Plataforma", "Como usuário, quero que o dashboard e as listas carreguem rápido mesmo com centenas de reuniões.", "UC13", "Must", 8, True, "Entregue (Sprint 3)"),
    ("US-35", "Plataforma", "Como operação, quero rastrear cada requisição entre os serviços (X-Request-ID).", "—", "Should", 3, False, "Entregue (Sprint 3)"),
    ("US-36", "Plataforma", "Como operação, quero um teste de carga de importação em lote antes de cada deploy.", "—", "Should", 5, False, "Planejado (Sprint 4)"),
    ("US-37", "Plataforma", "Como equipe, queremos integrar um modelo de classificação supervisionado à LeanIA.", "UC17", "Won't", 13, False, "Fora do escopo desta fase"),
]

# rodadas de Planning Poker registradas para as histórias com maior divergência
POKER = [
    ("US-09 Importar em lote (JSON/CSV)", [8, 21, 13, 13, 8], 13, "Quem votou 21 lembrou dos três formatos de JSON e das codificações do CSV; quem votou 8 considerou só o formato principal."),
    ("US-10 Enviar áudio e transcrever", [5, 13, 8, 13, 13], 13, "Dependência de um serviço externo de transcrição e tratamento de arquivos grandes justificaram o valor maior."),
    ("US-21 Análise assíncrona com fila e recuperação", [5, 8, 3, 8, 13], 8, "O incidente de pool esgotado em produção mostrou que fila, retentativa e recuperação são parte da história."),
    ("US-33 Migrar para Oracle", [8, 13, 21, 13, 13], 13, "Além do schema, exigiu reescrever repositórios, converter listas embutidas em tabelas filhas e revalidar a aplicação."),
    ("US-34 Dashboard rápido em escala", [5, 13, 8, 8, 13], 8, "Cache, projeções e ajustes no driver foram considerados um único incremento após a discussão."),
]

UCS = [
    ("UC01", "Autenticar-se (login/logout)", "Usuário autenticado", "include UC02", "RF de segurança (JWT)"),
    ("UC02", "Renovar sessão (refresh token)", "Sistema", "incluído por UC01", "RF de segurança (cookie HttpOnly)"),
    ("UC03", "Gerenciar usuários e perfis", "Administrador", "—", "Governança de acesso (novo)"),
    ("UC04", "Submeter transcrição (texto)", "Account Executive, Gerente de Vendas", "include UC17", "RF-01, RF-02"),
    ("UC05", "Importar em lote (JSON/CSV TOTVS)", "Analista de BI", "include UC17", "Novo (validado com dado real)"),
    ("UC06", "Enviar áudio para transcrição", "Account Executive; serviço de transcrição", "extend UC04", "Novo (canal opcional)"),
    ("UC07", "Listar e filtrar transcrições", "Usuário autenticado", "—", "RF-03, RF-06"),
    ("UC08", "Visualizar detalhe da análise (com exportação PDF/CSV)", "Usuário autenticado", "—", "RF-04, RF-15"),
    ("UC09", "Excluir transcrição", "Gerente de Vendas", "—", "RF-05"),
    ("UC10", "Reprocessar análise", "Gerente de Vendas", "include UC17", "Novo"),
    ("UC11", "Listar e filtrar alertas", "Diretor Comercial, CS Manager", "—", "RF-18"),
    ("UC12", "Marcar alerta como visto/resolvido", "Diretor Comercial, CS Manager", "—", "RF-19"),
    ("UC13", "Visualizar dashboard executivo", "Usuário autenticado", "extend UC14", "RF-20 a RF-25"),
    ("UC14", "Consultar insights de riscos e oportunidades", "CS Manager, Analista de BI", "estende UC13", "Consolidado v2"),
    ("UC15", "Avaliar análise (útil/ajuste)", "Account Executive", "include UC16", "Novo (aprendizado contínuo)"),
    ("UC16", "Ajustar contexto adaptativo da LeanIA", "Sistema", "extend UC17", "Novo"),
    ("UC17", "Analisar transcrição (orquestração)", "LeanIA", "include UC18 a UC21", "RF-16"),
    ("UC18", "Classificar sentimento", "LeanIA", "incluído por UC17", "RF-07"),
    ("UC19", "Identificar produtos e concorrentes", "LeanIA", "incluído por UC17", "RF-08, RF-09"),
    ("UC20", "Detectar oportunidades e riscos", "LeanIA", "incluído por UC17; include UC22", "RF-10, RF-11"),
    ("UC21", "Gerar resumo, persona, budget e próxima ação", "LeanIA", "incluído por UC17", "RF-12 a RF-15"),
    ("UC22", "Gerar alertas", "LeanIA", "incluído por UC20", "RF-17"),
]

TELAS = [
    ("01_login.png", "Tela de login", [
        ("Adequação ao público-alvo", "A tela apresenta a proposta em uma frase (“Inteligência conversacional para times que precisam agir rápido”) e três selos de confiança (dados protegidos, acesso autenticado, IA integrada), o que fala diretamente com o executivo comercial que precisa confiar na plataforma antes de usá-la."),
        ("Facilidade de aprendizado", "Apenas dois campos e um botão de ação primária (“Entrar”); não há jargão técnico e os exemplos de preenchimento aparecem dentro dos campos."),
        ("Facilidade de operação", "Formulário curto, em um único passo, com validação de e-mail no próprio campo e foco no primeiro campo ao abrir a página."),
        ("Navegabilidade", "Depois do login o usuário vai direto ao dashboard, que é o ponto de partida de todas as jornadas; a barra superior fica visível em todas as telas."),
        ("UX Writing", "Títulos em linguagem de convite (“Entrar na plataforma”, “Use suas credenciais para acessar o painel”), sem termos como “autenticação” ou “credencial inválida” no fluxo normal."),
    ]),
    ("02_dashboard.png", "Dashboard executivo", [
        ("Adequação ao público-alvo", "Diretores e gestores veem os seis indicadores principais (transcrições, processadas, bases consolidadas, oportunidades, riscos e alertas novos) na primeira dobra, com os números reais da base (623 reuniões, 960 oportunidades, 579 riscos, 244 alertas)."),
        ("Facilidade de aprendizado", "Cores consistentes em todo o produto: vermelho para risco, verde para oportunidade, azul para alerta e âmbar para sentimento misto; cada cartão traz um subtítulo explicando o que o número significa."),
        ("Facilidade de operação", "As duas ações mais frequentes (“Nova transcrição” e “Atualizar”) ficam no cabeçalho; a data da última atualização informa se os dados estão em dia."),
        ("Navegabilidade", "Os blocos “Alertas recentes” e “Transcrições recentes” levam direto ao alerta ou à reunião, sem trocar de menu; o campo “Buscar cliente” antecipa o filtro mais usado."),
        ("UX Writing", "Rótulos em linguagem de negócio (“Riscos consolidados”, “Categorias de risco agrupadas para priorização”) em vez de nomes de campos do sistema."),
    ]),
    ("03_transcricoes.png", "Gestão de transcrições", [
        ("Adequação ao público-alvo", "Vendedores e gestores acompanham muitas reuniões; cada cartão mostra cliente, vendedor, origem (JSON TOTVS), status, sentimento e a contagem de oportunidades, riscos e produtos, sem abrir o detalhe."),
        ("Facilidade de aprendizado", "Três ações de entrada claramente separadas (“Enviar áudio”, “Importar JSON TOTVS” e “Nova transcrição”), e filtros de sentimento e status em botões de seleção única."),
        ("Facilidade de operação", "Filtro por cliente e por vendedor no topo, sempre visível; o trecho inicial da conversa aparece no cartão para reconhecer a reunião rapidamente."),
        ("Navegabilidade", "Cada cartão é um link para o detalhe; a lista é paginada e a barra superior mantém o contexto (a seção ativa fica destacada)."),
        ("UX Writing", "Estados escritos como situações (“Processada”, “Pendente”, “Erro”) e sentimentos com uma palavra (“Misto”, “Negativo”), fáceis de escanear."),
    ]),
    ("v_04_analise_a.png", "Detalhe da análise (resumo, persona, budget e feedback)", [
        ("Adequação ao público-alvo", "O resumo da análise abre com o que o vendedor precisa saber: produto em uso, sentimento, probabilidade de venda, interlocutor, budget e próxima ação; os três cartões destacam persona, budget e confiança no vendedor."),
        ("Facilidade de aprendizado", "A seção “Além do óbvio” organiza o que o cliente disse em quatro caixas com nomes autoexplicativos (próximos passos, dúvidas da jornada, reclamações, elogios)."),
        ("Facilidade de operação", "O feedback para a LeanIA fica ao lado do resumo, com dois botões (“Útil” e “Precisa ajuste”) e um campo de texto com exemplo de preenchimento; o histórico de feedback aparece logo abaixo."),
        ("Navegabilidade", "Link “Voltar para Transcrições” no topo e botões de exportação (CSV, PDF) no cabeçalho do cartão; a transcrição original fica em um bloco rolável, sem competir com a análise."),
        ("UX Writing", "A próxima ação é escrita como instrução (“Enviar proposta ou agendar demonstração com os decisores nos próximos dias”), e o texto de apoio do feedback explica o efeito (“Esse retorno entra no contexto das próximas análises”)."),
    ]),
    ("v_04_analise_b.png", "Detalhe da análise (qualidade da conversa, produtos, oportunidades, riscos e alertas)", [
        ("Adequação ao público-alvo", "Os indicadores de qualidade (escuta ativa, participação de vendedor e cliente, perguntas) apoiam o gestor de vendas no coaching do time, um uso que surgiu com a LeanIA v2."),
        ("Facilidade de aprendizado", "Cada oportunidade e risco mostra a categoria, o score ou a severidade, o trecho da conversa que serviu de evidência e uma recomendação, no mesmo formato em todas as telas."),
        ("Facilidade de operação", "Os blocos são independentes e colapsam bem em telas menores; o número de ocorrências internas evita que o usuário precise contar."),
        ("Navegabilidade", "Os alertas gerados aparecem ao final do detalhe e também na central de alertas, então o usuário pode agir de qualquer um dos dois lugares."),
        ("UX Writing", "Recomendações escritas como ação (“Validar aderência, estimar valor e encaminhar proposta”), e categorias em português de negócio (“Insatisfação com processos”)."),
    ]),
    ("05_alertas.png", "Central de alertas", [
        ("Adequação ao público-alvo", "CS Managers e diretores precisam agir rápido: o filtro de severidade e a alternância entre “Novos” e “Todos” ficam no topo, e os três cartões de resumo mostram o tamanho da fila."),
        ("Facilidade de aprendizado", "A severidade aparece como etiqueta colorida (“Alto”) e o tipo do alerta em uma etiqueta separada; o motivo e a recomendação têm caixas próprias nos alertas consolidados."),
        ("Facilidade de operação", "As duas ações possíveis (“Visto” e “Resolver”) ficam sempre no mesmo lugar do cartão, à direita, reduzindo o tempo de triagem em listas longas."),
        ("Navegabilidade", "Cada alerta identifica o cliente e o trecho da reunião que o originou, evitando abrir a transcrição para decidir; a data e hora de geração ficam no rodapé do cartão."),
        ("UX Writing", "A mensagem do alerta é uma frase de recomendação com a evidência entre aspas, e não um registro técnico; próximo passo: traduzir os códigos de tipo (RISCO_CHURN) para rótulos em linguagem natural, item já no backlog da Sprint 4."),
    ]),
]


# ------------------------------------------------------------------------------------------ seções
def capa():
    integrantes = "<br>".join("%s &ndash; RM %s" % (e(n), rm) for n, rm in INTEGRANTES)
    return """
<div class="capa">
  <div class="inst">FIAP &ndash; Faculdade de Informática e Administração Paulista<br>Engenharia de Software &ndash; 2&ordm; ano<br>Challenge 2026 &ndash; TOTVS</div>
  <div class="tit">
    <div class="t1">%s</div>
    <div class="t2">Plataforma de análise automatizada de reuniões comerciais com inteligência artificial</div>
    <div class="t3"><b>Agile Methodology with Squad Framework &ndash; Sprint 3</b><br>Refino da visão do produto, casos de uso, backlog priorizado, protótipo de telas e retrospectiva</div>
  </div>
  <div class="grupo"><b>Grupo %s</b><br>%s</div>
  <div class="rod">São Paulo<br>Setembro de 2026</div>
</div>
""" % (e(SOLUCAO), e(EQUIPE), integrantes)


def cabecalho():
    linhas = "<br>".join("%s &ndash; RM: %s" % (e(n), rm) for n, rm in INTEGRANTES)
    itens = [("1.", "Refino da visão do produto e descrição da solução"), ("2.", "Diagrama de casos de uso completo"),
             ("3.", "Backlog refinado, priorizado (MoSCoW) e estimado (Planning Poker)"), ("4.", "Protótipo de telas e comentários de UX"),
             ("5.", "Retrospectiva da Sprint 3 como insumo para a Sprint 4"), ("6.", "Documentos de referência")]
    sumario = "".join("<tr><td style='width:1.2cm'>%s</td><td>%s</td></tr>" % (n, t) for n, t in itens)
    return """
<div class="cab"><b>Grupo %s</b><br>%s<br>Agile Methodology with Squad Framework &ndash; Challenge 2026 TOTVS &ndash; Sprint 3 &ndash; 2&ordm; ano</div>
<h1>Refino do produto %s &ndash; Sprint 3</h1>
<h2>Sumário</h2><table class='sumario'>%s</table>
""" % (e(EQUIPE), linhas, e(SOLUCAO), sumario)


def visao(pasta):
    return """
<h2>1. Refino da visão do produto e descrição da solução</h2>
<p>Este capítulo compara a visão do produto apresentada nas Sprints 1 e 2 (documento de Visão e Requisitos, versão 1.0, abril de 2026)
com a visão refinada após a Sprint 3, indicando o que mudou, por que mudou e qual é o estado atual da solução.</p>
<h3>1.1 Declaração de visão (formato Moore)</h3>
<table>
<tr><th style="width:2.6cm">Versão</th><th>Declaração</th></tr>
<tr><td>1.0 (abril de 2026)</td><td>Para as equipes comerciais e de Customer Success da TOTVS que precisam transformar milhares de reuniões diárias em inteligência de negócio
acionável, o TOTVS Inteligência Conversacional é uma plataforma de análise automatizada de transcrições que detecta oportunidades de venda, riscos de churn e sinais
de insatisfação em tempo real, por meio de IA local e regras de negócio especializadas. Diferente de soluções genéricas de CRM ou transcrição, nosso produto
entende o ecossistema de produtos TOTVS e o contexto competitivo do mercado de ERP brasileiro, entregando alertas priorizados e dashboards analíticos
diretamente para líderes comerciais, sem depender de APIs pagas ou exposição de dados sensíveis de clientes.</td></tr>
<tr><td>2.0 (setembro de 2026, refinada)</td><td>Para equipes comerciais, diretores e gerentes de Customer Success da TOTVS, que precisam identificar, de forma rápida,
segura e escalável, oportunidades de receita e riscos de perda de clientes a partir de reuniões gravadas, o TOTVS Inteligência Conversacional é uma
plataforma de análise automatizada de transcrições com IA própria (LeanIA), <b>autenticação por perfis, ingestão em lote dos arquivos da TOTVS e
aplicativo móvel</b>, que detecta oportunidades de upsell e cross-sell, riscos de churn e menções a concorrentes, <b>identifica a persona e o orçamento
do interlocutor e sugere a próxima ação comercial</b>, gerando alertas priorizados e um dashboard executivo <b>validado com uma base real de 623
reuniões</b>. Diferente de ferramentas genéricas de CRM ou de transcrição, nosso produto opera sem custo de API, com conhecimento embutido do
ecossistema TOTVS, <b>aprende com o feedback dos usuários</b> e mantém os dados dos clientes em um banco corporativo Oracle.</td></tr>
</table>
<p class="nota">Os trechos em negrito são os pontos acrescentados ou reforçados em relação à declaração original.</p>

<h3>1.2 Quadro de mudanças em relação à visão 1.0</h3>
<table>
<tr><th style="width:2.8cm">Dimensão</th><th style="width:4.6cm">Visão 1.0</th><th>Visão 2.0 (refinada)</th><th style="width:4.2cm">Motivo da mudança</th></tr>
<tr><td>Persistência</td><td>Não definia o banco; o protótipo usava MongoDB.</td><td>Migração completa para Oracle Database 19c (instância da FIAP), com modelo relacional
de 20 tabelas, scripts versionados e validação da estrutura na subida da aplicação.</td><td>Aderência ao padrão corporativo e integridade referencial entre transcrição, análise, riscos e alertas.</td></tr>
<tr><td>Segurança e acesso</td><td>Autenticação implícita, não tratada na visão.</td><td>Login com JWT e refresh token em cookie HttpOnly, perfis ADMIN e USER, tela de gestão de usuários,
limite de tentativas e validação de origem.</td><td>Dados de clientes e do pipeline comercial exigem controle de acesso.</td></tr>
<tr><td>Ingestão</td><td>Envio manual de texto por formulário ou API.</td><td>Importação em lote dos arquivos JSON e CSV da TOTVS (detecção de formato e codificação, sem duplicar
reuniões) e envio de áudio com transcrição automática.</td><td>O arquivo real da TOTVS (1.174 registros, 48 MB) mostrou que a entrada manual não escala.</td></tr>
<tr><td>Motor de IA</td><td>Modelo de sentimento e regras simples por palavra-chave.</td><td>LeanIA v2: análise por locutor, sentença e oração; papéis de vendedor e cliente; sentimento por área;
léxico com negação e intensificadores; persona, budget, confiança no vendedor, próxima ação e insights além do óbvio.</td><td>O motor v1 gerava falsos positivos nas
transcrições reais; o refino seguiu o exemplo de referência do briefing.</td></tr>
<tr><td>Aprendizado contínuo</td><td>Não previsto.</td><td>Feedback adaptativo: o usuário avalia a análise e o histórico entra como contexto nas próximas execuções.</td>
<td>Diferencial identificado para reduzir falsos positivos ao longo do tempo.</td></tr>
<tr><td>Desempenho</td><td>Meta de dashboard em até 2 segundos, sem estratégia definida.</td><td>Cache de leitura, consultas por projeção, carregamento sob demanda e ajustes do driver
Oracle; dashboard e listas abaixo de 1 segundo com o cache aquecido.</td><td>Incidente na importação em massa (pool de conexões esgotado) exigiu arquitetura de leitura dedicada.</td></tr>
<tr><td>Resiliência</td><td>Retentativa genérica.</td><td>Fila assíncrona com retentativas, job de recuperação de análises presas e keep-alive entre os serviços gratuitos.</td>
<td>Aprendizado direto do incidente de produção.</td></tr>
<tr><td>Canais</td><td>Aplicação web.</td><td>Aplicação web e aplicativo Android com dashboard, reuniões, alertas e insights, usando a mesma API.</td>
<td>Gestores consultam alertas fora do escritório.</td></tr>
<tr><td>Personas</td><td>Diretor Comercial, Gerente de Vendas, Account Executive, CS Manager e Analista de BI.</td><td>Mantidas as cinco personas e acrescentado o Administrador
(gestão de usuários e perfis).</td><td>Necessidade operacional da autenticação multiusuário.</td></tr>
</table>

<h3>1.3 Quadro É / Não É / Faz / Não Faz (refinado)</h3>
<table>
<tr><th style="width:25%%">É</th><th style="width:25%%">Não é</th><th style="width:25%%">Faz</th><th>Não faz</th></tr>
<tr><td>Ferramenta de apoio à decisão comercial</td><td>Substituta do vendedor ou do gestor</td><td>Identifica produtos, concorrentes, oportunidades, riscos, persona e budget</td><td>Fecha vendas ou envia propostas ao cliente</td></tr>
<tr><td>Analisador de transcrições com IA própria</td><td>Gravador ou transcritor obrigatório de reuniões</td><td>Classifica o sentimento geral e por área e sugere a próxima ação</td><td>Toma decisões sozinha ou altera contratos</td></tr>
<tr><td>Central de alertas e dashboard executivo</td><td>CRM ou sistema de gestão de pipeline</td><td>Gera alertas priorizados e mantém histórico de tratamento</td><td>Garante a previsão de churn</td></tr>
<tr><td>Plataforma multiusuário (web e Android)</td><td>Serviço baseado em APIs pagas de IA</td><td>Importa em lote os arquivos da TOTVS e aprende com o feedback</td><td>Envia dados de clientes a terceiros</td></tr>
</table>

<h3>1.4 Descrição da solução</h3>
<p>O TOTVS Inteligência Conversacional é composto por três serviços independentes e um banco relacional. O <b>frontend</b> (Next.js, publicado na Vercel) oferece
dashboard executivo, gestão de transcrições, central de alertas, páginas de riscos e oportunidades e administração de usuários. O <b>backend</b> (Java 17 e Spring
Boot, publicado na Render) expõe a API REST, autentica os usuários, persiste os dados no Oracle, orquestra a análise assíncrona, gera os alertas e mantém o cache
de leitura. A <b>LeanIA</b> (Python e FastAPI, publicada no Hugging Face Spaces) recebe o texto da reunião e devolve sentimento, produtos, concorrentes, oportunidades,
riscos, persona, budget, próxima ação e insights. O <b>aplicativo Android</b> consome a mesma API. O <b>Oracle Database 19c</b> da FIAP guarda usuários, sessões,
transcrições, análises, alertas, feedbacks e o histórico de status.</p>
%s
<h3>1.5 Personas e uso principal</h3>
<table>
<tr><th style="width:3.6cm">Persona</th><th>Uso principal na plataforma</th></tr>
<tr><td>Diretor Comercial</td><td>Acompanha os indicadores de oportunidades e riscos no dashboard e trata os alertas de alta prioridade.</td></tr>
<tr><td>Gerente de Vendas</td><td>Monitora as transcrições do time, filtra por cliente e vendedor, reprocessa análises e orienta a estratégia comercial.</td></tr>
<tr><td>Account Executive</td><td>Registra a reunião (texto ou áudio), lê a análise detalhada, exporta o relatório e avalia a qualidade da análise.</td></tr>
<tr><td>CS Manager</td><td>Foca nos riscos de churn e insatisfação, consulta a página de riscos e aciona planos de retenção a partir dos alertas.</td></tr>
<tr><td>Analista de BI</td><td>Importa os arquivos da TOTVS em lote e extrai métricas de sentimento, riscos e oportunidades.</td></tr>
<tr><td>Administrador (novo)</td><td>Cadastra usuários, define perfis e ativa ou desativa contas.</td></tr>
</table>
""" % figura(os.path.join(pasta, "arquitetura.png"), "Figura 1 &ndash; Arquitetura de alto nível da solução.")


def casos_uso(pasta):
    linhas = "".join("<tr><td class='mono'>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>" % tuple(e(x) for x in uc) for uc in UCS)
    return """
<h2>2. Diagrama de casos de uso completo</h2>
<p>O diagrama da Figura 2 consolida os atores e os casos de uso do sistema após a Sprint 3. Os cinco perfis de negócio herdam, por generalização,
os casos de uso do ator <b>Usuário autenticado</b> (autenticar-se, listar transcrições, ver o detalhe e o dashboard) e possuem casos de uso próprios.
A <b>LeanIA</b> e o <b>serviço de transcrição de áudio</b> aparecem como atores secundários (sistemas). As relações «include» ligam a submissão, a
importação e o reprocessamento à análise; a análise inclui a classificação de sentimento, a identificação de produtos e concorrentes, a detecção de
oportunidades e riscos e a geração do resumo; os alertas são gerados a partir das oportunidades e riscos detectados.</p>
<div class='paisagem'>%s</div>
<h3>2.1 Atores</h3>
<table>
<tr><th style="width:4.2cm">Ator</th><th style="width:2.4cm">Tipo</th><th>Descrição</th></tr>
<tr><td>Usuário autenticado</td><td>Primário (geral)</td><td>Qualquer pessoa com login válido; concentra os casos de uso comuns a todos os perfis.</td></tr>
<tr><td>Diretor Comercial</td><td>Primário</td><td>Consome o dashboard executivo e trata alertas de alta prioridade.</td></tr>
<tr><td>Gerente de Vendas</td><td>Primário</td><td>Gerencia as transcrições do time (submete, exclui, reprocessa).</td></tr>
<tr><td>Account Executive</td><td>Primário</td><td>Submete reuniões (texto ou áudio), consulta a análise e fornece feedback.</td></tr>
<tr><td>CS Manager</td><td>Primário</td><td>Atua sobre riscos de churn e insatisfação e sobre os alertas relacionados.</td></tr>
<tr><td>Analista de BI</td><td>Primário</td><td>Importa os arquivos da TOTVS em lote e consulta os insights consolidados.</td></tr>
<tr><td>Administrador</td><td>Primário</td><td>Gerencia usuários e perfis de acesso.</td></tr>
<tr><td>LeanIA</td><td>Secundário (sistema)</td><td>Executa a análise: sentimento, entidades, oportunidades, riscos, resumo e alertas.</td></tr>
<tr><td>Serviço de transcrição de áudio</td><td>Secundário (sistema)</td><td>Transcreve o áudio enviado quando o provedor está habilitado.</td></tr>
</table>
<h3>2.2 Casos de uso e requisitos relacionados</h3>
<table class="bk">
<tr><th style="width:1.3cm">ID</th><th>Caso de uso</th><th style="width:3.6cm">Ator principal</th><th style="width:3.2cm">Relação</th><th style="width:3.4cm">Requisitos</th></tr>
%s
</table>
""" % (figura(os.path.join(pasta, "casos_de_uso.png"), "Figura 2 &ndash; Diagrama de casos de uso do TOTVS Inteligência Conversacional após a Sprint 3."), linhas)


def backlog():
    linhas = []
    for uid, epico, hist, uc, mos, sp, mvp, sit in BACKLOG:
        linhas.append("<tr><td class='mono'>%s</td><td>%s</td><td>%s</td><td class='c mono'>%s</td><td class='c'>%s</td><td class='c'>%d</td><td class='c %s'>%s</td><td>%s</td></tr>" % (
            uid, e(epico), e(hist), uc, mos, sp, "mvp" if mvp else "", "MVP" if mvp else "—", e(sit)))
    poker = "".join("<tr><td>%s</td>%s<td class='c'><b>%d</b></td><td>%s</td></tr>" % (
        e(h), "".join("<td class='c'>%d</td>" % v for v in votos), fim, e(just)) for h, votos, fim, just in POKER)
    total = sum(sp for *_, sp, _, _ in [(b[0], b[5], b[6], b[7]) for b in BACKLOG]) if False else sum(b[5] for b in BACKLOG)
    mvp_pts = sum(b[5] for b in BACKLOG if b[6])
    mvp_n = sum(1 for b in BACKLOG if b[6])
    por_mos = {}
    for b in BACKLOG:
        por_mos.setdefault(b[4], [0, 0])
        por_mos[b[4]][0] += 1
        por_mos[b[4]][1] += b[5]
    resumo = "".join("<tr><td>%s</td><td class='c'>%d</td><td class='c'>%d</td></tr>" % (m, por_mos[m][0], por_mos[m][1]) for m in ["Must", "Should", "Could", "Won't"])
    entregue = {}
    for b in BACKLOG:
        if b[7].startswith("Entregue"):
            s = b[7].split("(")[1].rstrip(")")
            entregue.setdefault(s, [0, 0])
            entregue[s][0] += 1
            entregue[s][1] += b[5]
    veloc = "".join("<tr><td>%s</td><td class='c'>%d</td><td class='c'>%d</td></tr>" % (s, entregue[s][0], entregue[s][1]) for s in ["Sprint 1", "Sprint 2", "Sprint 3"])
    return """
<h2 class='quebra'>3. Backlog refinado, priorizado (MoSCoW) e estimado (Planning Poker)</h2>
<p>O backlog foi refinado a partir dos requisitos do documento de visão, dos casos de uso da seção 2 e do que foi construído até a Sprint 3.
Cada história tem o caso de uso que a realiza, a prioridade pelo método <b>MoSCoW</b> (Must, Should, Could, Won't have), a estimativa em
<b>Story Points</b> obtida com <b>Planning Poker</b> e a situação atual. As histórias marcadas como <b>MVP</b> formam o produto mínimo viável.</p>
<h3>3.1 Método de estimativa</h3>
<ol>
<li>O Product Owner da rodada (papel que se alterna entre os cinco integrantes) lê a história e os critérios de aceite.</li>
<li>Cada integrante escolhe, em sigilo, uma carta da sequência de Fibonacci (1, 2, 3, 5, 8, 13, 21).</li>
<li>Todos revelam ao mesmo tempo, para evitar ancoragem no voto de quem fala primeiro.</li>
<li>Quem deu o menor e o maior valor explica o raciocínio; a equipe discute o escopo e as dependências.</li>
<li>Nova rodada até convergir; o valor de consenso vai para o backlog junto com a classificação MoSCoW.</li>
</ol>
<p class='semrecuo'>A tabela abaixo registra as rodadas das cinco histórias com maior divergência na primeira votação.</p>
<table class="bk">
<tr><th>História</th><th class='c'>Voto 1</th><th class='c'>Voto 2</th><th class='c'>Voto 3</th><th class='c'>Voto 4</th><th class='c'>Voto 5</th><th class='c'>Consenso</th><th style="width:6.5cm">Argumento decisivo</th></tr>
%s
</table>
<h3>3.2 Backlog priorizado</h3>
<table class="bk">
<tr><th style="width:1.55cm">ID</th><th style="width:2cm">Épico</th><th>História do usuário</th><th style="width:1.1cm">UC</th><th style="width:1.3cm">MoSCoW</th><th style="width:0.8cm">SP</th><th style="width:1cm">MVP</th><th style="width:2.8cm">Situação</th></tr>
%s
</table>
<h3>3.3 Resumo do backlog e do MVP</h3>
<table style="width:60%%">
<tr><th>Prioridade</th><th class='c'>Histórias</th><th class='c'>Story Points</th></tr>%s
<tr><td><b>Total</b></td><td class='c'><b>%d</b></td><td class='c'><b>%d</b></td></tr>
</table>
<p>O <b>MVP</b> reúne <b>%d histórias e %d Story Points</b>: autenticação, ciclo completo de transcrição, análise, alerta e dashboard, importação em lote,
persistência no Oracle e a base de desempenho e resiliência validada em produção. As histórias fora do MVP (persona, budget, confiança no vendedor,
insights, feedback adaptativo, áudio, aplicativo Android, exportação) são incrementos entregues sobre essa base; as histórias planejadas para a Sprint 4
são os filtros do dashboard e o teste de carga de importação.</p>
<table style="width:60%%">
<tr><th>Sprint</th><th class='c'>Histórias entregues</th><th class='c'>Story Points entregues</th></tr>%s
</table>
""" % (poker, "".join(linhas), resumo, len(BACKLOG), total, mvp_n, mvp_pts, veloc)


def prototipo(pasta_telas):
    h = ["<h2 class='quebra'>4. Protótipo de telas e comentários de UX</h2>",
         "<p>As telas a seguir são capturas da aplicação web em produção (Vercel), com os dados reais da base analisada, e não esboços:"
         " o protótipo de alta fidelidade da Sprint 1 evoluiu para o produto em uso. Para cada tela, os comentários seguem os critérios propostos"
         " na disciplina: adequação ao público-alvo, facilidade de aprendizado, facilidade de operação, navegabilidade e UX Writing.</p>",
         "<p>Identidade visual: azul-marinho TOTVS na barra de navegação e nos títulos, fundo claro, cartões com cantos arredondados e uma cor por"
         " significado (vermelho para risco, verde para oportunidade, azul para alerta, âmbar para sentimento misto), mantida em todas as telas e no aplicativo Android.</p>"]
    for i, (arq, titulo, itens) in enumerate(TELAS):
        h.append("<h3>4.%d %s</h3>" % (i + 1, e(titulo)))
        h.append(figura(os.path.join(pasta_telas, "v_" + arq if not arq.startswith("v_") else arq), "Figura %d &ndash; %s (captura da aplicação em produção)." % (i + 3, e(titulo))))
        h.append("<table class='ux'>" + "".join("<tr><td><b>%s</b></td><td>%s</td></tr>" % (e(a), e(b)) for a, b in itens) + "</table>")
    return "\n".join(h)


def retrospectiva():
    return """
<h2 class='quebra'>5. Retrospectiva da Sprint 3 como insumo para a Sprint 4</h2>
<h3>5.1 Objetivo da Sprint 3</h3>
<p>Migrar a persistência para o Oracle Database 19c sem regressão funcional, evoluir a LeanIA para a versão 2 (persona, budget, confiança no vendedor,
próxima ação e insights), viabilizar a importação em lote dos dados reais da TOTVS, publicar o aplicativo Android e entregar o Database Design completo,
mantendo a plataforma no ar em ambiente gratuito (Vercel, Render e Hugging Face Spaces).</p>
<h3>5.2 O que foi entregue</h3>
<ul>
<li>Migração completa de MongoDB para Oracle 19c: 20 tabelas com integridade referencial, scripts DDL e DML versionados, modelo no Data Modeler e validação da estrutura na subida da aplicação.</li>
<li>LeanIA v2 validada com o exemplo de referência do briefing e com testes automatizados em Python e Java.</li>
<li>Importação dos arquivos JSON e CSV da TOTVS: 1.174 registros processados, resultando em 623 reuniões únicas analisadas, 960 oportunidades, 579 riscos e 244 alertas consolidados em produção.</li>
<li>Gestão de usuários com perfis, feedback adaptativo, reprocessamento, exportação da análise e envio de áudio.</li>
<li>Aplicativo Android com dashboard, reuniões, alertas e insights, com cache local.</li>
<li>Cache de leitura, consultas por projeção e ajustes do driver, levando o dashboard de mais de 2 minutos para menos de 1 segundo com cache aquecido.</li>
<li>Fila de análise com retentativas, job de recuperação de análises presas e keep-alive entre os serviços gratuitos.</li>
</ul>
<h3 class='quebra'>5.3 Retrospectiva</h3>
<table class="retro">
<tr><th>O que funcionou bem</th><th>O que não funcionou bem</th><th>O que aprendemos</th><th>Ações para a Sprint 4</th></tr>
<tr>
<td><ul>
<li>Validar a LeanIA v2 com o exemplo real do briefing antes de generalizar as regras.</li>
<li>Gerar DDL, DML, diagrama e documento a partir de um único modelo, o que evitou divergências.</li>
<li>Resolver a hibernação dos serviços gratuitos com keep-alive, sem custo.</li>
<li>Dividir a equipe por disciplina com integração diária pelo repositório.</li>
</ul></td>
<td><ul>
<li>A importação de 1.174 reuniões em produção esgotou o pool de conexões e deixou 561 análises presas; o dashboard ficou indisponível.</li>
<li>Uma variável de ambiente desatualizada no serviço da LeanIA fez a análise cair no motor de reserva por dias sem aviso.</li>
<li>O trabalho emergencial de infraestrutura quase adiou as histórias de persona, budget e confiança no vendedor.</li>
<li>Não medimos a velocidade (Story Points por sprint) durante as Sprints 1 e 2; a série começa nesta entrega.</li>
</ul></td>
<td><ul>
<li>Testar carga com dado real antes de colocar em produção; o volume da TOTVS é diferente do volume dos testes.</li>
<li>Nunca carregar todos os registros de uma vez: consultas de resumo por tabela são a regra.</li>
<li>Configuração de ambiente faz parte da entrega e precisa de checagem automática.</li>
<li>Registrar votos do Planning Poker e histórias entregues por sprint torna a retrospectiva objetiva.</li>
</ul></td>
<td><ul>
<li>Teste de carga de importação em lote antes de cada deploy (US-36).</li>
<li>Filtros de período, cliente e segmento no dashboard (US-29).</li>
<li>Traduzir os códigos de tipo de alerta para rótulos em linguagem de negócio.</li>
<li>Produzir os artefatos da Sprint 4: casos de uso finais, diagramas de atividades e de sequência das duas funcionalidades principais, vídeo do MVP, TCO e relatório de review.</li>
</ul></td>
</tr>
</table>
<h3>5.4 Planejamento da Sprint 4</h3>
<table>
<tr><th style="width:1.4cm">ID</th><th>Item</th><th style="width:1.6cm">MoSCoW</th><th style="width:0.9cm">SP</th></tr>
<tr><td>US-36</td><td>Teste de carga de importação em lote (regressão do incidente de pool esgotado)</td><td>Must</td><td class='c'>5</td></tr>
<tr><td>US-29</td><td>Filtrar o dashboard por período, cliente e segmento</td><td>Should</td><td class='c'>5</td></tr>
<tr><td>US-38</td><td>Rótulos em linguagem de negócio para os tipos de alerta na central de alertas</td><td>Should</td><td class='c'>2</td></tr>
<tr><td>US-39</td><td>Consultas gerenciais em SQL (Sprint 4 de Database Design) integradas ao dashboard</td><td>Should</td><td class='c'>5</td></tr>
<tr><td>US-40</td><td>Vídeo de demonstração do MVP e relatório de review (proposta versus entregue)</td><td>Must</td><td class='c'>3</td></tr>
<tr><td>US-41</td><td>Diagramas de atividades e de sequência da análise de transcrição e da central de alertas</td><td>Must</td><td class='c'>3</td></tr>
<tr><td>US-42</td><td>Visão de negócio do produto com TCO (custo total de propriedade)</td><td>Must</td><td class='c'>3</td></tr>
</table>
<p>Capacidade planejada: 26 Story Points, abaixo dos %d pontos entregues na Sprint 3, porque a Sprint 4 concentra as entregas de encerramento
(documentação, vídeo e apresentação) e as demais disciplinas do Challenge.</p>

<h2>6. Documentos de referência</h2>
<ol>
<li>FIAP. Challenge 2026 &ndash; Engenharia de Software &ndash; TOTVS: Sprint 3 e Sprint 4. Guia de projeto, entregas, disciplinas e prazos.</li>
<li>Grupo Company 404. TOTVS Inteligência Conversacional &ndash; Visão do Produto e Requisitos de Software, versão 1.0, abril de 2026.</li>
<li>Grupo Company 404. Entrega de Agile Methodology with Squad Framework &ndash; Sprints 1 e 2 (visão do produto, personas, casos de uso e backlog inicial), junho de 2026.</li>
<li>Grupo Company 404. Entrega de Database Design &ndash; Sprint 3 (modelo relacional, DDL e DML), setembro de 2026.</li>
<li>TOTVS. Briefing do Challenge: inteligência conversacional para reuniões comerciais, e base anonimizada de transcrições (ANON_transcricao.json).</li>
</ol>
<p class='nota'>Repositório da solução: github.com/TLean07/totvs-conversational-ai (branch oracle). Aplicação web: totvs-conversational-ai-oracle.vercel.app.</p>
""" % sum(b[5] for b in BACKLOG if b[7] == "Entregue (Sprint 3)")


def gerar_html(pasta, pasta_telas):
    corpo = capa() + cabecalho() + visao(pasta) + casos_uso(pasta) + backlog() + prototipo(pasta_telas) + retrospectiva()
    return "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='utf-8'><title>%s - Agile Sprint 3</title><style>%s%s</style></head><body>%s</body></html>" % (
        e(SOLUCAO), base.CSS, CSS_EXTRA, corpo)


if __name__ == "__main__":
    pasta = os.path.abspath(os.path.join(AQUI, ".."))
    html_path = os.path.join(pasta, "Company404_Agile_Sprint3.html")
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(gerar_html(pasta, os.path.join(pasta, "telas")))
    base.gerar_pdf(html_path, os.path.join(pasta, "Company404_Agile_Sprint3.pdf"))
    print("ok")
