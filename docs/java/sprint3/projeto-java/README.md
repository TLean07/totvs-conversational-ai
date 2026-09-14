# TOTVS Inteligência Conversacional — Sprint 3 (Domain Driven Design – Java)

Grupo **Company 404** — Challenge FIAP x TOTVS 2026.

Projeto Java puro (JDK 17+, sem Spring) com as camadas pedidas na Sprint 3: **Model**, **Service** (regras de negócio),
**Connection** (classe de conexão com usuário e senha no código) e **DAO** (CRUD completo via JDBC) sobre o mesmo
schema Oracle usado pela solução em produção (entrega de Database Design da Sprint 3).

## Estrutura de pacotes

```
com.tlean07.fiap.challenge
├── model        Transcricao, Analise, Insight (abstrata) ← Oportunidade / Risco, Produto, Alerta, Usuario, Feedback e enums
├── service      AnalisadorTranscricao, GeradorAlertas, PriorizadorContas, CalculadoraIndicadores, TranscricaoService
├── connection   ConnectionFactory (URL, usuário e senha do Oracle da FIAP)
├── dao          DAO<T> (interface CRUD) e TranscricaoDAO, AnaliseDAO, AlertaDAO, UsuarioDAO, FeedbackDAO
├── exception    RegraNegocioException, DAOException
└── test         TesteRegrasNegocio (sem banco) e TesteDAO (CRUD no Oracle)
```

## Métodos de relevância (regras de negócio)

| Classe | Método | O que faz |
|---|---|---|
| `AnalisadorTranscricao` | `analisar(Transcricao)` | Sentimento com negação e intensificadores, produtos TOTVS e concorrentes, oportunidades com score, riscos com severidade, persona, budget, confiança no vendedor, resumo e próxima ação |
| `GeradorAlertas` | `gerar(Transcricao, Analise)` | Alertas por tipo de risco, concorrente e oportunidade alta, prioridade por regra e alerta de churn iminente |
| `PriorizadorContas` | `rankear(List<Transcricao>)` | Ordena as contas por urgência: peso polimórfico dos insights, NPS detrator/promotor e recência |
| `CalculadoraIndicadores` | `calcular(Transcricao)` | Locutores, turnos, palavras, perguntas, participação vendedor/cliente e escuta ativa |
| `Transcricao` | `validar()`, `iniciarProcessamento()`, `concluirProcessamento()`, `registrarErro()` | Validação e máquina de estados PENDENTE → PROCESSANDO → PROCESSADA/ERRO |
| `Alerta` | `marcarVisto()`, `resolver()` | Ciclo NOVO → VISTO → RESOLVIDO com transições inválidas rejeitadas |
| `Analise` | `calcularScoreRisco()`, `definirProximaAcao()`, `gerarResumo()` | Score de risco da conta e próxima ação sugerida |
| `Usuario` | `validar()`, `podeAcessar(Transcricao)` | Regras de acesso por perfil (ADMIN vê tudo; USER só o que criou) |

## Como executar

### Com Maven (recomendado)

```bash
mvn package                                  # compila e roda os testes JUnit
java -cp target/classes com.tlean07.fiap.challenge.test.TesteRegrasNegocio
java -cp "target/classes;lib/ojdbc11.jar" com.tlean07.fiap.challenge.test.TesteDAO
```

### Sem Maven (javac + jar em `lib/`)

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -cp lib/ojdbc11.jar -d out (Get-ChildItem -Recurse src/main/java -Filter *.java).FullName
java -cp "out;lib/ojdbc11.jar" com.tlean07.fiap.challenge.test.TesteRegrasNegocio
java -cp "out;lib/ojdbc11.jar" com.tlean07.fiap.challenge.test.TesteDAO
```

No IntelliJ, abra a pasta como projeto Maven (ou adicione `lib/ojdbc11.jar` como biblioteca) e execute as classes do pacote `test`.

`TesteDAO` conecta ao Oracle da FIAP (`oracle.fiap.com.br:1521/orcl`), cria um usuário e uma reunião de teste, processa
(análise + alertas), lê, atualiza e apaga tudo o que criou. As tabelas são as do script DDL entregue em Database Design.
