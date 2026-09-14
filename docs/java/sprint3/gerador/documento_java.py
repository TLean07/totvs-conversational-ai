# -*- coding: utf-8 -*-
"""
Documentação da entrega de Domain Driven Design – Java, Sprint 3 (Challenge 2026 TOTVS).
Mesmo estilo acadêmico das outras entregas; converte HTML em PDF com o Edge headless.
"""
import io
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ_DOCS = os.path.abspath(os.path.join(AQUI, "..", "..", ".."))
sys.path.insert(0, os.path.join(RAIZ_DOCS, "database-design", "sprint3", "gerador"))
import documento as base  # noqa: E402

EQUIPE = "Company 404"
SOLUCAO = "TOTVS Inteligência Conversacional"
INTEGRANTES = [
    ("Bruno Gama Masseiras", "562526"), ("Kauai Rosa de Assis Rocha", "563256"), ("Lucas Gilmar Pedro da Silva", "566231"),
    ("Leandro Afonso Silva Santos Junior", "561344"), ("Murilo da Silva Basto", "565445"),
]
TELAS = os.path.join(RAIZ_DOCS, "agile", "sprint3", "telas")
DB = os.path.join(RAIZ_DOCS, "database-design", "sprint3", "saida")

CSS_EXTRA = """
.figura img { max-height: 150mm; }
pre.console { font-family: Consolas, 'Courier New', monospace; font-size: 8pt; line-height: 1.25; border: 1px solid #000; padding: 6px 8px; white-space: pre-wrap; background: #fff; }
table.cod td, table.cod th { font-size: 9.5pt; }
td.mono, code { font-family: Consolas, 'Courier New', monospace; font-size: 9pt; }
"""

e = base.e
figura = base.figura


def ler(caminho):
    with io.open(caminho, encoding="utf-8", errors="replace") as f:
        return f.read()


def capa():
    integrantes = "<br>".join("%s &ndash; RM %s" % (e(n), rm) for n, rm in INTEGRANTES)
    return """
<div class="capa">
  <div class="inst">FIAP &ndash; Faculdade de Informática e Administração Paulista<br>Engenharia de Software &ndash; 2&ordm; ano<br>Challenge 2026 &ndash; TOTVS</div>
  <div class="tit">
    <div class="t1">%s</div>
    <div class="t2">Plataforma de análise automatizada de reuniões comerciais com inteligência artificial</div>
    <div class="t3"><b>Domain Driven Design &ndash; Java &ndash; Sprint 3</b><br>Camadas Model, Service, Connection e DAO (JDBC) sobre o Oracle Database da solução</div>
  </div>
  <div class="grupo"><b>Grupo %s</b><br>%s</div>
  <div class="rod">São Paulo<br>Setembro de 2026</div>
</div>
""" % (e(SOLUCAO), e(EQUIPE), integrantes)


def cabecalho():
    linhas = "<br>".join("%s &ndash; RM: %s" % (e(n), rm) for n, rm in INTEGRANTES)
    itens = [("1.", "Objetivo e escopo do projeto"), ("2.", "Principais funcionalidades da solução"), ("3.", "Protótipo: telas do sistema e como interagir"),
             ("4.", "Modelo do banco de dados"), ("5.", "Diagrama de classes atualizado"), ("6.", "Projeto Java"),
             ("6.1", "Estrutura de pacotes e camada Model"), ("6.2", "Métodos de relevância (regras de negócio)"), ("6.3", "Camada de conexão"),
             ("6.4", "Camada DAO com CRUD"), ("6.5", "Classes de teste e evidências de execução"), ("6.6", "Como executar"),
             ("7.", "Documentos de referência")]
    sumario = "".join("<tr><td style='width:1.2cm'>%s</td><td>%s</td></tr>" % (n, t) for n, t in itens)
    return """
<div class="cab"><b>Grupo %s</b><br>%s<br>Domain Driven Design &ndash; Java &ndash; Challenge 2026 TOTVS &ndash; Sprint 3 &ndash; 2&ordm; ano</div>
<h1>Projeto Java do %s &ndash; Sprint 3</h1>
<h2>Sumário</h2><table class='sumario'>%s</table>
""" % (e(EQUIPE), linhas, e(SOLUCAO), sumario)


