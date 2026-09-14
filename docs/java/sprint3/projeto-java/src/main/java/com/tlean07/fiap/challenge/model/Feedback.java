package com.tlean07.fiap.challenge.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avaliacao do usuario sobre uma analise (tabela ANALISE_FEEDBACKS). Alimenta o aprendizado da LeanIA.
 */
public class Feedback {

    private String id;
    private String ownerId;
    private String transcricaoId;
    private String analiseId;
    private boolean util;
    private String ajuste;
    private LocalDateTime criadoEm;

    public Feedback() {
        this.id = UUID.randomUUID().toString();
        this.criadoEm = LocalDateTime.now();
    }

    public Feedback(String ownerId, String transcricaoId, String analiseId, boolean util, String ajuste) {
        this();
        this.ownerId = ownerId;
        this.transcricaoId = transcricaoId;
        this.analiseId = analiseId;
        this.util = util;
        this.ajuste = ajuste;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTranscricaoId() { return transcricaoId; }
    public void setTranscricaoId(String transcricaoId) { this.transcricaoId = transcricaoId; }
    public String getAnaliseId() { return analiseId; }
    public void setAnaliseId(String analiseId) { this.analiseId = analiseId; }
    public boolean isUtil() { return util; }
    public void setUtil(boolean util) { this.util = util; }
    public String getAjuste() { return ajuste; }
    public void setAjuste(String ajuste) { this.ajuste = ajuste; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public String toString() {
        return "Feedback{" + (util ? "util" : "precisa ajuste") + (ajuste != null ? ": " + ajuste : "") + "}";
    }
}
