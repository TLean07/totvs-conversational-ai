package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.AlertaResponse;
import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.enums.TipoProduto;
import com.tlean07.fiap.challenge.enums.TipoRisco;
import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.AlertaConsolidado;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    public List<Alerta> gerarAlertas(AnaliseResultado analise, Transcricao transcricao) {
        List<Alerta> alertas = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();

        for (AlertaConsolidado alerta : safeList(analise.getAlertasConsolidados())) {
            String key = "CONSOLIDADO:" + alerta.getCategoria() + ":" + alerta.getTitulo();
            if (dedupe.add(key)) {
                alertas.add(salvarAlertaConsolidado(analise.getId(), alerta, analise.getOwnerId()));
            }
        }

        for (Risco risco : safeList(analise.getRiscos())) {
            if (risco.getSeveridade() == Severidade.ALTO) {
                String key = "RISCO_" + risco.getTipo().name();
                if (dedupe.add(key)) {
                    alertas.add(salvarAlerta(analise.getId(), key,
                            buildMensagemRisco(risco, transcricao), Severidade.ALTO, analise.getOwnerId()));
                }
            }

            if (risco.getTipo() == TipoRisco.CONCORRENCIA && risco.getConcorrenteMencionado() != null) {
                if (dedupe.add("CONCORRENTE_DETECTADO")) {
                    alertas.add(salvarAlerta(analise.getId(), "CONCORRENTE_DETECTADO",
                            "Cliente avaliando concorrente: " + risco.getConcorrenteMencionado()
                                    + ". " + risco.getDescricao(), Severidade.ALTO, analise.getOwnerId()));
                }
            }
        }

        for (Oportunidade oportunidade : safeList(analise.getOportunidades())) {
            if (oportunidade.getScore() != null && oportunidade.getScore() >= 0.7) {
                String key = "OPORTUNIDADE_ALTA:" + oportunidade.getTipo().name();
                if (dedupe.add(key)) {
                    alertas.add(salvarAlerta(analise.getId(), "OPORTUNIDADE_ALTA",
                            "Oportunidade de " + oportunidade.getTipo().name()
                                    + " com alta probabilidade: " + oportunidade.getDescricao(), Severidade.MEDIO,
                            analise.getOwnerId()));
                }
            }
        }

        boolean temConcorrenteAtivo = safeList(analise.getProdutosIdentificados()).stream()
                .anyMatch(p -> p.getTipo() == TipoProduto.CONCORRENTE);
        if (temConcorrenteAtivo && analise.getSentimentoGeral().name().equals("NEGATIVO")) {
            if (dedupe.add("CHURN_IMINENTE")) {
                alertas.add(salvarAlerta(analise.getId(), "CHURN_IMINENTE",
                        "ATENCAO: Cliente com sentimento negativo e avaliando concorrentes. Risco de churn iminente!",
                        Severidade.ALTO, analise.getOwnerId()));
            }
        }

        return alertas;
    }

    private Alerta salvarAlertaConsolidado(String analiseId, AlertaConsolidado consolidado, String ownerId) {
        String message = "%s. Ocorrencias: %d. Recomendacao: %s".formatted(
                consolidado.getMotivo(),
                consolidado.getOcorrencias(),
                consolidado.getRecomendacao()
        );
        Alerta alerta = Alerta.builder()
                .ownerId(ownerId)
                .analiseId(analiseId)
                .tipo("CONSOLIDADO_" + normalizeType(consolidado.getCategoria()))
                .titulo(consolidado.getTitulo())
                .categoria(consolidado.getCategoria())
                .mensagem(message)
                .motivo(consolidado.getMotivo())
                .recomendacao(consolidado.getRecomendacao())
                .ocorrencias(consolidado.getOcorrencias())
                .prioridade(consolidado.getSeveridade())
                .status(StatusAlerta.NOVO)
                .dataCriacao(LocalDateTime.now())
                .build();
        return alertaRepository.save(alerta);
    }

    private Alerta salvarAlerta(String analiseId, String tipo, String mensagem, Severidade prioridade, String ownerId) {
        Alerta alerta = Alerta.builder()
                .ownerId(ownerId)
                .analiseId(analiseId)
                .tipo(tipo)
                .titulo(tipo)
                .categoria(tipo)
                .mensagem(mensagem)
                .prioridade(prioridade)
                .status(StatusAlerta.NOVO)
                .dataCriacao(LocalDateTime.now())
                .build();
        return alertaRepository.save(alerta);
    }

    private String normalizeType(String value) {
        String normalized = value == null || value.isBlank() ? "GERAL" : value;
        return java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String buildMensagemRisco(Risco risco, Transcricao transcricao) {
        String cliente = transcricao.getCliente();
        return "Risco %s detectado%s: %s".formatted(
                risco.getTipo().name(),
                cliente != null ? " para cliente " + cliente : "",
                risco.getDescricao()
        );
    }

    public List<AlertaResponse> listarNovos() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<Alerta> alertas = user.isAdmin()
                ? alertaRepository.findByStatusOrderByPrioridadeAscDataCriacaoDesc(StatusAlerta.NOVO)
                : alertaRepository.findByOwnerIdAndStatusOrderByPrioridadeAscDataCriacaoDesc(user.id(), StatusAlerta.NOVO);

        return alertas
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AlertaResponse> listarTodos() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<Alerta> alertas = user.isAdmin()
                ? alertaRepository.findAll()
                : alertaRepository.findByOwnerId(user.id());

        return alertas
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AlertaResponse marcarComoVisto(String id) {
        Alerta alerta = findVisibleAlerta(id);
        alerta.setStatus(StatusAlerta.VISTO);
        return toResponse(alertaRepository.save(alerta));
    }

    public AlertaResponse marcarComoResolvido(String id) {
        Alerta alerta = findVisibleAlerta(id);
        alerta.setStatus(StatusAlerta.RESOLVIDO);
        return toResponse(alertaRepository.save(alerta));
    }

    public long contarNovos() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return user.isAdmin()
                ? alertaRepository.countByStatus(StatusAlerta.NOVO)
                : alertaRepository.countByOwnerIdAndStatus(user.id(), StatusAlerta.NOVO);
    }

    private Alerta findVisibleAlerta(String id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return (user.isAdmin()
                ? alertaRepository.findById(id)
                : alertaRepository.findByIdAndOwnerId(id, user.id()))
                .orElseThrow(() -> new AccessDeniedException("Alerta nao encontrado"));
    }

    public AlertaResponse toResponse(Alerta a) {
        return AlertaResponse.builder()
                .id(a.getId())
                .tipo(a.getTipo())
                .titulo(a.getTitulo())
                .categoria(a.getCategoria())
                .mensagem(a.getMensagem())
                .motivo(a.getMotivo())
                .recomendacao(a.getRecomendacao())
                .ocorrencias(a.getOcorrencias())
                .prioridade(a.getPrioridade())
                .status(a.getStatus())
                .dataCriacao(a.getDataCriacao())
                .build();
    }
}
