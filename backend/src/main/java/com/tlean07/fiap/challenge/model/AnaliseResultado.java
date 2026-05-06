package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Sentimento;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "analises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseResultado {

    @Id
    private String id;

    @Indexed
    private String ownerId;
    @Indexed
    private String transcricaoId;
    private Sentimento sentimentoGeral;
    private String resumo;
    private String personaIdentificada;
    private String budgetIdentificado;
    private Double scoreConfiancaVendedor;
    private Double scoreSentimento;
    private MetricasAgregadas metricasAgregadas;
    private String resumoExecutivo;
    @Indexed
    private LocalDateTime dataAnalise;

    @Builder.Default
    private List<ProdutoIdentificado> produtosIdentificados = new ArrayList<>();

    @Builder.Default
    private List<Oportunidade> oportunidades = new ArrayList<>();

    @Builder.Default
    private List<Risco> riscos = new ArrayList<>();

    @Builder.Default
    private List<Alerta> alertas = new ArrayList<>();

    @Builder.Default
    private List<RiscoConsolidado> riscosConsolidados = new ArrayList<>();

    @Builder.Default
    private List<OportunidadeConsolidada> oportunidadesConsolidadas = new ArrayList<>();

    @Builder.Default
    private List<AlertaConsolidado> alertasConsolidados = new ArrayList<>();

    @Builder.Default
    private List<ItemCritico> itensCriticos = new ArrayList<>();

    public void prePersist() {
        dataAnalise = LocalDateTime.now();
    }
}
