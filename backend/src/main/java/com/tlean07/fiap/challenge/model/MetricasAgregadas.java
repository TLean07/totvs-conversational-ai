package com.tlean07.fiap.challenge.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricasAgregadas {
    private int totalPartes;
    private int partesAnalisadas;
    private int partesNegativas;
    private int partesNeutras;
    private int partesPositivas;
    private int riscosDetectados;
    private int oportunidadesDetectadas;
    private int churnsPotenciais;
    private int mencoesCriticas;
    private int alertasConsolidados;
    private int chunksProcessados;
}
