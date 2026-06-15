package com.tlean07.fiap.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TranscricaoImportResponse {
    private boolean success;
    private String sourceFile;
    private String batchId;
    private int found;
    private int imported;
    private int skipped;
    private int failed;
    private List<TranscricaoImportItemResponse> items;

    // Campos legados mantidos para clientes que ainda usam o contrato anterior.
    private int totalRegistros;
    private int importados;
    private int ignorados;
    private List<String> avisos;
}
