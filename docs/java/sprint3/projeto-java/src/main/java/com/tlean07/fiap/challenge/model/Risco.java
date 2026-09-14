package com.tlean07.fiap.challenge.model;

/**
 * Risco de negocio detectado na reuniao (tabela ANALISE_RISCOS).
 */
public class Risco extends Insight {

    private TipoRisco tipo;
    private Severidade severidade;
    private String concorrenteMencionado;

    public Risco(TipoRisco tipo, String descricao, String evidencia, Severidade severidade, String concorrenteMencionado) {
        super(descricao, evidencia);
        this.tipo = tipo;
        this.severidade = severidade;
        this.concorrenteMencionado = concorrenteMencionado;
    }

    @Override
    public String getCategoria() {
        return "RISCO " + tipo;
    }

    /** Churn e perda de confianca pesam mais; a severidade multiplica o peso base. */
    @Override
    public double calcularPeso() {
        double base;
        switch (tipo) {
            case CHURN -> base = 5.0;
            case PERDA_CONFIANCA -> base = 4.0;
            case CONCORRENCIA -> base = 3.0;
            default -> base = 2.0;
        }
        double fator = switch (severidade) {
            case ALTO -> 1.5;
            case MEDIO -> 1.0;
            case BAIXO -> 0.5;
        };
        return base * fator;
    }

    public TipoRisco getTipo() { return tipo; }
    public void setTipo(TipoRisco tipo) { this.tipo = tipo; }
    public Severidade getSeveridade() { return severidade; }
    public void setSeveridade(Severidade severidade) { this.severidade = severidade; }
    public String getConcorrenteMencionado() { return concorrenteMencionado; }
    public void setConcorrenteMencionado(String concorrenteMencionado) { this.concorrenteMencionado = concorrenteMencionado; }

    @Override
    public String toString() {
        return super.toString() + " | Severidade: " + severidade + (concorrenteMencionado != null ? " | Concorrente: " + concorrenteMencionado : "");
    }
}
