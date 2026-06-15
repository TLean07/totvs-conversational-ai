package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.*;
import com.tlean07.fiap.challenge.dto.TranscricaoRequest;
import com.tlean07.fiap.challenge.dto.TranscricaoResponse;
import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.*;
import com.tlean07.fiap.challenge.repository.AnaliseFeedbackRepository;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.dto.TranscriptionResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private static final int MAX_EVIDENCES_PER_CATEGORY = 5;
    private static final Pattern LOCUTOR_PATTERN = Pattern.compile("\\[LOCUTOR\\s+\\d+]");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AnaliseFeedbackRepository feedbackRepository;
    private final AlertaRepository alertaRepository;
    private final AnaliseService analiseService;
    private final TranscricaoAnalysisJob analysisJob;
    private final AudioTranscriptionService audioTranscriptionService;
    private final AuditService auditService;
    private final TranscricaoImportService transcricaoImportService;

    public TranscricaoService(TranscricaoRepository transcricaoRepository,
                              AnaliseResultadoRepository analiseRepository,
                              AnaliseFeedbackRepository feedbackRepository,
                              AlertaRepository alertaRepository,
                              AnaliseService analiseService,
                              TranscricaoAnalysisJob analysisJob,
                              AudioTranscriptionService audioTranscriptionService,
                              AuditService auditService,
                              TranscricaoImportService transcricaoImportService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.feedbackRepository = feedbackRepository;
        this.alertaRepository = alertaRepository;
        this.analiseService = analiseService;
        this.analysisJob = analysisJob;
        this.audioTranscriptionService = audioTranscriptionService;
        this.auditService = auditService;
        this.transcricaoImportService = transcricaoImportService;
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
                .origemTranscricao("manual")
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

    public TranscricaoImportResponse importarTotvsJson(MultipartFile file, int limit, int offset) {
        return transcricaoImportService.importar(file, SecurityUtils.currentUser().id());
    }

    public TranscricaoResponse importarAudio(MultipartFile file, String cliente, String vendedor, String participantes) {
        String ownerId = SecurityUtils.currentUser().id();
        TranscriptionResultDTO transcription = audioTranscriptionService.transcrever(file, null);
        Integer durationMinutes = transcription.getDurationSeconds() == null
                ? null
                : Math.max(1, (int) Math.ceil(transcription.getDurationSeconds() / 60.0));

        Transcricao transcricao = Transcricao.builder()
                .ownerId(ownerId)
                .textoOriginal(transcription.getTranscript())
                .cliente(blankToNull(cliente) == null ? "Transcricao de audio - " + safeFileName(file.getOriginalFilename()) : cliente)
                .vendedor(blankToNull(vendedor) == null ? "Parakeet" : vendedor)
                .participantes(participantes)
                .duracaoMinutos(durationMinutes)
                .origemTranscricao("parakeet")
                .audioFileName(safeFileName(file.getOriginalFilename()))
                .transcriptionModel(transcription.getModel())
                .transcriptionLanguage(transcription.getLanguage())
                .audioDurationSeconds(transcription.getDurationSeconds())
                .transcriptionProcessingTimeMs(transcription.getProcessingTimeMs())
                .transcriptionWarnings(transcription.getWarnings())
                .status(StatusTranscricao.PENDENTE)
                .dataCriacao(LocalDateTime.now())
                .build();

        TranscricaoResponse response = salvarEEnfileirar(transcricao, ownerId, "transcricao.audio.import");
        auditService.record("transcricao.audio.transcribe", ownerId, response.getId(), "model=" + transcription.getModel());
        return response;
    }

    private TranscricaoResponse salvarEEnfileirar(Transcricao transcricao, String ownerId, String auditEvent) {
        Transcricao saved = transcricaoRepository.save(transcricao);
        saved.setStatus(StatusTranscricao.PROCESSANDO);
        saved = transcricaoRepository.save(saved);
        analysisJob.processar(saved.getId());
        auditService.record(auditEvent, ownerId, saved.getId(), "queued");
        return toResponse(saved);
    }

    private void processTotvsJsonRecordIntoSummary(JsonNode record, ImportCounters counters, TotvsImportSummary summary) {
        TotvsJsonRow row = parseTotvsJsonRecord(record);
        if (row == null || row.transcricao().isBlank()) {
            counters.ignorados++;
            return;
        }
        summary.add(row);
    }

    private TotvsJsonRow parseTotvsJsonRecord(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        return new TotvsJsonRow(
                jsonText(node, "ID_MEETING"),
                jsonText(node, "DT_MEETING"),
                jsonText(node, "FORMATO_MEETING"),
                jsonText(node, "ID_STATUS_MEETING"),
                jsonText(node, "STATUS_MEETING"),
                jsonText(node, "DURACAO_MEETING"),
                jsonText(node, "CODT"),
                "",
                jsonText(node, "FLG_EXTERNO"),
                jsonText(node, "DT_CRIACAO"),
                jsonText(node, "ANON_TRANSCRICAO"),
                jsonText(node, "UF"),
                jsonText(node, "CNAE"),
                jsonText(node, "NOME_UNIDADE"),
                jsonText(node, "NOME_SEGMENTO"),
                jsonText(node, "FAIXA_FATURAMENTO_CLIENTE_EC"),
                jsonText(node, "DT_ULTIMA_PESQUISA"),
                jsonText(node, "NOTA_NPS")
        );
    }

    private String jsonText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.isTextual() ? value.asText() : value.asText("");
    }

    private String buildCliente(TotvsJsonRow row) {
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
            return "arquivo JSON";
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

    private record TotvsJsonRow(
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
        private final Map<String, Integer> riskCategories = new LinkedHashMap<>();
        private final Map<String, Integer> opportunityCategories = new LinkedHashMap<>();
        private final Map<String, List<String>> riskEvidences = new LinkedHashMap<>();
        private final Map<String, List<String>> opportunityEvidences = new LinkedHashMap<>();
        private final Set<String> locutores = new LinkedHashSet<>();
        private final List<ItemCritico> criticalItems = new ArrayList<>();
        private final StringBuilder amostras = new StringBuilder();
        private int validRecords;
        private int totalDurationMinutes;
        private int npsCount;
        private int npsSum;
        private int negativeParts;
        private int positiveParts;
        private int neutralParts;
        private int churnSignals;
        private int criticalMentions;
        private int opportunitySignals;
        private String firstDate;
        private String lastDate;

        private TotvsImportSummary(String fileName) {
            this.fileName = safeFileName(fileName);
        }

        private void add(TotvsJsonRow row) {
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
            analyzeSignals(row);
            addSample(row);
        }

        private String buildText() {
            String metadata = """
                    [TRANSCRICAO JSON TOTVS - CONSOLIDADA]
                    ARQUIVO: %s
                    TOTAL_PARTES_VALIDAS: %d
                    PERIODO: %s ate %s
                    DURACAO_TOTAL_ESTIMADA_MIN: %d
                    NPS_MEDIO: %s
                    SENTIMENTO_PARTES: positivas=%d, neutras=%d, negativas=%d
                    RISCOS_DETECTADOS: %d
                    OPORTUNIDADES_DETECTADAS: %d
                    CHURNS_POTENCIAIS: %d
                    MENCOES_CRITICAS: %d
                    OBSERVACAO: Os registros do JSON foram interpretados como partes de uma unica transcricao.

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
                    positiveParts,
                    neutralParts,
                    negativeParts,
                    totalRisks(),
                    opportunitySignals,
                    churnSignals,
                    criticalMentions,
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
                return "Transcricao JSON TOTVS";
            }
            return String.join(", ", locutores.stream().limit(20).toList());
        }

        private MetricasAgregadas buildMetrics() {
            int alertCount = buildConsolidatedAlerts().size();
            return MetricasAgregadas.builder()
                    .totalPartes(validRecords)
                    .partesAnalisadas(validRecords)
                    .partesNegativas(negativeParts)
                    .partesNeutras(neutralParts)
                    .partesPositivas(positiveParts)
                    .riscosDetectados(totalRisks())
                    .oportunidadesDetectadas(opportunitySignals)
                    .churnsPotenciais(churnSignals)
                    .mencoesCriticas(criticalMentions)
                    .alertasConsolidados(alertCount)
                    .chunksProcessados(Math.max(1, (int) Math.ceil(validRecords / 50.0)))
                    .build();
        }

        private String buildExecutiveSummary() {
            return "Transcricao JSON TOTVS consolidada com %d parte(s), %d parte(s) negativas, %d risco(s), %d oportunidade(s), %d indicio(s) de churn e %d mencao(oes) critica(s)."
                    .formatted(validRecords, negativeParts, totalRisks(), opportunitySignals, churnSignals, criticalMentions);
        }

        private List<RiscoConsolidado> buildConsolidatedRisks() {
            return riskCategories.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .map(entry -> RiscoConsolidado.builder()
                            .categoria(entry.getKey())
                            .ocorrencias(entry.getValue())
                            .severidade(riskSeverity(entry.getKey(), entry.getValue()))
                            .evidencias(riskEvidences.getOrDefault(entry.getKey(), List.of()))
                            .recomendacao(riskRecommendation(entry.getKey()))
                            .build())
                    .toList();
        }

        private List<OportunidadeConsolidada> buildConsolidatedOpportunities() {
            return opportunityCategories.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .map(entry -> OportunidadeConsolidada.builder()
                            .categoria(entry.getKey())
                            .ocorrencias(entry.getValue())
                            .score(Math.min(0.95, 0.45 + (entry.getValue() * 0.08)))
                            .evidencias(opportunityEvidences.getOrDefault(entry.getKey(), List.of()))
                            .recomendacao(opportunityRecommendation(entry.getKey()))
                            .build())
                    .toList();
        }

        private List<AlertaConsolidado> buildConsolidatedAlerts() {
            List<AlertaConsolidado> alerts = new ArrayList<>();
            int negativeRate = validRecords == 0 ? 0 : (negativeParts * 100 / validRecords);
            if (negativeRate >= 15 || negativeParts >= 10) {
                alerts.add(AlertaConsolidado.builder()
                        .titulo("Alta recorrencia de sentimento negativo")
                        .categoria("Sentimento negativo")
                        .severidade(negativeRate >= 30 ? Severidade.ALTO : Severidade.MEDIO)
                        .ocorrencias(negativeParts)
                        .motivo("A transcricao consolidada possui recorrencia relevante de partes negativas.")
                        .recomendacao("Priorizar leitura dos itens criticos e planejar contato de recuperacao com responsavel comercial.")
                        .build());
            }
            if (churnSignals > 0) {
                alerts.add(AlertaConsolidado.builder()
                        .titulo("Indicadores de churn no JSON")
                        .categoria("Churn")
                        .severidade(churnSignals >= 5 ? Severidade.ALTO : Severidade.MEDIO)
                        .ocorrencias(churnSignals)
                        .motivo("Foram encontrados termos associados a cancelamento, troca de fornecedor ou nao renovacao.")
                        .recomendacao("Abrir plano de retencao, revisar causas recorrentes e acionar o time responsavel pela conta.")
                        .build());
            }
            if (criticalMentions >= 5) {
                alerts.add(AlertaConsolidado.builder()
                        .titulo("Muitas mencoes criticas operacionais")
                        .categoria("Criticidade operacional")
                        .severidade(criticalMentions >= 15 ? Severidade.ALTO : Severidade.MEDIO)
                        .ocorrencias(criticalMentions)
                        .motivo("A transcricao tem volume alto de mencoes como erro, instabilidade, lentidao, parada ou problema.")
                        .recomendacao("Agrupar causas tecnicas e priorizar plano de acao com suporte, CS e produto.")
                        .build());
            }
            for (Map.Entry<String, Integer> risk : riskCategories.entrySet()) {
                if (risk.getValue() >= 8) {
                    alerts.add(AlertaConsolidado.builder()
                            .titulo("Risco recorrente: " + risk.getKey())
                            .categoria(risk.getKey())
                            .severidade(riskSeverity(risk.getKey(), risk.getValue()))
                            .ocorrencias(risk.getValue())
                            .motivo("Categoria de risco apareceu muitas vezes na transcricao consolidada.")
                            .recomendacao(riskRecommendation(risk.getKey()))
                            .build());
                }
            }
            return alerts;
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

        private void analyzeSignals(TotvsJsonRow row) {
            String text = normalize(row.transcricao());
            boolean churn = hasChurnSignal(text);
            boolean competitor = hasCompetitorSignal(text);
            boolean trustLoss = hasTrustLossSignal(text);
            boolean critical = hasCriticalSignal(text);
            boolean negative = hasNegativeSignal(text) || churn || trustLoss || critical;
            boolean positive = containsAny(text, "otimo", "excelente", "bom atendimento", "satisfeito", "resolveu",
                    "funciona bem", "gostei", "aprovado", "melhorou");
            if (negative) {
                negativeParts++;
            } else if (positive) {
                positiveParts++;
            } else {
                neutralParts++;
            }

            if (churn) {
                churnSignals++;
                addRisk("CHURN", row);
            }
            if (negative) {
                addRisk("Insatisfacao", row);
            }
            if (competitor) {
                addRisk("Concorrencia", row);
            }
            if (trustLoss) {
                addRisk("Perda de confianca", row);
            }
            if (critical) {
                criticalMentions++;
                addCriticalItem("risco", churn ? "Churn" : "Criticidade operacional", row);
            }
            String opportunityCategory = classifyOpportunity(text);
            if (opportunityCategory != null) {
                opportunitySignals++;
                addOpportunity(opportunityCategory, row);
            }
        }

        private boolean hasNegativeSignal(String text) {
            if (containsAny(text, "sem problema", "sem problemas", "nao tem problema", "nao temos problema",
                    "nenhum problema", "tudo bem", "obrigado", "obrigada")) {
                return false;
            }
            return containsAny(text,
                    "erro recorrente", "falha recorrente", "problema recorrente", "problemas recorrentes",
                    "nao funciona", "nao esta funcionando", "parou de funcionar", "sistema fora",
                    "instabilidade", "instavel", "lentidao", "muito lento", "travando",
                    "insatisfeito", "insatisfeita", "reclamacao", "reclamar", "atraso no atendimento",
                    "suporte nao responde", "suporte demorou", "dificuldade operacional");
        }

        private boolean hasChurnSignal(String text) {
            return containsAny(text,
                    "cancelar contrato", "cancelamento do contrato", "cancelar a totvs", "cancelar com a totvs",
                    "churn", "trocar fornecedor", "trocar de sistema", "migrar para concorrente",
                    "nao renovar contrato", "nao vamos renovar", "encerrar contrato", "sair da totvs");
        }

        private boolean hasCompetitorSignal(String text) {
            return containsAny(text,
                    "concorrente", "proposta da sap", "proposta do sap", "sap business",
                    "oracle netsuite", "senior sistemas", "sankhya", "linx", "omie", "bling");
        }

        private boolean hasTrustLossSignal(String text) {
            return containsAny(text,
                    "perdi confianca", "perdemos confianca", "sem confianca", "desconfio",
                    "prometeram e nao cumpriram", "nao cumpriu o combinado", "quebrou a confianca");
        }

        private boolean hasCriticalSignal(String text) {
            return containsAny(text,
                    "critico", "urgente", "grave", "sem operar", "impacta faturamento",
                    "producao parada", "folha parada", "nao consigo emitir", "sistema fora");
        }

        private String classifyOpportunity(String text) {
            if (containsAny(text, "contratar treinamento", "precisamos de treinamento", "treinamento para equipe",
                    "capacitar equipe", "capacitacao")) {
                return "Treinamento e adocao";
            }
            if (containsAny(text, "integrar com", "integracao com", "precisamos integrar", "api de integracao",
                    "conectar com", "sincronizar com")) {
                return "Integracao";
            }
            if (containsAny(text, "novo modulo", "modulo adicional", "mais licencas", "comprar licenca",
                    "contratar modulo", "upgrade de plano", "ampliar contrato")) {
                return "Upsell";
            }
            if (containsAny(text, "precisamos automatizar", "queremos automatizar", "melhorar processo",
                    "dashboard gerencial", "relatorio gerencial", "orcamento", "proposta comercial",
                    "interesse em contratar")) {
                return "Melhoria no atendimento";
            }
            return null;
        }

        private String normalize(String value) {
            return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase();
        }

        private boolean containsAny(String text, String... terms) {
            for (String term : terms) {
                if (text.contains(term)) {
                    return true;
                }
            }
            return false;
        }

        private void addRisk(String category, TotvsJsonRow row) {
            riskCategories.merge(category, 1, Integer::sum);
            addEvidence(riskEvidences, category, row);
        }

        private void addOpportunity(String category, TotvsJsonRow row) {
            opportunityCategories.merge(category, 1, Integer::sum);
            addEvidence(opportunityEvidences, category, row);
        }

        private void addEvidence(Map<String, List<String>> target, String category, TotvsJsonRow row) {
            List<String> evidences = target.computeIfAbsent(category, key -> new ArrayList<>());
            if (evidences.size() >= MAX_EVIDENCES_PER_CATEGORY) {
                return;
            }
            String transcript = row.transcricao().replaceAll("\\s+", " ").trim();
            evidences.add("ID " + row.idMeeting() + ": " + transcript.substring(0, Math.min(240, transcript.length())));
        }

        private void addCriticalItem(String type, String category, TotvsJsonRow row) {
            if (criticalItems.size() >= 20) {
                return;
            }
            String transcript = row.transcricao().replaceAll("\\s+", " ").trim();
            criticalItems.add(ItemCritico.builder()
                    .tipo(type)
                    .categoria(category)
                    .trecho(transcript.substring(0, Math.min(300, transcript.length())))
                    .severidade("Churn".equals(category) ? Severidade.ALTO : Severidade.MEDIO)
                    .build());
        }

        private int totalRisks() {
            return riskCategories.values().stream().mapToInt(Integer::intValue).sum();
        }

        private Severidade riskSeverity(String category, int count) {
            if ("CHURN".equalsIgnoreCase(category) || count >= 15) {
                return Severidade.ALTO;
            }
            return count >= 5 ? Severidade.MEDIO : Severidade.BAIXO;
        }

        private String riskRecommendation(String category) {
            return switch (category.toUpperCase()) {
                case "CHURN" -> "Acionar plano de retencao, mapear causa raiz e envolver lideranca comercial.";
                case "CONCORRENCIA" -> "Comparar proposta concorrente, reforcar diferenciais TOTVS e remover bloqueios comerciais.";
                case "PERDA DE CONFIANCA" -> "Criar plano de recuperacao com prazos, responsaveis e acompanhamento executivo.";
                default -> "Agrupar evidencias, priorizar causas recorrentes e acompanhar evolucao com o cliente.";
            };
        }

        private String opportunityRecommendation(String category) {
            return switch (category.toUpperCase()) {
                case "UPSELL" -> "Validar aderencia, estimar valor e encaminhar proposta com modulo relacionado.";
                case "INTEGRACAO" -> "Mapear sistemas envolvidos, integrações existentes e esforço tecnico.";
                case "TREINAMENTO E ADOCAO" -> "Planejar capacitacao direcionada para reduzir atrito e aumentar uso.";
                default -> "Transformar a recorrencia em backlog de melhoria e acompanhar impacto no cliente.";
            };
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

        private void addSample(TotvsJsonRow row) {
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
                .origemTranscricao(t.getOrigemTranscricao())
                .meetingId(t.getMeetingId())
                .meetingDate(t.getMeetingDate())
                .format(t.getFormat())
                .meetingStatus(t.getMeetingStatus())
                .duration(t.getDuration())
                .codt(t.getCodt())
                .external(t.getExternal())
                .createdAtSource(t.getCreatedAtSource())
                .transcriptionText(t.getTranscriptionText())
                .sourceFileName(t.getSourceFileName())
                .importBatchId(t.getImportBatchId())
                .importedAt(t.getImportedAt())
                .missingMeetingId(t.isMissingMeetingId())
                .audioFileName(t.getAudioFileName())
                .transcriptionModel(t.getTranscriptionModel())
                .transcriptionLanguage(t.getTranscriptionLanguage())
                .audioDurationSeconds(t.getAudioDurationSeconds())
                .transcriptionProcessingTimeMs(t.getTranscriptionProcessingTimeMs())
                .transcriptionWarnings(t.getTranscriptionWarnings())
                .status(t.getStatus())
                .dataCriacao(t.getDataCriacao())
                .dataProcessamento(t.getDataProcessamento())
                .metricasAgregadas(t.getMetricasAgregadas())
                .resumoExecutivo(t.getResumoExecutivo())
                .riscosConsolidados(t.getRiscosConsolidados())
                .oportunidadesConsolidadas(t.getOportunidadesConsolidadas())
                .alertasConsolidados(t.getAlertasConsolidados())
                .itensCriticos(t.getItensCriticos());

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
