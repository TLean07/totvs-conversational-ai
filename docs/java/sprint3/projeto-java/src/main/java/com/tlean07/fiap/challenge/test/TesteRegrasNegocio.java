package com.tlean07.fiap.challenge.test;

import com.tlean07.fiap.challenge.exception.RegraNegocioException;
import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Perfil;
import com.tlean07.fiap.challenge.model.Sentimento;
import com.tlean07.fiap.challenge.model.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.model.Usuario;
import com.tlean07.fiap.challenge.service.AnalisadorTranscricao;
import com.tlean07.fiap.challenge.service.CalculadoraIndicadores;
import com.tlean07.fiap.challenge.service.GeradorAlertas;
import com.tlean07.fiap.challenge.service.PriorizadorContas;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Classe de teste (sem banco de dados): instancia os objetos do modelo e exercita os metodos
 * de relevancia. Cada verificacao imprime OK ou FALHOU; ao final, o resumo.
 */
public class TesteRegrasNegocio {

    private static int ok = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        System.out.println("=== TOTVS Inteligencia Conversacional - Teste das regras de negocio ===\n");

        Usuario vendedor = new Usuario("rafael.moura@company404.com.br", "Rafael Moura", "hash", Perfil.USER);
        Usuario diretora = new Usuario("diretoria@company404.com.br", "Helena Prado", "hash", Perfil.ADMIN);

        Transcricao reuniaoChurn = new Transcricao("Clinica Vida Plena", "Rafael Moura",
                "[LOCUTOR 2]: Estamos ha tres semanas com o chamado do RM aberto e ninguem retorna. "
                + "[LOCUTOR 1]: Entendo a frustracao, vou escalar hoje. "
                + "[LOCUTOR 2]: A Senior ja nos apresentou uma proposta e a diretoria esta pensando em trocar. "
                + "Se nao resolver ate o fim do mes, vamos cancelar o contrato.");
        reuniaoChurn.setOwnerId(vendedor.getId());
        reuniaoChurn.setNotaNps(3);
        reuniaoChurn.setDataReuniao(LocalDateTime.now().minusDays(2));

        Transcricao reuniaoOportunidade = new Transcricao("Metalurgica Andrade", "Rafael Moura",
                "[LOCUTOR 1]: Bom dia, pessoal. Hoje queria revisar o uso do Protheus no financeiro. "
                + "[LOCUTOR 2]: O modulo financeiro atende bem, mas a aprovacao de compras ainda e por e-mail. "
                + "[LOCUTOR 1]: O Fluig resolve exatamente esse fluxo de aprovacao. "
                + "[LOCUTOR 2]: Gostei. Precisamos de uma proposta ate o fim do mes. "
                + "[LOCUTOR 3]: O orcamento previsto para automacao e de uns R$ 60 mil. Nosso diretor financeiro aprova.");
        reuniaoOportunidade.setOwnerId(vendedor.getId());
        reuniaoOportunidade.setNotaNps(8);

        Transcricao reuniaoNeutra = new Transcricao("Colegio Nova Geracao", "Juliana Castro",
                "Beatriz: O RM Educacional atende a secretaria. Juliana: Podemos revisar a infraestrutura. Beatriz: Vamos avaliar com calma.");
        reuniaoNeutra.setOwnerId(diretora.getId());

        // 1. validacao e maquina de estados da transcricao
        System.out.println("-- Transcricao: validacao e ciclo de status");
        Transcricao invalida = new Transcricao("", "Vendedor", "curto");
        verificar("transcricao sem cliente e texto curto e rejeitada", lancaRegra(invalida::validar));
        verificar("status inicial PENDENTE", reuniaoChurn.getStatus() == StatusTranscricao.PENDENTE);
        reuniaoChurn.iniciarProcessamento();
        verificar("PENDENTE -> PROCESSANDO", reuniaoChurn.getStatus() == StatusTranscricao.PROCESSANDO);
        verificar("nao pode iniciar duas vezes", lancaRegra(reuniaoChurn::iniciarProcessamento));

        // 2. analisador
        System.out.println("\n-- AnalisadorTranscricao");
        AnalisadorTranscricao analisador = new AnalisadorTranscricao();
        Analise analiseChurn = analisador.analisar(reuniaoChurn);
        System.out.println("   " + analiseChurn);
        analiseChurn.getInsights().forEach(i -> System.out.println("   - " + i));
        verificar("sentimento NEGATIVO na reuniao de churn", analiseChurn.getSentimentoGeral() == Sentimento.NEGATIVO);
        verificar("risco de CHURN detectado", analiseChurn.getRiscos().stream().anyMatch(r -> r.getTipo().name().equals("CHURN")));
        verificar("concorrente Senior identificado", analiseChurn.getConcorrentes().stream().anyMatch(p -> p.getNome().equals("Senior")));
        verificar("TOTVS RM identificado como produto atual", analiseChurn.getProdutos().stream().anyMatch(p -> p.getNome().equals("TOTVS RM")));
        verificar("proxima acao e plano de retencao", analiseChurn.getProximaAcao().contains("retencao"));

