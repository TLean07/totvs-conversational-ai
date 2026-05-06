package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.*;
import com.tlean07.fiap.challenge.dto.TranscricaoRequest;
import com.tlean07.fiap.challenge.dto.TranscricaoResponse;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.AnaliseFeedback;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AnaliseFeedbackRepository;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TranscricaoService {

    private static final int MAX_IMPORT_LIMIT = 200;
    private static final int MAX_TRANSCRICAO_LENGTH = 20000;
    private static final int MAX_IMPORT_SAMPLE_CHARS = 12000;
    private static final Pattern TOTVS_RECORD_START = Pattern.compile("(?m)^\"\\d+,\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},");
    private static final Pattern LOCUTOR_PATTERN = Pattern.compile("\\[LOCUTOR\\s+\\d+]");

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AnaliseFeedbackRepository feedbackRepository;
    private final AlertaRepository alertaRepository;
    private final AnaliseService analiseService;
    private final TranscricaoAnalysisJob analysisJob;
    private final AuditService auditService;

    public TranscricaoService(TranscricaoRepository transcricaoRepository,
                              AnaliseResultadoRepository analiseRepository,
                              AnaliseFeedbackRepository feedbackRepository,
                              AlertaRepository alertaRepository,
                              AnaliseService analiseService,
                              TranscricaoAnalysisJob analysisJob,
                              AuditService auditService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.feedbackRepository = feedbackRepository;
        this.alertaRepository = alertaRepository;
        this.analiseService = analiseService;
        this.analysisJob = analysisJob;
        this.auditService = auditService;
    }

    public TranscricaoResponse criarEAnalisar(TranscricaoRequest request) {
        return criarEAnalisar(request, SecurityUtils.currentUser().id());
    }

    public TranscricaoResponse criarEAnalisar(TranscricaoRequest request, String ownerId) {
        Transcricao transcricao = Transcricao.builder()
                .ownerId(ownerId)
                .textoOriginal(request.getTextoOriginal())
                .participantes(request.getParticipantes())
                .duracaoMinutos(request.getDuracaoMinutos())
                .cliente(request.getCliente())
                .vendedor(request.getVendedor())
                .status(StatusTranscricao.PENDENTE)
                .dataCriacao(LocalDateTime.now())
                .build();

        if (request.getDataReuniao() != null && !request.getDataReuniao().isBlank()) {
            transcricao.setDataReuniao(LocalDateTime.parse(request.getDataReuniao(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        transcricao = transcricaoRepository.save(transcricao);

        transcricao.setStatus(StatusTranscricao.PROCESSANDO);
        transcricao = transcricaoRepository.save(transcricao);
        analysisJob.processar(transcricao.getId());
        auditService.record("transcricao.create", ownerId, transcricao.getId(), "queued");
        return toResponse(transcricao);
    }

    public TranscricaoImportResponse importarTotvsCsv(MultipartFile file, int limit, int offset) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV obrigatorio");
        }
        String ownerId = SecurityUtils.currentUser().id();
        List<String> avisos = new ArrayList<>();
        ImportCounters counters = processTotvsCsvAsSingleDocument(file, ownerId);

        if (counters.ignorados > 0) {
            avisos.add(counters.ignorados + " registro(s) foram ignorados por formato invalido ou transcricao vazia.");
        }
        avisos.add("Foi criada 1 transcricao consolidada para este CSV, evitando centenas de documentos na colecao transcricoes.");
        avisos.add("Os registros do CSV foram tratados como partes da mesma transcricao TOTVS.");
        avisos.add("A LeanIA analisa estatisticas e trechos representativos da transcricao consolidada.");
        avisos.add("O CSV bruto nao e salvo inteiro no MongoDB porque documentos MongoDB possuem limite de 16 MB.");

        auditService.record("transcricao.import.totvs", ownerId, file.getOriginalFilename(), "records=" + counters.totalRegistros + ";documents=1");
        return TranscricaoImportResponse.builder()
                .totalRegistros(counters.totalRegistros)
                .importados(counters.importados)
                .ignorados(counters.ignorados)
                .avisos(avisos)
                .build();
    }

    private ImportCounters processTotvsCsvAsSingleDocument(MultipartFile file, String ownerId) {
        ImportCounters counters = new ImportCounters();
        TotvsImportSummary summary = new TotvsImportSummary(file.getOriginalFilename());
        StringBuilder currentRecord = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\uFEFF", "");
                if (isTotvsRecordStart(line)) {
                    if (currentRecord != null) {
                        processTotvsRecordIntoSummary(currentRecord.toString(), counters, summary);
                    }
                    counters.totalRegistros++;
                    currentRecord = new StringBuilder(line);
                } else if (currentRecord != null) {
                    currentRecord.append('\n').append(line);
                }
            }
            if (currentRecord != null) {
                processTotvsRecordIntoSummary(currentRecord.toString(), counters, summary);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Nao foi possivel ler o arquivo CSV");
        }

        if (summary.validRecords == 0) {
            throw new IllegalArgumentException("Nenhuma transcricao valida foi encontrada no CSV");
        }

        TranscricaoRequest request = TranscricaoRequest.builder()
                .textoOriginal(summary.buildText())
                .cliente("Transcricao TOTVS - " + safeFileName(file.getOriginalFilename()))
                .vendedor("TOTVS CSV")
                .participantes(summary.buildParticipants())
                .duracaoMinutos(summary.totalDurationMinutes)
                .dataReuniao(toIsoLocalDateTime(summary.firstDate))
                .build();
        criarEAnalisar(request, ownerId);
        counters.importados = 1;

        return counters;
    }

    private boolean isTotvsRecordStart(String line) {
        return TOTVS_RECORD_START.matcher(line).find();
    }

    private void processTotvsRecordIntoSummary(String record, ImportCounters counters, TotvsImportSummary summary) {
        TotvsCsvRow row = parseTotvsRecord(record);
        if (row == null || row.transcricao().isBlank()) {
            counters.ignorados++;
            return;
        }
        summary.add(row);
    }

    private TotvsCsvRow parseTotvsRecord(String rawRecord) {
        String record = rawRecord.trim();
        if (record.startsWith("\"")) {
            record = record.substring(1);
        }
        if (record.endsWith("\"")) {
            record = record.substring(0, record.length() - 1);
        }

        String[] firstFields = record.split(",", 11);
        if (firstFields.length < 11) {
            return null;
        }

        String transcriptAndTail = firstFields[10];
        if (transcriptAndTail.startsWith("\"\"")) {
            transcriptAndTail = transcriptAndTail.substring(2);
        }

        int tailStart = transcriptAndTail.lastIndexOf("\"\",");
        if (tailStart < 0) {
            return null;
        }

        String transcript = transcriptAndTail.substring(0, tailStart)
                .replace("\"\"", "\"")
                .trim();
        String[] tail = transcriptAndTail.substring(tailStart + 3).split(",", -1);
        if (tail.length < 7) {
            return null;
        }

        return new TotvsCsvRow(
                firstFields[0],
                firstFields[1],
                firstFields[2],
                firstFields[3],
                firstFields[4],
                firstFields[5],
                firstFields[6],
                firstFields[7],
                firstFields[8],
                firstFields[9],
                transcript,
                tail[0],
                tail[1],
                tail[2],
                tail[3],
                tail[4],
                tail[5],
                tail[6].replace("\"", "")
        );
    }

    private String buildImportedText(TotvsCsvRow row) {
        String metadata = """
                [IMPORTACAO CSV TOTVS]
                ID_MEETING: %s
                DATA_REUNIAO: %s
                FORMATO: %s
                STATUS: %s
                DURACAO: %s
                CODT: %s
                UF: %s
                CNAE: %s
                UNIDADE: %s
                SEGMENTO: %s
                FAIXA_FATURAMENTO: %s
                NOTA_NPS: %s

                TRANSCRICAO:
                """.formatted(
                row.idMeeting(),
                row.dtMeeting(),
                row.formatoMeeting(),
                row.statusMeeting(),
                row.duracaoMeeting(),
                row.codt(),
                row.uf(),
                row.cnae(),
                row.nomeUnidade(),
                row.nomeSegmento(),
                row.faixaFaturamento(),
                row.notaNps()
        );
        int remaining = Math.max(0, MAX_TRANSCRICAO_LENGTH - metadata.length() - 120);
        String transcript = row.transcricao();
        if (transcript.length() > remaining) {
            transcript = transcript.substring(0, remaining)
                    + "\n\n[TRANSCRICAO TRUNCADA PARA RESPEITAR O LIMITE DE ANALISE]";
        }
        return metadata + transcript;
    }

    private String buildCliente(TotvsCsvRow row) {
        String codt = blankToNull(row.codt());
        String segmento = blankToNull(row.nomeSegmento());
        String unidade = blankToNull(row.nomeUnidade());
        String base = codt == null ? "Cliente TOTVS" : "CODT " + codt;
        if (segmento != null) {
            base += " - " + segmento;
        } else if (unidade != null) {
            base += " - " + unidade;
        }
        return base.length() > 160 ? base.substring(0, 160) : base;
    }

    private String safeFileName(String value) {
        String fileName = blankToNull(value);
        if (fileName == null) {
            return "arquivo CSV";
        }
        return fileName.length() > 120 ? fileName.substring(0, 120) : fileName;
    }

    private String extractLocutores(String transcript) {
        Set<String> speakers = new LinkedHashSet<>();
        Matcher matcher = LOCUTOR_PATTERN.matcher(transcript);
        while (matcher.find() && speakers.size() < 20) {
            speakers.add(matcher.group().replace("[", "").replace("]", ""));
        }
        return String.join(", ", speakers);
    }

    private Integer parseDurationMinutes(String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.split(":");
        if (parts.length != 3) return null;
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return Math.max(1, hours * 60 + minutes + (seconds > 0 ? 1 : 0));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toIsoLocalDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replace(" ", "T");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record TotvsCsvRow(
            String idMeeting,
            String dtMeeting,
            String formatoMeeting,
            String idStatusMeeting,
            String statusMeeting,
            String duracaoMeeting,
            String codt,
            String tpRecurso,
            String flgExterno,
            String dtCriacao,
            String transcricao,
            String uf,
            String cnae,
            String nomeUnidade,
            String nomeSegmento,
            String faixaFaturamento,
            String dtUltimaPesquisa,
            String notaNps
    ) {
    }

    private static class ImportCounters {
        private int totalRegistros;
        private int importados;
        private int ignorados;
    }

    private class TotvsImportSummary {
        private final String fileName;
        private final Map<String, Integer> segmentos = new LinkedHashMap<>();
        private final Map<String, Integer> unidades = new LinkedHashMap<>();
        private final Map<String, Integer> ufs = new LinkedHashMap<>();
        private final Map<String, Integer> status = new LinkedHashMap<>();
        private final Map<String, Integer> faixasFaturamento = new LinkedHashMap<>();
        private final Set<String> locutores = new LinkedHashSet<>();
        private final StringBuilder amostras = new StringBuilder();
        private int validRecords;
        private int totalDurationMinutes;
        private int npsCount;
        private int npsSum;
        private String firstDate;
        private String lastDate;

        private TotvsImportSummary(String fileName) {
            this.fileName = safeFileName(fileName);
        }

        private void add(TotvsCsvRow row) {
            validRecords++;
            addCount(segmentos, row.nomeSegmento());
            addCount(unidades, row.nomeUnidade());
            addCount(ufs, row.uf());
            addCount(status, row.statusMeeting());
            addCount(faixasFaturamento, row.faixaFaturamento());
            addLocutores(row.transcricao());

            Integer minutes = parseDurationMinutes(row.duracaoMeeting());
            if (minutes != null) {
                totalDurationMinutes += minutes;
            }
            addNps(row.notaNps());
            updateDates(row.dtMeeting());
            addSample(row);
        }

        private String buildText() {
            String metadata = """
                    [TRANSCRICAO CSV TOTVS - CONSOLIDADA]
                    ARQUIVO: %s
                    TOTAL_PARTES_VALIDAS: %d
                    PERIODO: %s ate %s
                    DURACAO_TOTAL_ESTIMADA_MIN: %d
                    NPS_MEDIO: %s
                    OBSERVACAO: Os registros do CSV foram interpretados como partes de uma unica transcricao.

                    DISTRIBUICAO_POR_SEGMENTO:
                    %s

                    DISTRIBUICAO_POR_UNIDADE:
                    %s

                    DISTRIBUICAO_POR_UF:
                    %s

                    DISTRIBUICAO_POR_STATUS:
                    %s

                    DISTRIBUICAO_POR_FAIXA_FATURAMENTO:
                    %s

                    TRECHOS_REPRESENTATIVOS_DA_TRANSCRICAO:
                    """.formatted(
                    fileName,
                    validRecords,
                    blankOrDash(firstDate),
                    blankOrDash(lastDate),
                    totalDurationMinutes,
                    npsCount == 0 ? "-" : String.format("%.1f", (double) npsSum / npsCount),
                    formatTop(segementosOrEmpty(segmentos)),
                    formatTop(segementosOrEmpty(unidades)),
                    formatTop(segementosOrEmpty(ufs)),
                    formatTop(segementosOrEmpty(status)),
                    formatTop(segementosOrEmpty(faixasFaturamento))
            );
            String text = metadata + amostras;
            if (text.length() > MAX_TRANSCRICAO_LENGTH) {
                return text.substring(0, MAX_TRANSCRICAO_LENGTH - 80)
                        + "\n\n[RESUMO TRUNCADO PARA RESPEITAR O LIMITE DE ANALISE]";
            }
            return text;
        }

        private String buildParticipants() {
            if (locutores.isEmpty()) {
                return "Transcricao CSV TOTVS";
            }
            return String.join(", ", locutores.stream().limit(20).toList());
        }

        private void addCount(Map<String, Integer> map, String value) {
            String key = blankToNull(value);
            if (key == null) {
                key = "NAO_INFORMADO";
            }
            map.merge(key, 1, Integer::sum);
        }

        private void addLocutores(String transcript) {
            Matcher matcher = LOCUTOR_PATTERN.matcher(transcript);
            while (matcher.find() && locutores.size() < 20) {
                locutores.add(matcher.group().replace("[", "").replace("]", ""));
            }
        }

        private void addNps(String value) {
            try {
                int nps = Integer.parseInt(value.replace("\"", "").trim());
                npsSum += nps;
                npsCount++;
            } catch (RuntimeException ignored) {
                // NPS ausente ou nao numerico nao deve bloquear a importacao.
            }
        }

        private void updateDates(String value) {
            String date = blankToNull(value);
            if (date == null) {
                return;
            }
            if (firstDate == null || date.compareTo(firstDate) < 0) {
                firstDate = date;
            }
            if (lastDate == null || date.compareTo(lastDate) > 0) {
                lastDate = date;
            }
        }

        private void addSample(TotvsCsvRow row) {
            if (amostras.length() >= MAX_IMPORT_SAMPLE_CHARS) {
                return;
            }
            String transcript = row.transcricao().replaceAll("\\s+", " ").trim();
            int maxSnippet = Math.min(900, transcript.length());
            String snippet = transcript.substring(0, maxSnippet);
            if (transcript.length() > maxSnippet) {
                snippet += "...";
            }
            String sample = """

                    ---
                    ID_MEETING: %s
                    DATA: %s
                    CODT: %s
                    SEGMENTO: %s
                    UNIDADE: %s
                    UF: %s
                    NPS: %s
                    TRECHO:
                    %s
                    """.formatted(
                    row.idMeeting(),
                    row.dtMeeting(),
                    row.codt(),
                    row.nomeSegmento(),
                    row.nomeUnidade(),
                    row.uf(),
                    row.notaNps(),
                    snippet
            );
            if (amostras.length() + sample.length() <= MAX_IMPORT_SAMPLE_CHARS) {
                amostras.append(sample);
            }
        }

        private Map<String, Integer> segementosOrEmpty(Map<String, Integer> values) {
            return values;
        }

        private String formatTop(Map<String, Integer> values) {
            if (values.isEmpty()) {
                return "-";
            }
            return values.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(10)
                    .map(entry -> "- " + entry.getKey() + ": " + entry.getValue())
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("-");
        }

        private String blankOrDash(String value) {
            return value == null || value.isBlank() ? "-" : value;
        }
    }

    public List<TranscricaoResponse> listarTodas() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<Transcricao> transcricoes = user.isAdmin()
                ? transcricaoRepository.findAllByOrderByDataCriacaoDesc()
                : transcricaoRepository.findByOwnerIdOrderByDataCriacaoDesc(user.id());

        return transcricoes
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<TranscricaoResponse> listarPaginado(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "dataCriacao"));
        AuthenticatedUser user = SecurityUtils.currentUser();
        Page<Transcricao> result = user.isAdmin()
                ? transcricaoRepository.findAllByOrderByDataCriacaoDesc(pageRequest)
                : transcricaoRepository.findByOwnerIdOrderByDataCriacaoDesc(user.id(), pageRequest);

        return PageResponse.<TranscricaoResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public TranscricaoResponse buscarPorId(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        return toResponse(transcricao);
    }

    public List<TranscricaoResponse> buscarPorCliente(String cliente) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<Transcricao> transcricoes = user.isAdmin()
                ? transcricaoRepository.findByClienteContainingIgnoreCase(cliente)
                : transcricaoRepository.findByOwnerIdAndClienteContainingIgnoreCase(user.id(), cliente);

        return transcricoes
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deletar(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        limparAnaliseEAlertas(transcricao);
        feedbackRepository.deleteByTranscricaoId(transcricao.getId());
        transcricaoRepository.delete(transcricao);
        auditService.record("transcricao.delete", SecurityUtils.currentUser().id(), id, "success");
    }

    public TranscricaoResponse reprocessar(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        transcricao.setStatus(StatusTranscricao.PROCESSANDO);
        transcricao.setDataProcessamento(null);
        transcricao = transcricaoRepository.save(transcricao);
        analysisJob.processar(transcricao.getId());
        auditService.record("transcricao.reprocess", SecurityUtils.currentUser().id(), id, "queued");
        return toResponse(transcricao);
    }

    public AnaliseFeedbackResponse registrarFeedback(String id, AnaliseFeedbackRequest request) {
        Transcricao transcricao = findVisibleTranscricao(id);
        AnaliseResultado analise = transcricao.getAnaliseResultado();
        if (analise == null) {
            analise = analiseRepository.findByTranscricaoId(transcricao.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Transcricao ainda nao possui analise"));
        }

        AnaliseFeedback feedback = AnaliseFeedback.builder()
                .ownerId(transcricao.getOwnerId())
                .transcricaoId(transcricao.getId())
                .analiseId(analise.getId())
                .util(request.isUtil())
                .ajuste(request.getAjuste() == null ? "" : request.getAjuste().trim())
                .createdAt(LocalDateTime.now())
                .build();

        AnaliseFeedback saved = feedbackRepository.save(feedback);
        auditService.record("analise.feedback", SecurityUtils.currentUser().id(), id, "success");
        return AnaliseFeedbackResponse.builder()
                .id(saved.getId())
                .transcricaoId(saved.getTranscricaoId())
                .analiseId(saved.getAnaliseId())
                .util(saved.isUtil())
                .ajuste(saved.getAjuste())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<AnaliseFeedbackResponse> listarFeedbacks(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<AnaliseFeedback> feedbacks = user.isAdmin()
                ? feedbackRepository.findByTranscricaoIdOrderByCreatedAtDesc(transcricao.getId())
                : feedbackRepository.findByTranscricaoIdAndOwnerIdOrderByCreatedAtDesc(transcricao.getId(), user.id());

        return feedbacks.stream()
                .map(this::toFeedbackResponse)
                .toList();
    }

    private Transcricao findVisibleTranscricao(String id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return (user.isAdmin()
                ? transcricaoRepository.findById(id)
                : transcricaoRepository.findByIdAndOwnerId(id, user.id()))
                .orElseThrow(() -> new AccessDeniedException("Transcricao nao encontrada"));
    }

    private AnaliseFeedbackResponse toFeedbackResponse(AnaliseFeedback feedback) {
        return AnaliseFeedbackResponse.builder()
                .id(feedback.getId())
                .transcricaoId(feedback.getTranscricaoId())
                .analiseId(feedback.getAnaliseId())
                .util(feedback.isUtil())
                .ajuste(feedback.getAjuste())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    private void limparAnaliseEAlertas(Transcricao transcricao) {
        if (transcricao.getAnaliseResultado() != null && transcricao.getAnaliseResultado().getId() != null) {
            alertaRepository.deleteByAnaliseId(transcricao.getAnaliseResultado().getId());
            analiseRepository.deleteById(transcricao.getAnaliseResultado().getId());
        }
        analiseRepository.findByTranscricaoId(transcricao.getId()).ifPresent(analise -> {
            alertaRepository.deleteByAnaliseId(analise.getId());
            analiseRepository.delete(analise);
        });
    }

    public TranscricaoResponse toResponse(Transcricao t) {
        TranscricaoResponse.TranscricaoResponseBuilder builder = TranscricaoResponse.builder()
                .id(t.getId())
                .textoOriginal(t.getTextoOriginal())
                .participantes(t.getParticipantes())
                .dataReuniao(t.getDataReuniao())
                .duracaoMinutos(t.getDuracaoMinutos())
                .cliente(t.getCliente())
                .vendedor(t.getVendedor())
                .status(t.getStatus())
                .dataCriacao(t.getDataCriacao())
                .dataProcessamento(t.getDataProcessamento());

        // Buscar analise se existir
        if (t.getAnaliseResultado() != null) {
            builder.analise(analiseService.toResponse(t.getAnaliseResultado()));
        } else {
            analiseRepository.findByTranscricaoId(t.getId())
                    .ifPresent(a -> builder.analise(analiseService.toResponse(a)));
        }

        return builder.build();
    }
}
