package com.tlean07.fiap.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TranscricaoImportItemResponse {
    private int index;
    private String meetingId;
    private String transcriptionId;
    private String status;
    private String reason;
    private List<String> warnings;
}
