package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.config.CacheConfig.CacheNames;
import com.tlean07.fiap.challenge.dto.TranscricaoInsightsResponse;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Insights por transcricao para o dashboard. As transcricoes entram por uma projecao (id e cliente):
 * carregar a entidade inteira trazia o texto completo de cada reuniao (~35 mil caracteres) so para
 * montar os graficos, o que travava o dashboard depois de uma importacao grande.
 */
@Service
public class TranscricaoInsightsService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;

    public TranscricaoInsightsService(TranscricaoRepository transcricaoRepository,
                                      AnaliseResultadoRepository analiseRepository) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
    }

    @Cacheable(cacheNames = CacheNames.INSIGHTS, key = "@cacheKeys.user()")
    public List<TranscricaoInsightsResponse> listarTodos() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<TranscricaoRepository.InsightRow> transcricoes = user.isAdmin()
                ? transcricaoRepository.findInsightRows()
                : transcricaoRepository.findInsightRowsByOwnerId(user.id());
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
                .map(row -> toResponse(row, analisePorTranscricao.get(row.getId())))
                .toList();
    }

    private TranscricaoInsightsResponse toResponse(TranscricaoRepository.InsightRow row, AnaliseResultado analise) {
        return TranscricaoInsightsResponse.builder()
                .id(row.getId())
                .cliente(row.getCliente())
                .riscosConsolidados(analise == null ? List.of() : safeList(analise.getRiscosConsolidados()))
                .oportunidadesConsolidadas(analise == null ? List.of() : safeList(analise.getOportunidadesConsolidadas()))
                .riscos(analise == null ? List.of() : safeList(analise.getRiscos()))
                .oportunidades(analise == null ? List.of() : safeList(analise.getOportunidades()))
                .build();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
