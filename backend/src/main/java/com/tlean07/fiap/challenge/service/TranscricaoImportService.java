package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tlean07.fiap.challenge.dto.TranscricaoImportItemResponse;
import com.tlean07.fiap.challenge.dto.TranscricaoImportResponse;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TranscricaoImportService {

    private static final Logger log = LoggerFactory.getLogger(TranscricaoImportService.class);
    private static final Pattern LOCUTOR_PATTERN = Pattern.compile("\\[LOCUTOR\\s+\\d+]");
    private static final List<DateTimeFormatter> SOURCE_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private final TotvsTranscriptionJsonParser parser;
    private final TranscricaoRepository transcricaoRepository;
    private final TranscricaoAnalysisJob analysisJob;
    private final AuditService auditService;

    public TranscricaoImportService(TotvsTranscriptionJsonParser parser,
                                    TranscricaoRepository transcricaoRepository,
                                    TranscricaoAnalysisJob analysisJob,
                                    AuditService auditService) {
        this.parser = parser;
        this.transcricaoRepository = transcricaoRepository;
        this.analysisJob = analysisJob;
        this.auditService = auditService;
    }

    public TranscricaoImportResponse importar(MultipartFile file, String ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo JSON obrigatorio");
        }

        String sourceFile = safeFileName(file.getOriginalFilename());
        String batchId = UUID.randomUUID().toString();
        LocalDateTime importedAt = LocalDateTime.now();
        ImportResult result = new ImportResult(sourceFile, batchId);
        Set<String> meetingIdsInFile = new HashSet<>();

        try {
            TotvsTranscriptionJsonParser.ParseSummary summary = parser.parse(
                    file.getInputStream(),
                    (index, record) -> processRecord(
                            index,
                            record,
                            ownerId,
                            sourceFile,
                            batchId,
                            importedAt,
                            meetingIdsInFile,
                            result
                    )
            );
            result.found = summary.found();

            if (!summary.transcriptionFieldFound()) {
                throw new IllegalArgumentException("Nenhum campo ANON_TRANSCRICAO foi encontrado no arquivo JSON");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Nao foi possivel ler o arquivo JSON. Verifique se o formato e valido.");
        }

        log.info("[TranscriptionImport] file={} found={}", sourceFile, result.found);
        auditService.record(
                "transcricao.import.totvs",
                ownerId,
                sourceFile,
                "batchId=" + batchId
                        + ";found=" + result.found
                        + ";imported=" + result.imported
                        + ";skipped=" + result.skipped
                        + ";failed=" + result.failed
        );

        return result.toResponse();
    }

    private void processRecord(int index,
                               JsonNode record,
                               String ownerId,
                               String sourceFile,
                               String batchId,
                               LocalDateTime importedAt,
                               Set<String> meetingIdsInFile,
                               ImportResult result) {
        String sourceMeetingId = blankToNull(jsonText(record, "ID_MEETING"));
        String meetingId = sourceMeetingId;
        boolean missingMeetingId = meetingId == null;
        List<String> warnings = new ArrayList<>();

        if (missingMeetingId) {
            meetingId = "generated-" + UUID.randomUUID();
            warnings.add("MISSING_ID_MEETING");
        }

        String transcriptionText = blankToNull(jsonText(record, "ANON_TRANSCRICAO"));
        if (transcriptionText == null) {
            result.failed++;
            result.items.add(item(index, meetingId, null, "FAILED", "MISSING_ANON_TRANSCRICAO", warnings));
            log.warn("[TranscriptionImport] failed index={} reason=MISSING_ANON_TRANSCRICAO", index);
            return;
        }

        if (!missingMeetingId
                && (!meetingIdsInFile.add(meetingId) || transcricaoRepository.existsByMeetingId(meetingId))) {
            result.skipped++;
            result.items.add(item(index, meetingId, null, "SKIPPED_DUPLICATE", "ID_MEETING_ALREADY_EXISTS", warnings));
            log.info("[TranscriptionImport] skippedDuplicate meetingId={}", meetingId);
            return;
        }

        String meetingDate = blankToNull(jsonText(record, "DT_MEETING"));
        LocalDateTime parsedMeetingDate = parseSourceDate(meetingDate);
        if (meetingDate != null && parsedMeetingDate == null) {
            warnings.add("INVALID_DT_MEETING");
        }

        String createdAtSource = blankToNull(jsonText(record, "DT_CRIACAO"));
        if (createdAtSource != null && parseSourceDate(createdAtSource) == null) {
            warnings.add("INVALID_DT_CRIACAO");
        }

        String duration = blankToNull(jsonText(record, "DURACAO_MEETING"));
        Transcricao transcricao = Transcricao.builder()
                .ownerId(ownerId)
                .textoOriginal(transcriptionText)
                .transcriptionText(transcriptionText)
                .participantes(extractLocutores(transcriptionText))
                .dataReuniao(parsedMeetingDate)
                .duracaoMinutos(parseDurationMinutes(duration))
                .cliente(buildCliente(record))
                .vendedor("TOTVS JSON")
                .origemTranscricao("json")
                .meetingId(meetingId)
                .meetingDate(meetingDate)
                .format(blankToNull(jsonText(record, "FORMATO_MEETING")))
                .meetingStatus(blankToNull(jsonText(record, "STATUS_MEETING")))
                .duration(duration)
                .codt(blankToNull(jsonText(record, "CODT")))
                .external(jsonBoolean(record, "FLG_EXTERNO"))
                .createdAtSource(createdAtSource)
                .sourceFileName(sourceFile)
                .importBatchId(batchId)
                .importedAt(importedAt)
                .missingMeetingId(missingMeetingId)
                .status(StatusTranscricao.PROCESSANDO)
                .dataCriacao(importedAt)
                .build();

        try {
            Transcricao saved = transcricaoRepository.save(transcricao);
            analysisJob.processar(saved.getId());
            result.imported++;
            result.items.add(item(index, meetingId, saved.getId(), "IMPORTED", null, warnings));
            log.info("[TranscriptionImport] imported meetingId={}", meetingId);
        } catch (DataIntegrityViolationException e) {
            result.skipped++;
            result.items.add(item(index, meetingId, null, "SKIPPED_DUPLICATE", "ID_MEETING_ALREADY_EXISTS", warnings));
            log.info("[TranscriptionImport] skippedDuplicate meetingId={}", meetingId);
        } catch (RuntimeException e) {
            result.failed++;
            result.items.add(item(index, meetingId, null, "FAILED", "SAVE_ERROR", warnings));
            log.warn("[TranscriptionImport] failed index={} reason=SAVE_ERROR error={}", index, e.getClass().getSimpleName());
        }
    }

    private TranscricaoImportItemResponse item(int index,
                                               String meetingId,
                                               String transcriptionId,
                                               String status,
                                               String reason,
                                               List<String> warnings) {
        return TranscricaoImportItemResponse.builder()
                .index(index)
                .meetingId(meetingId)
                .transcriptionId(transcriptionId)
                .status(status)
                .reason(reason)
                .warnings(List.copyOf(warnings))
                .build();
    }

    private String jsonText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private Boolean jsonBoolean(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText().trim().toLowerCase(Locale.ROOT);
        if (text.equals("true") || text.equals("1") || text.equals("s") || text.equals("sim")) {
            return true;
        }
        if (text.equals("false") || text.equals("0") || text.equals("n") || text.equals("nao")) {
            return false;
        }
        return null;
    }

    private String buildCliente(JsonNode record) {
        String codt = blankToNull(jsonText(record, "CODT"));
        String segmento = blankToNull(jsonText(record, "NOME_SEGMENTO"));
        String unidade = blankToNull(jsonText(record, "NOME_UNIDADE"));
        String base = codt == null ? "Cliente TOTVS" : "CODT " + codt;
        if (segmento != null) {
            base += " - " + segmento;
        } else if (unidade != null) {
            base += " - " + unidade;
        }
        return base.length() > 160 ? base.substring(0, 160) : base;
    }

    private String extractLocutores(String transcript) {
        Set<String> speakers = new LinkedHashSet<>();
        java.util.regex.Matcher matcher = LOCUTOR_PATTERN.matcher(transcript);
        while (matcher.find() && speakers.size() < 20) {
            speakers.add(matcher.group().replace("[", "").replace("]", ""));
        }
        return speakers.isEmpty() ? null : String.join(", ", speakers);
    }

    private Integer parseDurationMinutes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.trim().split(":");
        if (parts.length != 3) {
            return null;
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return Math.max(1, hours * 60 + minutes + (seconds > 0 ? 1 : 0));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseSourceDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : SOURCE_DATE_FORMATS) {
            try {
                return LocalDateTime.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato conhecido sem interromper o lote.
            }
        }
        return null;
    }

    private String safeFileName(String value) {
        String fileName = blankToNull(value);
        if (fileName == null) {
            return "arquivo.json";
        }
        return fileName.length() > 120 ? fileName.substring(0, 120) : fileName;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static class ImportResult {
        private final String sourceFile;
        private final String batchId;
        private final List<TranscricaoImportItemResponse> items = new ArrayList<>();
        private int found;
        private int imported;
        private int skipped;
        private int failed;

        private ImportResult(String sourceFile, String batchId) {
            this.sourceFile = sourceFile;
            this.batchId = batchId;
        }

        private TranscricaoImportResponse toResponse() {
            List<String> warnings = new ArrayList<>();
            if (skipped > 0) {
                warnings.add(skipped + " transcricao(oes) ignorada(s) porque o ID_MEETING ja existia.");
            }
            if (failed > 0) {
                warnings.add(failed + " transcricao(oes) apresentaram erro e nao impediram as demais importacoes.");
            }
            warnings.add("Cada reuniao foi salva separadamente e vinculada ao lote " + batchId + ".");

            return TranscricaoImportResponse.builder()
                    .success(imported > 0 || skipped > 0)
                    .sourceFile(sourceFile)
                    .batchId(batchId)
                    .found(found)
                    .imported(imported)
                    .skipped(skipped)
                    .failed(failed)
                    .items(List.copyOf(items))
                    .totalRegistros(found)
                    .importados(imported)
                    .ignorados(skipped + failed)
                    .avisos(warnings)
                    .build();
        }
    }
}
