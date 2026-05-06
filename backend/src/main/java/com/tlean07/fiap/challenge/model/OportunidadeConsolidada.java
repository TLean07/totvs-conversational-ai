package com.tlean07.fiap.challenge.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OportunidadeConsolidada {
    private String categoria;
    private Double score;
    private int ocorrencias;
    @Builder.Default
    private List<String> evidencias = new ArrayList<>();
    private String recomendacao;
}
