package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.TranscricaoInsightsResponse;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TranscricaoInsightsService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;

    public TranscricaoInsightsService(TranscricaoRepository transcricaoRepository,
                                      AnaliseResultadoRepository analiseRepository) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
    }

    public List<TranscricaoInsightsResponse> listarTodos() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<Transcricao> transcricoes = user.isAdmin()
                ? transcricaoRepository.findAllForInsights()
                : transcricaoRepository.findAllByOwnerIdForInsights(user.id());
        List<AnaliseResultado> analises = user.isAdmin()
                ? analiseRepository.findAllForInsights()
                : analiseRepository.findAllByOwnerIdForInsights(user.id());

        Map<String, AnaliseResultado> analisePorTranscricao = analises.stream()
                .filter(analise -> analise.getTranscricaoId() != null)
                .collect(Collectors.toMap(
                        AnaliseResultado::getTranscricaoId,
                        Function.identity(),
                        (first, ignored) -> first));

        return transcricoes.stream()
                .map(transcricao -> toResponse(transcricao, analisePorTranscricao.get(transcricao.getId())))
                .toList();
    }

    private TranscricaoInsightsResponse toResponse(Transcricao transcricao, AnaliseResultado analise) {
        return TranscricaoInsightsResponse.builder()
                .id(transcricao.getId())
                .cliente(transcricao.getCliente())
                .riscosConsolidados(prefer(
                        analise == null ? null : analise.getRiscosConsolidados(),
                        transcricao.getRiscosConsolidados()))
                .oportunidadesConsolidadas(prefer(
                        analise == null ? null : analise.getOportunidadesConsolidadas(),
                        transcricao.getOportunidadesConsolidadas()))
                .riscos(analise == null ? List.of() : safeList(analise.getRiscos()))
                .oportunidades(analise == null ? List.of() : safeList(analise.getOportunidades()))
                .build();
    }

    private <T> List<T> prefer(List<T> primary, List<T> secondary) {
        return primary != null && !primary.isEmpty() ? primary : safeList(secondary);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
