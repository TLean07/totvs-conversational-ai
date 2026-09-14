package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.Produto;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.Severidade;
import com.tlean07.fiap.challenge.model.TipoRisco;
import com.tlean07.fiap.challenge.model.Transcricao;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Metodo de relevancia 2: gera os alertas acionaveis a partir de uma analise, aplicando as
 * regras de prioridade do produto e evitando alertas duplicados para a mesma reuniao.
 */
public class GeradorAlertas {

    public List<Alerta> gerar(Transcricao transcricao, Analise analise) {
        List<Alerta> alertas = new ArrayList<>();
        Set<String> chaves = new LinkedHashSet<>();
        String cliente = transcricao.getCliente();

        for (Risco r : analise.getRiscos()) {
            String tipo = "RISCO_" + r.getTipo().name();
            if (!chaves.add(tipo)) {
                continue; // um alerta por tipo de risco
            }
            Severidade prioridade = prioridadeDoRisco(r);
            String mensagem = "Risco " + r.getTipo() + " detectado para cliente " + cliente + ": " + r.getDescricao()
                    + ". Trecho: \"" + r.getEvidencia() + "\"";
            alertas.add(criar(tipo, tipo, tipo, mensagem, prioridade, transcricao, analise));
        }

        for (Produto p : analise.getConcorrentes()) {
            String chave = "CONCORRENTE_" + p.getNome();
            if (chaves.add(chave)) {
                alertas.add(criar("CONCORRENTE_DETECTADO", "CONCORRENTE_DETECTADO", "CONCORRENTE_DETECTADO",
                        "Concorrente " + p.getNome() + " mencionado pelo cliente " + cliente + ": \"" + p.getContexto() + "\"",
                        Severidade.MEDIO, transcricao, analise));
            }
        }

        for (Oportunidade o : analise.getOportunidades()) {
            if (o.isAlta() && chaves.add("OPORTUNIDADE_" + o.getTipo())) {
                alertas.add(criar("OPORTUNIDADE_ALTA", "Oportunidade de " + o.getTipo() + ": " + o.getProdutoRelacionado(), "OPORTUNIDADE_ALTA",
                        "Oportunidade de " + o.getTipo() + " com alta probabilidade: " + o.getDescricao(), Severidade.MEDIO, transcricao, analise));
            }
        }

        // churn iminente: churn ALTO junto com concorrente citado ou perda de confianca
        boolean churnAlto = analise.getRiscos().stream().anyMatch(r -> r.getTipo() == TipoRisco.CHURN && r.getSeveridade() == Severidade.ALTO);
        boolean agravante = !analise.getConcorrentes().isEmpty() || analise.getRiscos().stream().anyMatch(r -> r.getTipo() == TipoRisco.PERDA_CONFIANCA);
        if (churnAlto && agravante && chaves.add("CHURN_IMINENTE")) {
            alertas.add(0, criar("CHURN_IMINENTE", "Churn iminente", "CHURN_IMINENTE",
                    "Cliente " + cliente + " combina intencao de cancelamento com concorrente em negociacao ou perda de confianca. Acionar a lideranca hoje.",
                    Severidade.ALTO, transcricao, analise));
        }
        return alertas;
    }

    /** Regra: churn e perda de confianca sao sempre ALTO; os demais seguem a severidade do risco. */
    Severidade prioridadeDoRisco(Risco r) {
        if (r.getTipo() == TipoRisco.CHURN || r.getTipo() == TipoRisco.PERDA_CONFIANCA) {
            return Severidade.ALTO;
        }
        return r.getSeveridade();
    }

    private Alerta criar(String tipo, String titulo, String categoria, String mensagem, Severidade prioridade, Transcricao t, Analise a) {
        Alerta alerta = new Alerta(tipo, titulo, categoria, mensagem, prioridade);
        alerta.setOwnerId(t.getOwnerId());
        alerta.setAnaliseId(a.getId());
        return alerta;
    }
}
