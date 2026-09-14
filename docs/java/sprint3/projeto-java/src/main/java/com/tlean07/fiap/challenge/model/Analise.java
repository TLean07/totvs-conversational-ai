package com.tlean07.fiap.challenge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resultado da analise de uma transcricao (tabela ANALISES e suas tabelas filhas).
 * A lista de insights guarda oportunidades e riscos de forma polimorfica.
 */
public class Analise {

    private String id;
    private String ownerId;
    private String transcricaoId;
    private Sentimento sentimentoGeral;
    private String resumo;
    private String personaIdentificada;
    private String budgetIdentificado;
    private double scoreConfiancaVendedor;
    private double scoreSentimento;
    private String proximaAcao;
    private LocalDateTime dataAnalise;
    private final List<Produto> produtos = new ArrayList<>();
    private final List<Insight> insights = new ArrayList<>();

    public Analise() {
        this.id = UUID.randomUUID().toString();
        this.dataAnalise = LocalDateTime.now();
        this.sentimentoGeral = Sentimento.NEUTRO;
    }

    public List<Oportunidade> getOportunidades() {
        return insights.stream().filter(i -> i instanceof Oportunidade).map(i -> (Oportunidade) i).collect(Collectors.toList());
    }

    public List<Risco> getRiscos() {
        return insights.stream().filter(i -> i instanceof Risco).map(i -> (Risco) i).collect(Collectors.toList());
    }

    public List<Produto> getConcorrentes() {
        return produtos.stream().filter(Produto::isConcorrente).collect(Collectors.toList());
    }

    /**
     * Regra de negocio: score de risco da conta = soma dos pesos dos insights (riscos positivos,
     * oportunidades negativos), com penalidade por sentimento negativo. Nunca fica abaixo de zero.
     */
    public double calcularScoreRisco() {
        double soma = insights.stream().mapToDouble(Insight::calcularPeso).sum();
        if (sentimentoGeral == Sentimento.NEGATIVO) {
            soma += 3.0;
        } else if (sentimentoGeral == Sentimento.MISTO) {
            soma += 1.0;
        }
        return Math.max(0.0, soma);
    }

    /**
     * Regra de negocio: a proxima acao depende do que foi encontrado.
     */
    public String definirProximaAcao() {
        boolean churn = getRiscos().stream().anyMatch(r -> r.getTipo() == TipoRisco.CHURN && r.getSeveridade() == Severidade.ALTO);
        boolean perdaConfianca = getRiscos().stream().anyMatch(r -> r.getTipo() == TipoRisco.PERDA_CONFIANCA);
        boolean oportunidadeAlta = getOportunidades().stream().anyMatch(Oportunidade::isAlta);
        if (churn) {
            proximaAcao = "Acionar plano de retencao imediato com a lideranca comercial.";
        } else if (perdaConfianca) {
            proximaAcao = "Envolver a diretoria de CS e apresentar plano de comunicacao antes de qualquer proposta.";
        } else if (oportunidadeAlta && budgetIdentificado != null) {
            proximaAcao = "Enviar proposta com o budget mencionado (" + budgetIdentificado + ") nos proximos dias.";
        } else if (oportunidadeAlta) {
            proximaAcao = "Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.";
        } else if (!getOportunidades().isEmpty()) {
            proximaAcao = "Qualificar necessidade, budget e prazo antes de enviar proposta.";
        } else {
            proximaAcao = "Manter acompanhamento e registrar os pontos da reuniao no CRM.";
        }
        return proximaAcao;
    }

    /** Resumo textual da analise, no mesmo formato usado pela LeanIA. */
    public String gerarResumo() {
        StringBuilder sb = new StringBuilder();
        List<Produto> atuais = produtos.stream().filter(p -> p.getTipo() == TipoProduto.ATUAL).collect(Collectors.toList());
        if (!atuais.isEmpty()) {
            sb.append("Cliente utiliza ").append(atuais.stream().map(Produto::getNome).collect(Collectors.joining(", "))).append(". ");
        }
        sb.append("Sentimento ").append(sentimentoGeral.name().toLowerCase()).append(". ");
        for (Oportunidade o : getOportunidades()) {
            sb.append(o.isAlta() ? "Alta" : "Possivel").append(" probabilidade de venda (").append(o.getProdutoRelacionado()).append("). ");
        }
        for (Risco r : getRiscos()) {
            sb.append("Risco de ").append(r.getTipo().name().toLowerCase()).append(" (").append(r.getSeveridade().name().toLowerCase()).append("). ");
        }
        if (personaIdentificada != null) {
            sb.append("Interlocutor: ").append(personaIdentificada).append(". ");
        }
        if (budgetIdentificado != null) {
            sb.append("Budget mencionado: ").append(budgetIdentificado).append(". ");
        }
        sb.append("Proxima acao sugerida: ").append(definirProximaAcao());
        resumo = sb.toString();
        return resumo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTranscricaoId() { return transcricaoId; }
    public void setTranscricaoId(String transcricaoId) { this.transcricaoId = transcricaoId; }
    public Sentimento getSentimentoGeral() { return sentimentoGeral; }
    public void setSentimentoGeral(Sentimento sentimentoGeral) { this.sentimentoGeral = sentimentoGeral; }
    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }
    public String getPersonaIdentificada() { return personaIdentificada; }
    public void setPersonaIdentificada(String personaIdentificada) { this.personaIdentificada = personaIdentificada; }
    public String getBudgetIdentificado() { return budgetIdentificado; }
    public void setBudgetIdentificado(String budgetIdentificado) { this.budgetIdentificado = budgetIdentificado; }
    public double getScoreConfiancaVendedor() { return scoreConfiancaVendedor; }
    public void setScoreConfiancaVendedor(double scoreConfiancaVendedor) { this.scoreConfiancaVendedor = scoreConfiancaVendedor; }
    public double getScoreSentimento() { return scoreSentimento; }
    public void setScoreSentimento(double scoreSentimento) { this.scoreSentimento = scoreSentimento; }
    public String getProximaAcao() { return proximaAcao; }
    public void setProximaAcao(String proximaAcao) { this.proximaAcao = proximaAcao; }
    public LocalDateTime getDataAnalise() { return dataAnalise; }
    public void setDataAnalise(LocalDateTime dataAnalise) { this.dataAnalise = dataAnalise; }
    public List<Produto> getProdutos() { return produtos; }
    public List<Insight> getInsights() { return insights; }

    @Override
    public String toString() {
        return "Analise{" + sentimentoGeral + " | produtos=" + produtos.size() + " | oportunidades=" + getOportunidades().size()
                + " | riscos=" + getRiscos().size() + " | scoreRisco=" + String.format("%.1f", calcularScoreRisco()) + "}";
    }
}
