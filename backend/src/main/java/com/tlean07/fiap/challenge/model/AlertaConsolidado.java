package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaConsolidado {
    private String titulo;
    private String categoria;
    private Severidade severidade;
    private int ocorrencias;
    private String motivo;
    private String recomendacao;
}
