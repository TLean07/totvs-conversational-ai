package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.AiAnalysisDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O motor local precisa reproduzir a "Entrega Esperada" do briefing do Challenge FIAP x TOTVS
 * (pagina 15) para a transcricao de exemplo.
 */
class LeanIALocalAnalyzerTest {

    private static final String TEXTO_BRIEFING = "Bom dia, João. Então, o nosso Protheus está atendendo o backoffice, "
            + "mas o time de RH está sofrendo muito com a folha manual. O pessoal viu uma demo da Senior e gostou, mas eu "
            + "prefiro consolidar tudo na TOTVS se o RM for realmente integrado. Ah, e por favor, não mencione esse valor "
            + "de R$ 50 mil que conversamos para o meu CFO ainda, ele está focado no ROI do semestre.";

    private final LeanIALocalAnalyzer analyzer = new LeanIALocalAnalyzer();

    @Test
    void exemploDoBriefingEntregaOResultadoEsperado() {
        AiAnalysisDTO r = analyzer.analisar(TEXTO_BRIEFING, null);
        Map<String, String> produtos = r.getProdutos().stream()
                .collect(Collectors.toMap(AiAnalysisDTO.ProdutoDTO::getNomeProduto, AiAnalysisDTO.ProdutoDTO::getTipo));

        assertEquals("ATUAL", produtos.get("TOTVS Protheus"));
        assertEquals("OPORTUNIDADE", produtos.get("TOTVS RM"));
        assertEquals("CONCORRENTE", produtos.get("Senior"));

        AiAnalysisDTO.RiscoDTO concorrencia = r.getRiscos().stream()
                .filter(x -> "CONCORRENCIA".equals(x.getTipo())).findFirst().orElseThrow();
        assertEquals("ALTO", concorrencia.getSeveridade());
        assertEquals("Senior", concorrencia.getConcorrenteMencionado());
        assertTrue(concorrencia.getDescricao().contains("RH"));

        AiAnalysisDTO.OportunidadeDTO upsell = r.getOportunidades().get(0);
        assertEquals("UPSELL", upsell.getTipo());
        assertTrue(upsell.getDescricao().contains("Folha de Pagamento"));
        assertEquals("TOTVS RM", upsell.getProdutoRelacionado());
        assertTrue(upsell.getScore() >= 0.8);
        assertEquals("R$ 50.000,00", upsell.getValorEstimado());

        assertTrue(r.getPersonaIdentificada().startsWith("Gestor Operacional"));
        assertEquals("R$ 50.000,00", r.getBudgetIdentificado());
        assertEquals("MISTO", r.getSentimentoGeral());
        assertTrue(r.getSentimentoDetalhe().contains("Backoffice") && r.getSentimentoDetalhe().contains("RH"));
        assertTrue(r.getInsightsAdicionais().getObservacoes().stream().anyMatch(o -> o.contains("CFO")));
        assertTrue(r.getScoreConfiancaVendedor() >= 0.6);
        assertEquals("Concorrencia", r.getAlertasConsolidados().get(0).getCategoria());
        assertTrue(r.getProximaAcao().contains("Senior"));
    }

    @Test
    void dialogoComLocutoresMedeParticipacaoEJornada() {
        String dialogo = "[LOCUTOR 1]: Boa tarde! Obrigado por vir. Me conta sobre seu negócio.\n"
                + "[LOCUTOR 2]: A gente atua em varejo há 10 anos. Temos 15 lojas em São Paulo.\n"
                + "[LOCUTOR 1]: E qual é o principal desafio?\n"
                + "[LOCUTOR 2]: Controle de estoque. A gente perde muito tempo com isso, é tudo manual e a gente perde venda.\n"
                + "[LOCUTOR 1]: Nossa solução conecta todas as lojas em uma plataforma integrada.\n"
                + "[LOCUTOR 2]: Quanto custa? E quanto tempo leva?\n"
                + "[LOCUTOR 1]: Uns 15 a 20 mil por mês e uns 30 dias no máximo. Vou enviar a proposta formal.\n"
                + "[LOCUTOR 2]: Pode mandar. Preciso conversar com meu sócio, amanhã a gente responde.\n";
        AiAnalysisDTO r = analyzer.analisar(dialogo, null);
        Map<String, Object> ind = r.getIndicadoresQualidade();
        assertEquals(2, ind.get("locutores"));
        assertEquals(100, (int) ind.get("participacaoVendedorPct") + (int) ind.get("participacaoClientePct"));
        assertTrue((int) ind.get("perguntasCliente") >= 2);
        assertFalse(r.getInsightsAdicionais().getDuvidasJornada().isEmpty());
        assertFalse(r.getInsightsAdicionais().getProximosPassos().isEmpty());
        assertEquals("R$ 15.000,00 a R$ 20.000,00 por mes", r.getBudgetIdentificado());
        assertEquals("NOVO_PRODUTO", r.getOportunidades().get(0).getTipo());
        assertTrue(r.getOportunidades().get(0).getDescricao().contains("Estoque"));
        assertTrue(r.getRiscos().stream().anyMatch(x -> "INSATISFACAO".equals(x.getTipo())));
        assertTrue(r.getRiscos().stream().noneMatch(x -> "CHURN".equals(x.getTipo())));
    }

    @Test
    void cancelarReuniaoNaoEChurnMasCancelarContratoE() {
        assertTrue(analyzer.analisar("[LOCUTOR 1]: Vamos precisar cancelar a reuniao de amanha, pode ser?", null)
                .getRiscos().stream().noneMatch(x -> "CHURN".equals(x.getTipo())));
        assertTrue(analyzer.analisar("[LOCUTOR 2]: Se continuar assim vamos cancelar o contrato e migrar para outro fornecedor.", null)
                .getRiscos().stream().anyMatch(x -> "CHURN".equals(x.getTipo())));
    }

    @Test
    void negacaoNaoContaComoProblema() {
        AiAnalysisDTO r = analyzer.analisar("[LOCUTOR 2]: Nao temos nenhum problema com o Protheus, esta funcionando bem.", null);
        assertEquals("POSITIVO", r.getSentimentoGeral());
        assertTrue(r.getRiscos().isEmpty());
    }
}
