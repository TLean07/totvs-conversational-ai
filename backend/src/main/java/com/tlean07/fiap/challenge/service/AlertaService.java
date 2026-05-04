package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.AlertaResponse;
import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.enums.TipoProduto;
import com.tlean07.fiap.challenge.enums.TipoRisco;
import com.tlean07.fiap.challenge.model.Alerta;
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
import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    public List<Alerta> gerarAlertas(AnaliseResultado analise, Transcricao transcricao) {
        List<Alerta> alertas = new ArrayList<>();

        for (Risco risco : analise.getRiscos()) {
            if (risco.getSeveridade() == Severidade.ALTO) {
                alertas.add(salvarAlerta(analise.getId(), "RISCO_" + risco.getTipo().name(),
                        buildMensagemRisco(risco, transcricao), Severidade.ALTO, analise.getOwnerId()));
            }

            if (risco.getTipo() == TipoRisco.CONCORRENCIA && risco.getConcorrenteMencionado() != null) {
                alertas.add(salvarAlerta(analise.getId(), "CONCORRENTE_DETECTADO",
                        "Cliente avaliando concorrente: " + risco.getConcorrenteMencionado()
                                + ". " + risco.getDescricao(), Severidade.ALTO, analise.getOwnerId()));
            }
        }

        for (Oportunidade oportunidade : analise.getOportunidades()) {
            if (oportunidade.getScore() != null && oportunidade.getScore() >= 0.7) {
                alertas.add(salvarAlerta(analise.getId(), "OPORTUNIDADE_ALTA",
                        "Oportunidade de " + oportunidade.getTipo().name()
                                + " com alta probabilidade: " + oportunidade.getDescricao(), Severidade.MEDIO,
                        analise.getOwnerId()));
            }
        }

        boolean temConcorrenteAtivo = analise.getProdutosIdentificados().stream()
                .anyMatch(p -> p.getTipo() == TipoProduto.CONCORRENTE);
        if (temConcorrenteAtivo && analise.getSentimentoGeral().name().equals("NEGATIVO")) {
            alertas.add(salvarAlerta(analise.getId(), "CHURN_IMINENTE",
                    "ATENCAO: Cliente com sentimento negativo e avaliando concorrentes. Risco de churn iminente!",
                    Severidade.ALTO, analise.getOwnerId()));
        }

        return alertas;
    }

    private Alerta salvarAlerta(String analiseId, String tipo, String mensagem, Severidade prioridade, String ownerId) {
        Alerta alerta = Alerta.builder()
                .ownerId(ownerId)
                .analiseId(analiseId)
                .tipo(tipo)
                .mensagem(mensagem)
                .prioridade(prioridade)
                .status(StatusAlerta.NOVO)
                .dataCriacao(LocalDateTime.now())
                .build();
        return alertaRepository.save(alerta);
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
                .mensagem(a.getMensagem())
                .prioridade(a.getPrioridade())
                .status(a.getStatus())
                .dataCriacao(a.getDataCriacao())
                .build();
    }
}
