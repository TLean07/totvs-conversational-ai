package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dao.AlertaDAO;
import com.tlean07.fiap.challenge.dao.AnaliseDAO;
import com.tlean07.fiap.challenge.dao.TranscricaoDAO;
import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Transcricao;

import java.util.List;

/**
 * Orquestra o fluxo completo de uma reuniao: validar, gravar, analisar, gerar alertas e
 * atualizar o status, usando os DAOs. E o mesmo fluxo executado pelo backend Spring da solucao.
 */
public class TranscricaoService {

    private final TranscricaoDAO transcricaoDAO = new TranscricaoDAO();
    private final AnaliseDAO analiseDAO = new AnaliseDAO();
    private final AlertaDAO alertaDAO = new AlertaDAO();
    private final AnalisadorTranscricao analisador = new AnalisadorTranscricao();
    private final GeradorAlertas geradorAlertas = new GeradorAlertas();

    /** Cadastra a reuniao (status PENDENTE) depois de validar as regras de negocio. */
    public Transcricao cadastrar(Transcricao transcricao) {
        transcricao.validar();
        transcricaoDAO.inserir(transcricao);
        return transcricao;
    }

    /** PENDENTE -> PROCESSANDO -> PROCESSADA, gravando a analise e os alertas gerados. */
    public Analise processar(Transcricao transcricao) {
        transcricao.iniciarProcessamento();
        transcricaoDAO.atualizar(transcricao);
        try {
            Analise analise = analisador.analisar(transcricao);
            analiseDAO.inserir(analise);
            List<Alerta> alertas = geradorAlertas.gerar(transcricao, analise);
            if (!alertas.isEmpty()) {
                alertaDAO.inserirTodos(alertas);
            }
            transcricao.concluirProcessamento(analise);
            transcricaoDAO.atualizar(transcricao);
            return analise;
        } catch (RuntimeException e) {
            transcricao.registrarErro();
            transcricaoDAO.atualizar(transcricao);
            throw e;
        }
    }

    /** Remove a reuniao; a analise, os detalhes e os alertas saem em cascata no banco. */
    public void excluir(Transcricao transcricao) {
        transcricaoDAO.excluir(transcricao.getId());
    }
}
