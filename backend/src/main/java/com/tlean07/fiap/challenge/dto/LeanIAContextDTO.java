package com.tlean07.fiap.challenge.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeanIAContextDTO {
    private List<String> produtosFrequentes;
    private List<String> concorrentesFrequentes;
    private List<String> riscosFrequentes;
    private List<String> oportunidadesFrequentes;
}