def objetivo():
    return """
<h2>1. Objetivo e escopo do projeto</h2>
<p>O <b>TOTVS Inteligência Conversacional</b> transforma transcrições de reuniões comerciais e de Customer Success da TOTVS em inteligência
acionável: a partir do texto da reunião, a solução identifica produtos TOTVS em uso, concorrentes citados, oportunidades de upsell, cross-sell e
novo produto, riscos de churn, insatisfação e perda de confiança, o sentimento geral da conversa, a persona e o orçamento do interlocutor, e sugere
a próxima ação comercial. Os resultados alimentam alertas priorizados e um dashboard executivo, na web e no aplicativo Android.</p>
<p>Nas Sprints 1 e 2, o projeto Java demonstrou os conceitos de orientação a objetos (encapsulamento, herança, polimorfismo e composição) com um
analisador por palavras-chave lendo o arquivo real da TOTVS. Nesta Sprint 3, o projeto evolui para a <b>arquitetura em camadas usada em sala</b>:
a camada <b>Model</b> representa as entidades do domínio (transcrição, análise, insight, oportunidade, risco, produto, alerta, usuário e feedback) com
os mesmos atributos das tabelas Oracle; a camada <b>Service</b> concentra as regras de negócio; a camada de <b>Connection</b> abre a conexão JDBC com o
Oracle da FIAP (usuário e senha no código, conforme pedido); e a camada <b>DAO</b> implementa o CRUD completo de cada entidade. Duas classes de
teste exercitam, respectivamente, as regras de negócio (sem banco) e o CRUD contra o banco real.</p>
<p>O escopo é o mesmo domínio da solução em produção (backend Spring Boot, frontend Next.js, LeanIA em Python e Oracle 19c). O projeto Java desta
entrega é independente de frameworks e usa exatamente o schema entregue em Database Design na Sprint 3, o que permite ao professor executar o CRUD
no schema do grupo ou em qualquer schema criado com o script DDL.</p>
"""


def funcionalidades():
    return """
<h2>2. Principais funcionalidades da solução</h2>
<table>
<tr><th style="width:4.4cm">Funcionalidade</th><th>Descrição</th><th style="width:3.4cm">No projeto Java</th></tr>
<tr><td>Ingestão de transcrições</td><td>Formulário web, API REST, importação em lote dos arquivos JSON e CSV da TOTVS e transcrição de áudio.</td><td>Transcricao, TranscricaoDAO, TranscricaoService</td></tr>
<tr><td>Análise da reunião (LeanIA)</td><td>Sentimento, produtos e concorrentes, oportunidades com score, riscos com severidade, persona, budget, confiança no vendedor, resumo e próxima ação.</td><td>AnalisadorTranscricao, Analise, Insight, Oportunidade, Risco, Produto</td></tr>
<tr><td>Alertas acionáveis</td><td>Alertas por risco, concorrente e oportunidade alta, com prioridade e ciclo NOVO &rarr; VISTO &rarr; RESOLVIDO.</td><td>GeradorAlertas, Alerta, AlertaDAO</td></tr>
<tr><td>Priorização de contas</td><td>Ordena os clientes por urgência de atuação combinando riscos, oportunidades, NPS e recência da reunião.</td><td>PriorizadorContas</td></tr>
<tr><td>Qualidade da conversa</td><td>Participação de vendedor e cliente, perguntas, turnos e escuta ativa a partir dos locutores da transcrição.</td><td>CalculadoraIndicadores</td></tr>
<tr><td>Feedback adaptativo</td><td>O usuário avalia a análise (útil ou ajuste) e o histórico entra como contexto das próximas análises.</td><td>Feedback, FeedbackDAO</td></tr>
<tr><td>Segurança e perfis</td><td>Usuários com perfil ADMIN ou USER; o usuário comum só acessa o que criou.</td><td>Usuario, UsuarioDAO</td></tr>
<tr><td>Dashboard executivo</td><td>Métricas consolidadas, gráficos de sentimento, riscos e oportunidades, reuniões e alertas recentes (web e Android).</td><td>consultas dos DAOs (listar, contar, por status)</td></tr>
</table>
"""