        Analise analiseOport = analisador.analisar(reuniaoOportunidade);
        System.out.println("   " + analiseOport);
        analiseOport.getInsights().forEach(i -> System.out.println("   - " + i));
        verificar("oportunidade alta para o Fluig", analiseOport.getOportunidades().stream().anyMatch(o -> o.isAlta() && o.getProdutoRelacionado().contains("Fluig")));
        verificar("budget R$ 60.000,00 extraido", "R$ 60.000,00".equals(analiseOport.getBudgetIdentificado()));
        verificar("persona de decisor identificada", analiseOport.getPersonaIdentificada() != null && analiseOport.getPersonaIdentificada().contains("decisor"));
        verificar("confianca no vendedor maior que 0,5", analiseOport.getScoreConfiancaVendedor() > 0.5);
        System.out.println("   Resumo: " + analiseOport.getResumo());

        Analise analiseNeutra = analisador.analisar(reuniaoNeutra);
        verificar("reuniao sem sinais fica NEUTRA", analiseNeutra.getSentimentoGeral() == Sentimento.NEUTRO);

        // 3. conclusao do processamento
        reuniaoChurn.concluirProcessamento(analiseChurn);
        verificar("PROCESSANDO -> PROCESSADA com analise vinculada", reuniaoChurn.isProcessada() && analiseChurn.getTranscricaoId().equals(reuniaoChurn.getId()));
        reuniaoOportunidade.iniciarProcessamento();
        reuniaoOportunidade.concluirProcessamento(analiseOport);
        reuniaoNeutra.iniciarProcessamento();
        reuniaoNeutra.concluirProcessamento(analiseNeutra);

        // 4. gerador de alertas e ciclo do alerta
        System.out.println("\n-- GeradorAlertas e ciclo do alerta");
        List<Alerta> alertas = new GeradorAlertas().gerar(reuniaoChurn, analiseChurn);
        alertas.forEach(a -> System.out.println("   - " + a));
        verificar("alerta CHURN_IMINENTE gerado (churn + concorrente)", alertas.stream().anyMatch(a -> a.getTipo().equals("CHURN_IMINENTE")));
        verificar("alerta de churn tem prioridade ALTO", alertas.stream().filter(a -> a.getTipo().equals("RISCO_CHURN")).allMatch(Alerta::isAltaPrioridade));
        Alerta primeiro = alertas.get(0);
        verificar("resolver alerta NOVO direto e rejeitado", lancaRegra(primeiro::resolver));
        primeiro.marcarVisto();
        primeiro.resolver();
        verificar("NOVO -> VISTO -> RESOLVIDO", !primeiro.isPendente());
        List<Alerta> alertasOport = new GeradorAlertas().gerar(reuniaoOportunidade, analiseOport);
        verificar("oportunidade alta gera alerta OPORTUNIDADE_ALTA", alertasOport.stream().anyMatch(a -> a.getTipo().equals("OPORTUNIDADE_ALTA")));

        // 5. priorizacao de contas
        System.out.println("\n-- PriorizadorContas");
        List<PriorizadorContas.ContaPriorizada> ranking = new PriorizadorContas().rankear(List.of(reuniaoNeutra, reuniaoOportunidade, reuniaoChurn));
        ranking.forEach(c -> System.out.println("   " + c));
        verificar("conta com churn e NPS detrator fica em primeiro", ranking.get(0).getTranscricao() == reuniaoChurn);
        verificar("conta com oportunidade fica por ultimo", ranking.get(ranking.size() - 1).getTranscricao() == reuniaoOportunidade);

        // 6. indicadores da conversa
        System.out.println("\n-- CalculadoraIndicadores");
        CalculadoraIndicadores.IndicadoresConversa ind = new CalculadoraIndicadores().calcular(reuniaoOportunidade);
        System.out.println("   " + ind);
        verificar("tres locutores reconhecidos", ind.getLocutores() == 3);
        verificar("participacoes somam 100%", ind.getParticipacaoVendedorPct() + ind.getParticipacaoClientePct() == 100);

        // 7. controle de acesso
        System.out.println("\n-- Usuario: controle de acesso");
        verificar("vendedor acessa a propria reuniao", vendedor.podeAcessar(reuniaoChurn));
        verificar("vendedor nao acessa reuniao de outro usuario", !vendedor.podeAcessar(reuniaoNeutra));
        verificar("administradora acessa qualquer reuniao", diretora.podeAcessar(reuniaoChurn));
        vendedor.setAtivo(false);
        verificar("usuario inativo nao acessa nada", !vendedor.podeAcessar(reuniaoChurn));
        verificar("usuario com e-mail invalido e rejeitado", lancaRegra(() -> new Usuario("email-invalido", "Nome", "h", Perfil.USER).validar()));

        System.out.printf("%n=== Resultado: %d verificacoes OK, %d falha(s) ===%n", ok, falhas);
        if (falhas > 0) {
            System.exit(1);
        }
    }

    private static boolean lancaRegra(Runnable acao) {
        try {
            acao.run();
            return false;
        } catch (RegraNegocioException e) {
            return true;
        }
    }

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            ok++;
            System.out.println("   [OK]     " + descricao);
        } else {
            falhas++;
            System.out.println("   [FALHOU] " + descricao);
        }
    }
}
