package com.tlean07.fiap.challenge.model;

/**
 * Classe abstrata que concentra o que oportunidades e riscos tem em comum
 * (heranca; as subclasses sobrescrevem getCategoria e calcularPeso - polimorfismo).
 */
public abstract class Insight {

    private String descricao;
    private String evidencia;

    protected Insight(String descricao, String evidencia) {
        this.descricao = descricao;
        this.evidencia = evidencia;
    }

    /** Nome da categoria exibido nos relatorios e nos alertas. */
    public abstract String getCategoria();

    /**
     * Peso do insight na priorizacao da conta: riscos pesam positivo (aumentam a urgencia),
     * oportunidades pesam negativo (a conta esta saudavel e com potencial).
     */
    public abstract double calcularPeso();

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }

    @Override
    public String toString() {
        return getCategoria() + ": " + descricao + " | Evidencia: \"" + evidencia + "\"";
    }
}
