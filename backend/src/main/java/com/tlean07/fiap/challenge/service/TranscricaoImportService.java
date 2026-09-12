package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.dto.TranscricaoImportItemResponse;
import com.tlean07.fiap.challenge.dto.TranscricaoImportResponse;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * Importa reunioes do export TOTVS em JSON (array, NDJSON, objeto aninhado) ou CSV.
 * Cada reuniao vira uma {@link Transcricao} com os metadados do cliente e entra na fila de analise.
 */
@Service
public class TranscricaoImportService {

    private static final Logger log = LoggerFactory.getLogger(TranscricaoImportService.class);
    private static final Pattern LOCUTOR_PATTERN = Pattern.compile("\\[LOCUTOR\\s+\\d+]");
    private static final List<DateTimeFormatter> SOURCE_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );
    private static final DateTimeFormatter[] SOURCE_DATE_ONLY = {
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    private final TotvsTranscriptionJsonParser parser;
    private final TotvsTranscriptionCsvParser csvParser;
    private final TranscricaoRepository transcricaoRepository;
    private final TranscricaoAnalysisJob analysisJob;
    private final AuditService auditService;

    @Autowired
    public TranscricaoImportService(TotvsTranscriptionJsonParser parser,
                                    TotvsTranscriptionCsvParser csvParser,
                                    TranscricaoRepository transcricaoRepository,
                                    TranscricaoAnalysisJob analysisJob,
                                    AuditService auditService) {
        this.parser = parser;
        this.csvParser = csvParser;
        this.transcricaoRepository = transcricaoRepository;
        this.analysisJob = analysisJob;
        this.auditService = auditService;
    }

    /** Construtor de compatibilidade (testes antigos): CSV usa o mesmo ObjectMapper do JSON. */
    public TranscricaoImportService(TotvsTranscriptionJsonParser parser,
                                    TranscricaoRepository transcricaoRepository,
                                    TranscricaoAnalysisJob analysisJob,
                                    AuditService auditService) {
        this(parser, new TotvsTranscriptionCsvParser(new ObjectMapper()), transcricaoRepository, analysisJob, auditService);
    }

    public TranscricaoImportResponse importar(MultipartFile file, String ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo JSON ou CSV obrigatorio");
        }

        String sourceFile = safeFileName(file.getOriginalFilename());
        String batchId = UUID.randomUUID().toString();
        LocalDateTime importedAt = LocalDateTime.now();
        ImportResult result = new ImportResult(sourceFile, batchId);
        Set<String> meetingIdsInFile = new HashSet<>();

        try (InputStream raw = file.getInputStream();
             BufferedInputStream input = new BufferedInputStream(raw, 64 * 1024)) {
            SourceFormat format = detectFormat(input, file);
            result.format = format;
            TotvsTranscriptionJsonParser.RecordConsumer consumer = (index, record) -> processRecord(
                    index, record, ownerId, sourceFile, batchId, importedAt, meetingIdsInFile, result);
            TotvsTranscriptionJsonParser.ParseSummary summary = format == SourceFormat.CSV
                    ? csvParser.parse(input, consumer)
                    : parser.parse(input, consumer);
            result.found = summary.found();

            if (!summary.transcriptionFieldFound()) {
                throw new IllegalArgumentException("Nenhum campo ANON_TRANSCRICAO foi encontrado no arquivo " + format);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Nao foi possivel ler o arquivo. Verifique se e um JSON ou CSV valido.");
        }

        log.info("[TranscriptionImport] file={} format={} found={}", sourceFile, result.format, result.found);
        auditService.record(
                "transcricao.import.totvs",
                ownerId,
                sourceFile,
                "batchId=" + batchId
                        + ";format=" + result.format
                        + ";found=" + result.found
                        + ";imported=" + result.imported
                        + ";skipped=" + result.skipped
                        + ";failed=" + result.failed
        );

        return result.toResponse();
    }

    /**
     * Decide pelo conteudo, nao so pela extensao: JSON comeca com '{' ou '[' (ignorando BOM e espacos);
     * qualquer outra coisa com cabecalho textual e tratada como CSV.
     */
    static SourceFormat detectFormat(BufferedInputStream input, MultipartFile file) throws IOException {
        input.mark(4096);
        byte[] head = input.readNBytes(4096);
        input.reset();
        int i = 0;
        if (head.length >= 3 && (head[0] & 0xFF) == 0xEF && (head[1] & 0xFF) == 0xBB && (head[2] & 0xFF) == 0xBF) {
            i = 3;
        }
        while (i < head.length && Character.isWhitespace(head[i])) {
            i++;
        }
        if (i < head.length && (head[i] == '{' || head[i] == '[')) {
            return SourceFormat.JSON;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json") || type.contains("json")) {
            return SourceFormat.JSON;
        }
        return SourceFormat.CSV;
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

        Integer notaNps = parseNps(jsonText(record, "NOTA_NPS"));
        if (blankToNull(jsonText(record, "NOTA_NPS")) != null && notaNps == null) {
            warnings.add("INVALID_NOTA_NPS");
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
                .vendedor("TOTVS " + (result.format == SourceFormat.CSV ? "CSV" : "JSON"))
                .origemTranscricao(result.format == SourceFormat.CSV ? "csv" : "json")
                .meetingId(meetingId)
                .meetingDate(meetingDate)
                .format(blankToNull(jsonText(record, "FORMATO_MEETING")))
                .meetingStatus(blankToNull(jsonText(record, "STATUS_MEETING")))
                .duration(duration)
                .codt(blankToNull(jsonText(record, "CODT")))
                .external(jsonBoolean(record, "FLG_EXTERNO"))
                .createdAtSource(createdAtSource)
                .uf(truncate(blankToNull(jsonText(record, "UF")), 10))
                .cnae(truncate(blankToNull(jsonText(record, "CNAE")), 20))
                .nomeUnidade(truncate(blankToNull(jsonText(record, "NOME_UNIDADE")), 200))
                .nomeSegmento(truncate(blankToNull(jsonText(record, "NOME_SEGMENTO")), 200))
                .faixaFaturamento(truncate(firstNonBlank(jsonText(record, "FAIXA_FATURAMENTO_CLIENTE_EC"),
                        jsonText(record, "FAIXA_FATURAMENTO_CLIENTE")), 200))
                .tipoRecurso(truncate(blankToNull(jsonText(record, "TP_RECURSO")), 50))
                .notaNps(notaNps)
                .dataUltimaPesquisa(truncate(blankToNull(jsonText(record, "DT_ULTIMA_PESQUISA")), 50))
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

    private Integer parseNps(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            int nps = (int) Math.round(Double.parseDouble(text.replace(',', '.')));
            return nps < 0 || nps > 10 ? null : nps;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildCliente(JsonNode record) {
        String codt = blankToNull(jsonText(record, "CODT"));
        String segmento = blankToNull(jsonText(record, "NOME_SEGMENTO"));
        String unidade = blankToNull(jsonText(record, "NOME_UNIDADE"));
        String base = codt == null ? "Cliente TOTVS" : "CODT " + codt;
        if (segmento != null && !"None".equalsIgnoreCase(segmento)) {
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
        String text = value.trim();
        for (DateTimeFormatter formatter : SOURCE_DATE_FORMATS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // Tenta o proximo formato conhecido sem interromper o lote.
            }
        }
        for (DateTimeFormatter formatter : SOURCE_DATE_ONLY) {
            try {
                return java.time.LocalDate.parse(text, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // idem
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String text = blankToNull(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    enum SourceFormat {
        JSON, CSV
    }

    private static class ImportResult {
        private final String sourceFile;
        private final String batchId;
        private final List<TranscricaoImportItemResponse> items = new ArrayList<>();
        private SourceFormat format = SourceFormat.JSON;
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
                    .sourceFormat(format.name())
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