def prototipo():
    telas = [
        ("v_01_login.png", "Tela de login", "Informe o e-mail e a senha cadastrados e clique em <b>Entrar</b>. Após a autenticação, o sistema abre o dashboard; a sessão é renovada automaticamente e o botão <b>Sair</b> encerra o acesso. No projeto Java, o cadastro e a consulta de usuários são feitos por UsuarioDAO e a regra de acesso por Usuario.podeAcessar."),
        ("v_02_dashboard.png", "Dashboard executivo", "Primeira tela após o login. Os seis cartões mostram os totais (transcrições, processadas, oportunidades, riscos e alertas novos); os gráficos detalham sentimento, riscos por tipo e oportunidades por tipo. Use <b>Nova transcrição</b> para cadastrar uma reunião, <b>Atualizar</b> para recarregar os dados, e os blocos de alertas e transcrições recentes para ir direto ao registro. Os totais equivalem às consultas listar e contar dos DAOs."),
        ("v_03_transcricoes.png", "Gestão de transcrições", "Lista as reuniões com cliente, vendedor, origem, status e sentimento. Filtre por cliente ou vendedor no topo e por sentimento e status nos botões. <b>Nova transcrição</b> abre o formulário de texto, <b>Importar JSON TOTVS</b> envia o arquivo da TOTVS e <b>Enviar áudio</b> transcreve um arquivo de áudio. Clique no cartão para abrir o detalhe; o ícone de lixeira exclui a reunião (TranscricaoDAO.excluir, com exclusão em cascata da análise e dos alertas)."),
        ("v_04_analise_a.png", "Detalhe da análise", "Mostra a transcrição original e, abaixo, o resumo da análise, persona, budget e confiança no vendedor, a próxima ação sugerida e os insights além do óbvio. Em <b>Feedback para a LeanIA</b>, clique em <b>Útil</b> ou <b>Precisa ajuste</b> e, se quiser, descreva a correção; o histórico aparece logo abaixo (FeedbackDAO). Os botões <b>CSV</b> e <b>PDF</b> exportam a análise; <b>Reprocessar</b> refaz a análise (TranscricaoService.processar)."),
        ("v_05_alertas.png", "Central de alertas", "Fila de alertas gerados pela análise, com severidade, tipo, cliente e o trecho que originou o alerta. Filtre por severidade e alterne entre <b>Novos</b> e <b>Todos</b>. Em cada cartão, <b>Visto</b> marca o alerta como visto e <b>Resolver</b> o encerra; a ordem das ações é obrigatória (Alerta.marcarVisto e Alerta.resolver, persistidos por AlertaDAO.atualizar)."),
    ]
    h = ["<h2 class='quebra'>3. Protótipo: telas do sistema e como interagir</h2>",
         "<p>As telas são capturas da aplicação web em produção, com os dados reais da base de 623 reuniões analisadas. Para cada tela,"
         " o texto explica como o usuário interage e qual classe do projeto Java realiza a operação correspondente.</p>"]
    for i, (arq, titulo, texto) in enumerate(telas):
        h.append("<h3>3.%d %s</h3>" % (i + 1, e(titulo)))
        h.append(figura(os.path.join(TELAS, arq), "Figura %d &ndash; %s." % (i + 1, e(titulo))))
        h.append("<p>%s</p>" % texto)
    return "\n".join(h)


