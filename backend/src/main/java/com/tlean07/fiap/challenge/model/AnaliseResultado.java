package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Sentimento;
import com.tlean07.fiap.challenge.model.converter.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resultado da analise de uma transcricao (tabela ANALISES).
 * As listas ficam em tabelas filhas ANALISE_*; sao carregadas junto com a entidade
 * porque o job assincrono le a analise fora de uma sessao JPA aberta.
 */
@Entity
@Table(name = "ANALISES", indexes = {
        @Index(name = "IDX_ANALISES_OWNER", columnList = "OWNER_ID"),
        @Index(name = "IDX_ANALISES_TRANSCRICAO", columnList = "TRANSCRICAO_ID"),
        @Index(name = "IDX_ANALISES_DATA", columnList = "DATA_ANALISE")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseResultado {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "OWNER_ID", length = 36)
    private String ownerId;

    @Column(name = "TRANSCRICAO_ID", length = 36)
    private String transcricaoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "SENTIMENTO_GERAL", length = 20)
    private Sentimento sentimentoGeral;

    @Lob
    @Column(name = "RESUMO")
    private String resumo;

    @Column(name = "PERSONA_IDENTIFICADA", length = 500)
    private String personaIdentificada;

    @Column(name = "BUDGET_IDENTIFICADO", length = 500)
    private String budgetIdentificado;

    @Column(name = "SCORE_CONFIANCA_VENDEDOR")
    private Double scoreConfiancaVendedor;

    @Column(name = "SCORE_SENTIMENTO")
    private Double scoreSentimento;

    @Embedded
    private MetricasAgregadas metricasAgregadas;

    @Lob
    @Column(name = "RESUMO_EXECUTIVO")
    private String resumoExecutivo;

    @Column(name = "DATA_ANALISE")
    private LocalDateTime dataAnalise;

    /** Proxima acao comercial sugerida pela LeanIA. */
    @Column(name = "PROXIMA_ACAO", length = 500)
    private String proximaAcao;

    /** Ex.: "Misto (Satisfeito com Backoffice / Frustrado com RH)". */
    @Column(name = "SENTIMENTO_DETALHE", length = 300)
    private String sentimentoDetalhe;

    /** Elogios, reclamacoes, duvidas da jornada, proximos passos e observacoes (JSON). */
    @Lob
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "INSIGHTS_JSON")
    private Map<String, Object> insightsAdicionais;

    /** Indicadores da conversa: locutores, participacao, perguntas, escuta ativa (JSON). */
    @Lob
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "INDICADORES_JSON")
    private Map<String, Object> indicadoresQualidade;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "ANALISE_PRODUTOS",
            joinColumns = @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_ANALISE_PRODUTOS")))
    @OrderColumn(name = "POSICAO")
    private List<ProdutoIdentificado> produtosIdentificados = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "ANALISE_OPORTUNIDADES",
            joinColumns = @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_ANALISE_OPORTUNIDADES")))
    @OrderColumn(name = "POSICAO")
    private List<Oportunidade> oportunidades = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "ANALISE_RISCOS",
            joinColumns = @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_ANALISE_RISCOS")))
    @OrderColumn(name = "POSICAO")
    private List<Risco> riscos = new ArrayList<>();

    /** Alertas gerados para esta analise (tabela ALERTAS, coluna ANALISE_ID). Lado inverso, somente leitura. */
    @Builder.Default
    @OneToMany(mappedBy = "analise", fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @OrderBy("dataCriacao ASC")
    private List<Alerta> alertas = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "ANALISE_RISCOS_CONS",
            joinColumns = @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_ANALISE_RISCOS_CONS")))
    @OrderColumn(name = "POSICAO")
    private List<RiscoConsolidado> riscosConsolidados = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "ANALISE_OPORT_CONS",
            joinColumns = @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_ANALISE_OPORT_CONS")))
    @OrderColumn(name = "POSICAO")
    private List<OportunidadeConsolidada> oportunidadesConsolidadas = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "ANALISE_ALERTAS_CONS",
            joinColumns = @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_ANALISE_ALERTAS_CONS")))
    @OrderColumn(name = "POSICAO")
    private List<AlertaConsolidado> alertasConsolidados = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "ANALISE_ITENS_CRITICOS",
            joinColumns = @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_ANALISE_ITENS_CRITICOS")))
    @OrderColumn(name = "POSICAO")
    private List<ItemCritico> itensCriticos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (dataAnalise == null) {
            dataAnalise = LocalDateTime.now();
        }
    }
}
