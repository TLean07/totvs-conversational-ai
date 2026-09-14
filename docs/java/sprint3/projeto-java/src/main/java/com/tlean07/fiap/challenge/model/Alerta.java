package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.exception.RegraNegocioException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Alerta acionavel gerado a partir de uma analise (tabela ALERTAS).
 * O ciclo de tratamento e NOVO -> VISTO -> RESOLVIDO; transicoes fora dessa ordem sao rejeitadas.
 */
public class Alerta {

    private String id;
    private String ownerId;
    private String analiseId;
    private String tipo;
    private String titulo;
    private String categoria;
    private String mensagem;
    private Severidade prioridade;
    private StatusAlerta status;
    private LocalDateTime dataCriacao;

    public Alerta() {
        this.id = UUID.randomUUID().toString();
        this.status = StatusAlerta.NOVO;
        this.dataCriacao = LocalDateTime.now();
    }

    public Alerta(String tipo, String titulo, String categoria, String mensagem, Severidade prioridade) {
        this();
        this.tipo = tipo;
        this.titulo = titulo;
        this.categoria = categoria;
        this.mensagem = mensagem;
        this.prioridade = prioridade;
    }

    /** NOVO -> VISTO. */
    public void marcarVisto() {
        if (status != StatusAlerta.NOVO) {
            throw new RegraNegocioException("Somente um alerta NOVO pode ser marcado como VISTO (status atual: " + status + ")");
        }
        this.status = StatusAlerta.VISTO;
    }

    /** VISTO -> RESOLVIDO (um alerta precisa ter sido visto antes de ser resolvido). */
    public void resolver() {
        if (status != StatusAlerta.VISTO) {
            throw new RegraNegocioException("Somente um alerta VISTO pode ser RESOLVIDO (status atual: " + status + ")");
        }
        this.status = StatusAlerta.RESOLVIDO;
    }

    public boolean isPendente() {
        return status != StatusAlerta.RESOLVIDO;
    }

    public boolean isAltaPrioridade() {
        return prioridade == Severidade.ALTO;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getAnaliseId() { return analiseId; }
    public void setAnaliseId(String analiseId) { this.analiseId = analiseId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public Severidade getPrioridade() { return prioridade; }
    public void setPrioridade(Severidade prioridade) { this.prioridade = prioridade; }
    public StatusAlerta getStatus() { return status; }
    public void setStatus(StatusAlerta status) { this.status = status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    @Override
    public String toString() {
        return "[" + prioridade + "] " + tipo + " - " + titulo + " (" + status + ")";
    }
}
