package com.tlean07.fiap.challenge.model;

/**
 * Produto TOTVS ou concorrente identificado na reuniao (tabela ANALISE_PRODUTOS).
 */
public class Produto {

    private String nome;
    private TipoProduto tipo;
    private String contexto;

    public Produto() {
    }

    public Produto(String nome, TipoProduto tipo, String contexto) {
        this.nome = nome;
        this.tipo = tipo;
        this.contexto = contexto;
    }

    public boolean isConcorrente() {
        return tipo == TipoProduto.CONCORRENTE;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoProduto getTipo() { return tipo; }
    public void setTipo(TipoProduto tipo) { this.tipo = tipo; }
    public String getContexto() { return contexto; }
    public void setContexto(String contexto) { this.contexto = contexto; }

    @Override
    public String toString() {
        return nome + " (" + tipo + ")";
    }
}
