# Politica de Retencao de Dados

## Objetivo

Definir como o projeto deve tratar transcricoes, analises, alertas, feedbacks e tokens para reduzir exposicao de dados sensiveis sem quebrar a experiencia do usuario.

## Dados armazenados

- Usuarios: e-mail, nome, perfil, status e hash da senha.
- Refresh tokens: apenas hash, data de criacao, expiracao e revogacao.
- Transcricoes: texto original, cliente, vendedor, participantes, duracao e datas.
- Analises: sentimento, resumo, produtos, oportunidades, riscos, alertas e scores.
- Feedbacks: avaliacao da analise e comentario de ajuste.

## Retencao recomendada

| Dado | Retencao recomendada | Motivo |
| --- | --- | --- |
| Refresh tokens revogados/expirados | 30 dias | Auditoria curta e reducao de superficie |
| Transcricoes brutas | 180 dias | Dados comerciais podem conter informacao sensivel |
| Analises e alertas | 365 dias | Historico util para tendencias comerciais |
| Feedbacks da LeanIA | 365 dias | Melhora adaptativa sem guardar transcricao indefinidamente |
| Logs de aplicacao | 30 a 90 dias | Investigacao operacional e seguranca |

## Boas praticas

- Nao registrar texto integral de transcricoes em logs.
- Nao registrar senhas, tokens, cookies ou connection strings.
- Usar `X-Request-ID` para investigar problemas sem expor conteudo sensivel.
- Antes de enviar texto para IA, preferir remover ou mascarar CPF, CNPJ, e-mail, telefone e dados bancarios.
- Permitir exclusao de transcricao e remover analise, alertas e feedbacks relacionados.

## Implementacao atual

- O sistema ja remove analises, alertas e feedbacks relacionados quando uma transcricao e excluida.
- Refresh tokens sao armazenados como hash.
- Logs possuem request id.
- A LeanIA usa token interno e nao exige API paga.

## Melhorias futuras

- Criar job agendado para expurgar automaticamente dados antigos.
- Adicionar tela administrativa para configurar retencao.
- Implementar redacao automatica de dados sensiveis antes da chamada da LeanIA.
- Exportar relatorio de auditoria por periodo.
