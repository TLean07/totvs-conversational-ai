package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiscoConsolidado {
    private String categoria;
    private Severidade severidade;
    private int ocorrencias;
    @Builder.Default
    private List<String> evidencias = new ArrayList<>();
    private String recomendacao;
}
