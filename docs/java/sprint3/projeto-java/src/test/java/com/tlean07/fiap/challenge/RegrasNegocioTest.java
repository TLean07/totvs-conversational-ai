package com.tlean07.fiap.challenge;

import com.tlean07.fiap.challenge.exception.RegraNegocioException;
import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Sentimento;
import com.tlean07.fiap.challenge.model.Severidade;
import com.tlean07.fiap.challenge.model.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.service.AnalisadorTranscricao;
import com.tlean07.fiap.challenge.service.GeradorAlertas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes unitarios (JUnit 5) das regras de negocio, executados pelo Maven. */
class RegrasNegocioTest {

    @Test
    void reuniaoComChurnGeraRiscoAltoEAlertaIminente() {
        Transcricao t = new Transcricao("Clinica", "Rafael", "A Senior ja nos apresentou uma proposta. Se nao resolver, vamos cancelar o contrato.");
        Analise a = new AnalisadorTranscricao().analisar(t);
        assertEquals(Sentimento.NEGATIVO, a.getSentimentoGeral());
        assertTrue(a.getRiscos().stream().anyMatch(r -> r.getSeveridade() == Severidade.ALTO));
        List<Alerta> alertas = new GeradorAlertas().gerar(t, a);
        assertTrue(alertas.stream().anyMatch(al -> al.getTipo().equals("CHURN_IMINENTE")));
    }

    @Test
    void cicloDeStatusDaTranscricao() {
        Transcricao t = new Transcricao("Cliente", "Vendedor", "Texto suficientemente longo para a analise.");
        t.iniciarProcessamento();
        assertEquals(StatusTranscricao.PROCESSANDO, t.getStatus());
        assertThrows(RegraNegocioException.class, t::iniciarProcessamento);
        t.concluirProcessamento(new Analise());
        assertEquals(StatusTranscricao.PROCESSADA, t.getStatus());
    }

    @Test
    void alertaNaoPodeSerResolvidoSemSerVisto() {
        Alerta a = new Alerta("RISCO_CHURN", "Churn", "RISCO_CHURN", "msg", Severidade.ALTO);
        assertThrows(RegraNegocioException.class, a::resolver);
        a.marcarVisto();
        a.resolver();
        assertTrue(!a.isPendente());
    }

    @Test
    void budgetComMilEPorMes() {
        Transcricao t = new Transcricao("Cliente", "Vendedor", "Nosso limite e de R$ 8 mil por mes para o modulo de cobranca. Gostei da proposta.");
        Analise a = new AnalisadorTranscricao().analisar(t);
        assertEquals("R$ 8.000,00 por mes", a.getBudgetIdentificado());
    }
}
