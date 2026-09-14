package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.exception.RegraNegocioException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transcricao de uma reuniao comercial (tabela TRANSCRICOES). 1 reuniao = 1 objeto.
 * Guarda a referencia para a analise atual (atributo de referencia, relacionamento 1:1).
 */
public class Transcricao {

    public static final int TAMANHO_MINIMO_TEXTO = 20;

    private String id;
    private String ownerId;
    private String textoOriginal;
    private String participantes;
    private LocalDateTime dataReuniao;
    private Integer duracaoMinutos;
    private String cliente;
    private String vendedor;
    private String origemTranscricao;
    private String meetingId;
    private String uf;
    private String nomeSegmento;
    private Integer notaNps;
    private StatusTranscricao status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataProcessamento;
    private Analise analise;

    public Transcricao() {
        this.id = UUID.randomUUID().toString();
        this.status = StatusTranscricao.PENDENTE;
        this.dataCriacao = LocalDateTime.now();
        this.origemTranscricao = "manual";
    }

    public Transcricao(String cliente, String vendedor, String textoOriginal) {
        this();
        this.cliente = cliente;
        this.vendedor = vendedor;
        this.textoOriginal = textoOriginal;
    }

    /**
     * Regra de negocio: uma transcricao so pode ser analisada se tiver cliente, texto com
     * tamanho minimo e duracao positiva (quando informada).
     */
    public void validar() {
        if (cliente == null || cliente.isBlank()) {
            throw new RegraNegocioException("Cliente e obrigatorio");
        }
        if (textoOriginal == null || textoOriginal.trim().length() < TAMANHO_MINIMO_TEXTO) {
            throw new RegraNegocioException("Texto da transcricao deve ter ao menos " + TAMANHO_MINIMO_TEXTO + " caracteres");
        }
        if (duracaoMinutos != null && duracaoMinutos <= 0) {
            throw new RegraNegocioException("Duracao da reuniao deve ser positiva");
        }
        if (notaNps != null && (notaNps < 0 || notaNps > 10)) {
            throw new RegraNegocioException("Nota NPS deve estar entre 0 e 10");
        }
    }

    /**
     * Maquina de estados do processamento: PENDENTE -> PROCESSANDO.
     * Uma transcricao com ERRO tambem pode voltar para a fila (reprocessamento).
     */
    public void iniciarProcessamento() {
        if (status != StatusTranscricao.PENDENTE && status != StatusTranscricao.ERRO && status != StatusTranscricao.PROCESSADA) {
            throw new RegraNegocioException("Transcricao " + id + " ja esta em processamento");
        }
        validar();
        this.status = StatusTranscricao.PROCESSANDO;
    }

    /** PROCESSANDO -> PROCESSADA, vinculando a analise atual. */
    public void concluirProcessamento(Analise analise) {
        if (status != StatusTranscricao.PROCESSANDO) {
            throw new RegraNegocioException("Somente uma transcricao em PROCESSANDO pode ser concluida (status atual: " + status + ")");
        }
        if (analise == null) {
            throw new RegraNegocioException("Analise nao pode ser nula ao concluir o processamento");
        }
        this.analise = analise;
        analise.setTranscricaoId(this.id);
        this.status = StatusTranscricao.PROCESSADA;
        this.dataProcessamento = LocalDateTime.now();
    }

    /** PROCESSANDO -> ERRO. */
    public void registrarErro() {
        if (status != StatusTranscricao.PROCESSANDO) {
            throw new RegraNegocioException("Somente uma transcricao em PROCESSANDO pode ir para ERRO");
        }
        this.status = StatusTranscricao.ERRO;
    }

    public boolean isProcessada() {
        return status == StatusTranscricao.PROCESSADA && analise != null;
    }

    /** Trecho curto para listagens. */
    public String resumoCurto(int tamanho) {
        if (textoOriginal == null) {
            return "";
        }
        String limpo = textoOriginal.replaceAll("\\s+", " ").trim();
        return limpo.length() <= tamanho ? limpo : limpo.substring(0, tamanho) + "...";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTextoOriginal() { return textoOriginal; }
    public void setTextoOriginal(String textoOriginal) { this.textoOriginal = textoOriginal; }
    public String getParticipantes() { return participantes; }
    public void setParticipantes(String participantes) { this.participantes = participantes; }
    public LocalDateTime getDataReuniao() { return dataReuniao; }
    public void setDataReuniao(LocalDateTime dataReuniao) { this.dataReuniao = dataReuniao; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getVendedor() { return vendedor; }
    public void setVendedor(String vendedor) { this.vendedor = vendedor; }
    public String getOrigemTranscricao() { return origemTranscricao; }
    public void setOrigemTranscricao(String origemTranscricao) { this.origemTranscricao = origemTranscricao; }
    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getNomeSegmento() { return nomeSegmento; }
    public void setNomeSegmento(String nomeSegmento) { this.nomeSegmento = nomeSegmento; }
    public Integer getNotaNps() { return notaNps; }
    public void setNotaNps(Integer notaNps) { this.notaNps = notaNps; }
    public StatusTranscricao getStatus() { return status; }
    public void setStatus(StatusTranscricao status) { this.status = status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public LocalDateTime getDataProcessamento() { return dataProcessamento; }
    public void setDataProcessamento(LocalDateTime dataProcessamento) { this.dataProcessamento = dataProcessamento; }
    public Analise getAnalise() { return analise; }
    public void setAnalise(Analise analise) { this.analise = analise; }

    @Override
    public String toString() {
        return "Transcricao{" + cliente + " | " + vendedor + " | " + status + " | " + resumoCurto(60) + "}";
    }
}
