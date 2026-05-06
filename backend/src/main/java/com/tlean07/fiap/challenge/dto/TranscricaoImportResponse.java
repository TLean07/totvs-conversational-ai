package com.tlean07.fiap.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TranscricaoImportResponse {
    private int totalRegistros;
    private int importados;
    private int ignorados;
    private List<String> avisos;
}