def modelo_banco():
    return """
<h2>4. Modelo do banco de dados</h2>
<p>O projeto Java persiste os objetos no mesmo modelo relacional da solução, construído no Oracle SQL Developer Data Modeler e entregue em
Database Design (Sprint 3): 20 tabelas, 24 chaves estrangeiras, sequence, colunas IDENTITY e triggers de histórico. As classes do Model
correspondem às tabelas APP_USERS (Usuario), TRANSCRICOES (Transcricao), ANALISES (Analise), ANALISE_PRODUTOS (Produto),
ANALISE_OPORTUNIDADES (Oportunidade), ANALISE_RISCOS (Risco), ALERTAS (Alerta) e ANALISE_FEEDBACKS (Feedback).</p>
<div class='paisagem'>%s</div>
<div class='paisagem'>%s</div>
<div class='paisagem'>%s</div>
<table>
<tr><th style="width:3.6cm">Classe Java</th><th style="width:4cm">Tabela Oracle</th><th>Observações de mapeamento</th></tr>
<tr><td>Usuario</td><td>APP_USERS</td><td>ID UUID gerado no construtor; IS_ACTIVE 0/1 vira boolean; USER_ROLE vira o enum Perfil.</td></tr>
<tr><td>Transcricao</td><td>TRANSCRICOES</td><td>Colunas principais da reunião; STATUS vira StatusTranscricao; ANALISE_ID guarda a referência 1:1 para a análise atual.</td></tr>
<tr><td>Analise</td><td>ANALISES</td><td>Sentimento, resumo, persona, budget, scores, próxima ação; as listas ficam nas tabelas filhas.</td></tr>
<tr><td>Produto</td><td>ANALISE_PRODUTOS</td><td>Chave composta (POSICAO, ANALISE_ID); POSICAO preserva a ordem da lista.</td></tr>
<tr><td>Oportunidade / Risco</td><td>ANALISE_OPORTUNIDADES / ANALISE_RISCOS</td><td>Subclasses de Insight; AnaliseDAO grava e lê as duas listas na mesma transação.</td></tr>
<tr><td>Alerta</td><td>ALERTAS</td><td>PRIORIDADE vira Severidade e STATUS vira StatusAlerta; ON DELETE CASCADE a partir da análise.</td></tr>
<tr><td>Feedback</td><td>ANALISE_FEEDBACKS</td><td>UTIL 0/1 vira boolean; ANALISE_ID fica NULL se a análise for refeita (SET NULL).</td></tr>
</table>
""" % (figura(os.path.join(DB, "datamodeler_relacional_completo.png"), "Figura 6 &ndash; Modelo relacional completo exportado do Oracle SQL Developer Data Modeler."),
       figura(os.path.join(DB, "datamodeler_relacional_esquerda.png"), "Figura 7 &ndash; Metade esquerda do modelo: usuários, transcrições e listas consolidadas."),
       figura(os.path.join(DB, "datamodeler_relacional_direita.png"), "Figura 8 &ndash; Metade direita do modelo: análises, detalhes, alertas, histórico e feedbacks."))


def diagrama_classes():
    return """
<h2>5. Diagrama de classes atualizado</h2>
<p>A Figura 9 apresenta as classes do projeto com atributos (visibilidade, nome e tipo) e métodos, organizadas nos pacotes model, service e dao/connection.
Em relação às Sprints 1 e 2, a herança Insight &larr; Oportunidade / Risco e o polimorfismo da lista de insights foram mantidos; Transcricao,
Analise, Alerta, Usuario e Feedback ganharam os atributos das tabelas Oracle e os métodos de regra de negócio; entraram a interface genérica DAO&lt;T&gt;,
as cinco implementações DAO, a ConnectionFactory e os serviços.</p>
<div class='paisagem'>%s</div>
""" % figura(os.path.join(AQUI, "..", "diagrama_classes.png"), "Figura 9 &ndash; Diagrama de classes do projeto Java (Sprint 3).")


