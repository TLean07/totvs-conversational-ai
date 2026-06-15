package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.OportunidadeConsolidada;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.RiscoConsolidado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscricaoInsightsResponse {
    private String id;
    private String cliente;
    private List<RiscoConsolidado> riscosConsolidados;
    private List<OportunidadeConsolidada> oportunidadesConsolidadas;
    private List<Risco> riscos;
    private List<Oportunidade> oportunidades;
}
