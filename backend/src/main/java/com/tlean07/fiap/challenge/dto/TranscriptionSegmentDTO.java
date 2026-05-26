package com.tlean07.fiap.challenge.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionSegmentDTO {
    private Double start;
    private Double end;
    private String text;
    private Double confidence;
}
