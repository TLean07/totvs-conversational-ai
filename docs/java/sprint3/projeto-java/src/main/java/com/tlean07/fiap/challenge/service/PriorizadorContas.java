package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Transcricao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Metodo de relevancia 3: ordena as contas (reunioes analisadas) por urgencia de atuacao.
 * Score = score de risco da analise + penalidade por NPS baixo + bonus de recencia.
 * Contas sem analise ficam no fim da lista.
 */
public class PriorizadorContas {

    /** Resultado da priorizacao de uma conta. */
    public static class ContaPriorizada {
        private final Transcricao transcricao;
        private final double score;
        private final String motivo;

        ContaPriorizada(Transcricao transcricao, double score, String motivo) {
            this.transcricao = transcricao;
            this.score = score;
            this.motivo = motivo;
        }

        public Transcricao getTranscricao() { return transcricao; }
        public double getScore() { return score; }
        public String getMotivo() { return motivo; }

        @Override
        public String toString() {
            return String.format("%-28s score %5.1f  %s", transcricao.getCliente(), score, motivo);
        }
    }

    public List<ContaPriorizada> rankear(List<Transcricao> transcricoes) {
        List<ContaPriorizada> contas = new ArrayList<>();
        for (Transcricao t : transcricoes) {
            contas.add(avaliar(t));
        }
        contas.sort(Comparator.comparingDouble(ContaPriorizada::getScore).reversed());
        return contas;
    }

    public ContaPriorizada avaliar(Transcricao t) {
        Analise a = t.getAnalise();
        if (a == null) {
            return new ContaPriorizada(t, -1.0, "sem analise (" + t.getStatus() + ")");
        }
        double score = a.calcularScoreRisco();
        StringBuilder motivo = new StringBuilder();
        if (!a.getRiscos().isEmpty()) {
            motivo.append(a.getRiscos().size()).append(" risco(s)");
        }
        if (t.getNotaNps() != null) {
            if (t.getNotaNps() <= 6) {
                score += 3.0;           // detrator
                motivo.append(motivo.length() > 0 ? ", " : "").append("NPS detrator (").append(t.getNotaNps()).append(")");
            } else if (t.getNotaNps() >= 9) {
                score -= 1.0;           // promotor
                motivo.append(motivo.length() > 0 ? ", " : "").append("NPS promotor");
            }
        }
        if (t.getDataReuniao() != null && t.getDataReuniao().isAfter(java.time.LocalDateTime.now().minusDays(7))) {
            score += 1.0;               // reuniao recente: janela de acao aberta
            motivo.append(motivo.length() > 0 ? ", " : "").append("reuniao recente");
        }
        if (!a.getOportunidades().isEmpty()) {
            motivo.append(motivo.length() > 0 ? ", " : "").append(a.getOportunidades().size()).append(" oportunidade(s)");
        }
        if (motivo.length() == 0) {
            motivo.append("sem sinais relevantes");
        }
        return new ContaPriorizada(t, Math.max(0.0, Math.round(score * 10.0) / 10.0), motivo.toString());
    }
}