def projeto_java():
    regras = ler(os.path.join(AQUI, "..", "saida_teste_regras.txt"))
    dao = ler(os.path.join(AQUI, "..", "saida_teste_dao.txt"))
    return """
<h2 class='quebra'>6. Projeto Java</h2>
<h3>6.1 Estrutura de pacotes e camada Model</h3>
<pre class="console">com.tlean07.fiap.challenge
├── model        Transcricao, Analise, Insight (abstrata) ← Oportunidade / Risco, Produto, Alerta, Usuario, Feedback
│                e as enumerações Sentimento, Severidade, StatusAlerta, StatusTranscricao, TipoOportunidade, TipoProduto, TipoRisco, Perfil
├── service      AnalisadorTranscricao, GeradorAlertas, PriorizadorContas, CalculadoraIndicadores, TranscricaoService
├── connection   ConnectionFactory
├── dao          DAO&lt;T&gt; (interface CRUD), TranscricaoDAO, AnaliseDAO, AlertaDAO, UsuarioDAO, FeedbackDAO
├── exception    RegraNegocioException, DAOException
└── test         TesteRegrasNegocio (sem banco), TesteDAO (CRUD no Oracle)</pre>
<p>A camada Model segue os padrões trabalhados em sala: atributos privados com getters e setters, construtor padrão e construtor com os
campos principais, identificador UUID gerado no construtor (mesma estratégia do backend), toString para os relatórios, enumerações para os
domínios fechados (status, sentimento, severidade, tipos) e exceção própria de regra de negócio. A classe abstrata Insight define os métodos
abstratos getCategoria e calcularPeso; Oportunidade e Risco herdam dela e os sobrescrevem, e Analise guarda ambas em uma única
List&lt;Insight&gt; (polimorfismo), com getOportunidades e getRiscos filtrando por tipo. Transcricao possui uma Analise (composição 1:1) e
Analise possui listas de Produto e Insight.</p>

<h3>6.2 Métodos de relevância (regras de negócio)</h3>
<table class="cod">
<tr><th style="width:3.2cm">Classe</th><th style="width:4.4cm">Método</th><th>Lógica implementada</th></tr>
<tr><td>AnalisadorTranscricao</td><td class='mono'>analisar(Transcricao)</td><td>Normaliza o texto (acentos, caixa), separa sentenças por locutor, identifica produtos TOTVS e concorrentes (catálogo), classifica cada
produto como em uso ou argumento de venda pelo contexto da frase, detecta oportunidades (intenção de compra 0,85; interesse 0,60) e riscos (churn, perda de confiança,
concorrência em negociação, insatisfação com severidade por intensificadores), classifica o sentimento com léxico, negação (“não gostei”) e intensificadores, extrai
persona e budget (R$, mil, por mês) com expressões regulares, calcula a confiança no vendedor e gera o resumo com a próxima ação.</td></tr>
<tr><td>GeradorAlertas</td><td class='mono'>gerar(Transcricao, Analise)</td><td>Um alerta por tipo de risco (sem duplicar), alerta por concorrente citado e por oportunidade alta; churn e perda de confiança
recebem prioridade ALTO; churn alto combinado com concorrente ou perda de confiança gera o alerta CHURN_IMINENTE no topo da lista.</td></tr>
<tr><td>PriorizadorContas</td><td class='mono'>rankear(List&lt;Transcricao&gt;)</td><td>Score de urgência = soma polimórfica dos pesos dos insights (riscos somam, oportunidades subtraem) + penalidade por
sentimento negativo + 3 pontos para NPS detrator, &minus;1 para promotor, +1 para reunião nos últimos 7 dias; devolve as contas ordenadas com o motivo.</td></tr>
<tr><td>CalculadoraIndicadores</td><td class='mono'>calcular(Transcricao)</td><td>Reconhece os turnos “[LOCUTOR N]:”, conta palavras e perguntas por locutor, assume o locutor que mais fala como vendedor,
calcula as participações e classifica a escuta ativa (equilibrado, falou mais, ouviu mais).</td></tr>
<tr><td>Transcricao</td><td class='mono'>validar(), iniciarProcessamento(), concluirProcessamento(Analise), registrarErro()</td><td>Validação (cliente obrigatório, texto mínimo, duração positiva, NPS 0 a 10) e máquina de estados
PENDENTE &rarr; PROCESSANDO &rarr; PROCESSADA / ERRO, rejeitando transições inválidas com RegraNegocioException.</td></tr>
<tr><td>Alerta</td><td class='mono'>marcarVisto(), resolver()</td><td>Ciclo NOVO &rarr; VISTO &rarr; RESOLVIDO; um alerta não pode ser resolvido sem ter sido visto.</td></tr>
<tr><td>Analise</td><td class='mono'>calcularScoreRisco(), definirProximaAcao(), gerarResumo()</td><td>Score de risco da conta e escolha da próxima ação conforme churn, perda de confiança, oportunidade alta e budget.</td></tr>
<tr><td>Usuario</td><td class='mono'>validar(), podeAcessar(Transcricao)</td><td>E-mail válido, nome mínimo, perfil obrigatório; ADMIN acessa tudo, USER só as próprias reuniões, inativo não acessa.</td></tr>
</table>

<h3>6.3 Camada de conexão</h3>
<p>A classe <code>ConnectionFactory</code> (pacote connection) guarda a URL JDBC do Oracle da FIAP (<code>jdbc:oracle:thin:@//oracle.fiap.com.br:1521/orcl</code>),
o usuário (RM561344) e a senha como constantes no código, conforme solicitado na entrega. O método <code>getConnection()</code> abre a conexão com
<code>DriverManager</code> e desliga o autocommit, para que os DAOs controlem a transação; <code>testarConexao()</code> executa
<code>SELECT 1 FROM DUAL</code>. Falhas de conexão viram <code>DAOException</code>, que encapsula a <code>SQLException</code> original.</p>

<h3>6.4 Camada DAO com CRUD</h3>
<p>A interface genérica <code>DAO&lt;T&gt;</code> define inserir, buscarPorId, listar, atualizar e excluir. Cada DAO usa <code>PreparedStatement</code>
com parâmetros (sem concatenação de SQL), try-with-resources para fechar conexões e statements, commit explícito e mapeamento ResultSet &rarr; objeto.</p>
<table class="cod">
<tr><th style="width:3cm">DAO</th><th style="width:3.6cm">Tabela(s)</th><th>Operações</th></tr>
<tr><td>TranscricaoDAO</td><td>TRANSCRICOES</td><td>inserir, buscarPorId, listar (100 mais recentes), listarPorCliente, listarPorStatus, atualizar, excluir, contar.</td></tr>
<tr><td>AnaliseDAO</td><td>ANALISES, ANALISE_PRODUTOS, ANALISE_OPORTUNIDADES, ANALISE_RISCOS</td><td>inserir e atualizar gravam a análise e as três listas na mesma transação (batch); buscarPorId e
buscarPorTranscricao carregam as listas; excluir apaga a análise e o banco remove detalhes e alertas em cascata.</td></tr>
<tr><td>AlertaDAO</td><td>ALERTAS</td><td>inserir, inserirTodos (lote), buscarPorId, listar, listarPorStatus, listarPorAnalise, atualizar (status), excluir.</td></tr>
<tr><td>UsuarioDAO</td><td>APP_USERS</td><td>inserir (valida antes), buscarPorId, buscarPorEmail, listar, atualizar, excluir.</td></tr>
<tr><td>FeedbackDAO</td><td>ANALISE_FEEDBACKS</td><td>inserir, buscarPorId, listar, listarPorTranscricao, atualizar, excluir.</td></tr>
</table>
<p>A classe <code>TranscricaoService</code> orquestra o fluxo completo com os DAOs: cadastrar (valida e insere), processar (PENDENTE &rarr; PROCESSANDO,
analisa, grava a análise e os alertas, PROCESSADA; em caso de erro, ERRO) e excluir.</p>

<h3>6.5 Classes de teste e evidências de execução</h3>
<p><b>TesteRegrasNegocio</b> instancia usuários, transcrições, análises e alertas e verifica os métodos de relevância sem banco de dados
(29 verificações). Saída da execução:</p>
<pre class="console">%s</pre>
<p><b>TesteDAO</b> executa o CRUD completo no Oracle da FIAP: cria um usuário e uma reunião de teste, processa a reunião (análise e alertas gravados),
lê pelos métodos de consulta, atualiza a transcrição, um alerta, o feedback e o usuário, e ao final exclui tudo o que criou, comprovando a
exclusão em cascata. Saída da execução:</p>
<pre class="console">%s</pre>
<p>Os quatro testes JUnit 5 em <code>src/test/java</code> (churn com alerta iminente, ciclo de status, ciclo do alerta e extração de budget) também passam
no <code>mvn package</code>.</p>

<h3>6.6 Como executar</h3>
<ol>
<li>Requisitos: JDK 17 ou superior e acesso ao Oracle da FIAP. O driver <code>ojdbc11.jar</code> está em <code>lib/</code> e também é declarado no <code>pom.xml</code>.</li>
<li>Com Maven: <code>mvn package</code> compila, roda os testes JUnit e gera o jar. Sem Maven: compile com <code>javac -cp lib/ojdbc11.jar -d out</code> os arquivos de <code>src/main/java</code>.</li>
<li>Regras de negócio (sem banco): <code>java -cp target/classes com.tlean07.fiap.challenge.test.TesteRegrasNegocio</code>.</li>
<li>CRUD no Oracle: <code>java -cp "target/classes;lib/ojdbc11.jar" com.tlean07.fiap.challenge.test.TesteDAO</code>.</li>
<li>No IntelliJ ou Eclipse, abra a pasta como projeto Maven (ou adicione <code>lib/ojdbc11.jar</code> como biblioteca) e execute as classes do pacote <code>test</code>.</li>
</ol>
""" % (e(regras.strip()), e(dao.strip()))


