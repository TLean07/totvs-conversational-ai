package com.tlean07.fiap.challenge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Metricas consolidadas de uma transcricao/analise.
 * Componente embutido: vira colunas MET_* nas tabelas TRANSCRICOES e ANALISES.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricasAgregadas {
    @Column(name = "MET_TOTAL_PARTES")
    private int totalPartes;
    @Column(name = "MET_PARTES_ANALISADAS")
    private int partesAnalisadas;
    @Column(name = "MET_PARTES_NEGATIVAS")
    private int partesNegativas;
    @Column(name = "MET_PARTES_NEUTRAS")
    private int partesNeutras;
    @Column(name = "MET_PARTES_POSITIVAS")
    private int partesPositivas;
    @Column(name = "MET_RISCOS_DETECTADOS")
    private int riscosDetectados;
    @Column(name = "MET_OPORTUNIDADES_DETECTADAS")
    private int oportunidadesDetectadas;
    @Column(name = "MET_CHURNS_POTENCIAIS")
    private int churnsPotenciais;
    @Column(name = "MET_MENCOES_CRITICAS")
    private int mencoesCriticas;
    @Column(name = "MET_ALERTAS_CONSOLIDADOS")
    private int alertasConsolidados;
    @Column(name = "MET_CHUNKS_PROCESSADOS")
    private int chunksProcessados;
}
