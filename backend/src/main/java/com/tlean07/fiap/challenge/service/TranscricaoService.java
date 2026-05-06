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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TranscricaoService {

    private static final int MAX_IMPORT_LIMIT = 200;
    private static final int MAX_TRANSCRICAO_LENGTH = 20000;
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
        int safeLimit = Math.max(1, Math.min(MAX_IMPORT_LIMIT, limit));
        int safeOffset = Math.max(0, offset);
        String ownerId = SecurityUtils.currentUser().id();
        List<String> avisos = new ArrayList<>();
        List<String> records = splitTotvsRecords(readCsv(file));

        int importados = 0;
        int ignorados = 0;
        for (int i = safeOffset; i < records.size(); i++) {
            if (importados >= safeLimit) {
                break;
            }
            String record = records.get(i);
            TotvsCsvRow row = parseTotvsRecord(record);
            if (row == null || row.transcricao().isBlank()) {
                ignorados++;
                continue;
            }
            TranscricaoRequest request = TranscricaoRequest.builder()
                    .textoOriginal(buildImportedText(row))
                    .cliente(buildCliente(row))
                    .vendedor(blankToNull(row.nomeUnidade()))
                    .participantes(extractLocutores(row.transcricao()))
                    .duracaoMinutos(parseDurationMinutes(row.duracaoMeeting()))
                    .dataReuniao(toIsoLocalDateTime(row.dtMeeting()))
                    .build();
            criarEAnalisar(request, ownerId);
            importados++;
        }

        if (safeOffset >= records.size()) {
            avisos.add("O offset informado e maior que a quantidade de registros do CSV.");
        }
        if (records.size() > safeOffset + safeLimit) {
            avisos.add("Importacao limitada a " + safeLimit + " registros nesta execucao. Proximo offset sugerido: " + (safeOffset + importados + ignorados) + ".");
        }
        if (ignorados > 0) {
            avisos.add(ignorados + " registro(s) foram ignorados por formato invalido ou transcricao vazia.");
        }
        avisos.add("Transcricoes importadas entram como PROCESSANDO e aparecem no dashboard conforme a LeanIA concluir as analises.");

        auditService.record("transcricao.import.totvs", ownerId, file.getOriginalFilename(), "imported=" + importados);
        return TranscricaoImportResponse.builder()
                .totalRegistros(records.size())
                .importados(importados)
                .ignorados(ignorados)
                .avisos(avisos)
                .build();
    }

    private String readCsv(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8)
                    .replace("\uFEFF", "");
        } catch (IOException e) {
            throw new IllegalArgumentException("Nao foi possivel ler o arquivo CSV");
        }
    }

    private List<String> splitTotvsRecords(String csvText) {
        Matcher matcher = TOTVS_RECORD_START.matcher(csvText);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        List<String> records = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : csvText.length();
            records.add(csvText.substring(start, end).trim());
        }
        return records;
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
