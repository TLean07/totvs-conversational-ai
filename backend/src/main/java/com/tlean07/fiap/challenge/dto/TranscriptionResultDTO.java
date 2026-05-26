package com.tlean07.fiap.challenge.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionResultDTO {
    private String transcript;
    private String language;
    private Double durationSeconds;
    private String model;
    private Integer processingTimeMs;
    @Builder.Default
    private List<TranscriptionSegmentDTO> segments = new ArrayList<>();
    private Double confidence;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