def referencias():
    return """
<h2>7. Documentos de referência</h2>
<ol>
<li>FIAP. Challenge 2026 &ndash; Engenharia de Software &ndash; TOTVS: Sprint 3 e Sprint 4. Orientações da disciplina Domain Driven Design &ndash; Java.</li>
<li>Grupo Company 404. Entrega de Domain Driven Design &ndash; Java &ndash; Sprints 1 e 2 (classes de domínio, herança e polimorfismo), junho de 2026.</li>
<li>Grupo Company 404. Entrega de Database Design &ndash; Sprint 3 (modelo relacional, DDL e DML), setembro de 2026.</li>
<li>Oracle. Oracle Database JDBC Developer's Guide, 21c; Oracle Database 19c SQL Language Reference.</li>
</ol>
<p class='nota'>Repositório da solução: github.com/TLean07/totvs-conversational-ai (branch oracle). Aplicação web: totvs-conversational-ai-oracle.vercel.app.</p>
"""


def gerar_html():
    corpo = capa() + cabecalho() + objetivo() + funcionalidades() + prototipo() + modelo_banco() + diagrama_classes() + projeto_java() + referencias()
    return "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='utf-8'><title>%s - Java Sprint 3</title><style>%s%s</style></head><body>%s</body></html>" % (
        e(SOLUCAO), base.CSS, CSS_EXTRA, corpo)


if __name__ == "__main__":
    pasta = os.path.abspath(os.path.join(AQUI, ".."))
    html_path = os.path.join(pasta, "Company404_Java_Sprint3.html")
    with io.open(html_path, "w", encoding="utf-8") as f:
        f.write(gerar_html())
    base.gerar_pdf(html_path, os.path.join(pasta, "Company404_Java_Sprint3.pdf"))
    print("ok")
