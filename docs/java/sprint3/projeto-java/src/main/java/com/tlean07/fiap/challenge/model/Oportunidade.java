package com.tlean07.fiap.challenge.model;

/**
 * Oportunidade comercial detectada na reuniao (tabela ANALISE_OPORTUNIDADES).
 */
public class Oportunidade extends Insight {

    private TipoOportunidade tipo;
    private String produtoRelacionado;
    private double score;
    private String valorEstimado;

    public Oportunidade(TipoOportunidade tipo, String descricao, String evidencia, String produtoRelacionado, double score, String valorEstimado) {
        super(descricao, evidencia);
        this.tipo = tipo;
        this.produtoRelacionado = produtoRelacionado;
        this.score = score;
        this.valorEstimado = valorEstimado;
    }

    @Override
    public String getCategoria() {
        return "OPORTUNIDADE " + tipo;
    }

    /** Oportunidades fortes reduzem a urgencia de retencao: peso negativo proporcional ao score. */
    @Override
    public double calcularPeso() {
        return -2.0 * score;
    }

    public boolean isAlta() {
        return score >= 0.7;
    }

    public TipoOportunidade getTipo() { return tipo; }
    public void setTipo(TipoOportunidade tipo) { this.tipo = tipo; }
    public String getProdutoRelacionado() { return produtoRelacionado; }
    public void setProdutoRelacionado(String produtoRelacionado) { this.produtoRelacionado = produtoRelacionado; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(String valorEstimado) { this.valorEstimado = valorEstimado; }

    @Override
    public String toString() {
        return super.toString() + " | Produto: " + produtoRelacionado + " | Score: " + String.format("%.0f%%", score * 100) + " | Valor: " + valorEstimado;
    }
}
