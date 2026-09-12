package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.DashboardResponse;
import com.tlean07.fiap.challenge.enums.Sentimento;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.enums.UserRole;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TranscricaoRepository transcricaoRepository;
    @Mock
    private AnaliseResultadoRepository analiseRepository;
    @Mock
    private AlertaRepository alertaRepository;
    @Mock
    private AlertaService alertaService;
    @Mock
    private TranscricaoService transcricaoService;

    @InjectMocks
    private DashboardService dashboardService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminUsesDashboardSpecificQueriesWithoutLoadingFullCollections() {
        authenticate("admin-1", UserRole.ADMIN);
        when(analiseRepository.findDashboardRows()).thenReturn(List.of(
                row(Sentimento.POSITIVO), row(Sentimento.NEGATIVO), row(Sentimento.POSITIVO)));
        when(transcricaoRepository.count()).thenReturn(12L);
        when(transcricaoRepository.countByStatus(StatusTranscricao.PROCESSADA)).thenReturn(9L);
        when(analiseRepository.contarRiscosConsolidadosPorCategoria()).thenReturn(List.of(
                contagem("Churn", 1L), contagem("Concorrencia", 1L)));
        when(analiseRepository.contarOportunidadesConsolidadasPorCategoria()).thenReturn(List.of(
                contagem("Upsell", 1L)));
        when(alertaService.listarNovosDashboard()).thenReturn(List.of());
        when(transcricaoService.listarRecentesDashboard()).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(12L, response.getTotalTranscricoes());
        assertEquals(9L, response.getTranscricoesProcessadas());
        assertEquals(2L, response.getTotalRiscos());
        assertEquals(1L, response.getTotalOportunidades());
        assertEquals(2L, response.getSentimentoDistribuicao().get("POSITIVO"));
        assertEquals(1L, response.getSentimentoDistribuicao().get("NEGATIVO"));
        verify(analiseRepository).findDashboardRows();
        verify(analiseRepository, never()).findAll();
        verify(transcricaoService).listarRecentesDashboard();
        verify(transcricaoService, never()).listarTodas();
        verify(alertaService).listarNovosDashboard();
        verify(alertaService, never()).listarNovos();
    }

    @Test
    void regularUserKeepsOwnerFilteringOnOptimizedQueries() {
        authenticate("user-7", UserRole.USER);
        when(analiseRepository.findDashboardRowsByOwnerId("user-7")).thenReturn(List.of());
        when(transcricaoRepository.countByOwnerId("user-7")).thenReturn(4L);
        when(transcricaoRepository.countByOwnerIdAndStatus("user-7", StatusTranscricao.PROCESSADA))
                .thenReturn(3L);
        when(alertaService.listarNovosDashboard()).thenReturn(List.of());
        when(transcricaoService.listarRecentesDashboard()).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(4L, response.getTotalTranscricoes());
        assertEquals(3L, response.getTranscricoesProcessadas());
        verify(analiseRepository).findDashboardRowsByOwnerId("user-7");
        verify(transcricaoRepository).countByOwnerId("user-7");
        verify(transcricaoRepository).countByOwnerIdAndStatus("user-7", StatusTranscricao.PROCESSADA);
    }

    private static AnaliseResultadoRepository.ContagemRow contagem(String chave, long total) {
        return new AnaliseResultadoRepository.ContagemRow() {
            @Override public String getChave() { return chave; }
            @Override public Long getTotal() { return total; }
        };
    }

    private static AnaliseResultadoRepository.DashboardRow row(Sentimento sentimento) {
        return new AnaliseResultadoRepository.DashboardRow() {
            @Override public Sentimento getSentimentoGeral() { return sentimento; }
            @Override public Integer getTotalPartes() { return 0; }
            @Override public Integer getPartesAnalisadas() { return 0; }
            @Override public Integer getPartesNegativas() { return 0; }
            @Override public Integer getPartesPositivas() { return 0; }
            @Override public Integer getPartesNeutras() { return 0; }
            @Override public Integer getChurnsPotenciais() { return 0; }
            @Override public Integer getMencoesCriticas() { return 0; }
            @Override public Integer getAlertasConsolidados() { return 0; }
        };
    }

    private void authenticate(String id, UserRole role) {
        AuthenticatedUser user = new AuthenticatedUser(id, id + "@example.com", id, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
