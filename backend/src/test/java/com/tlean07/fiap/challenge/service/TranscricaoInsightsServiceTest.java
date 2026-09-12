package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.TranscricaoInsightsResponse;
import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.TipoOportunidade;
import com.tlean07.fiap.challenge.enums.TipoRisco;
import com.tlean07.fiap.challenge.enums.UserRole;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.OportunidadeConsolidada;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.RiscoConsolidado;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscricaoInsightsServiceTest {

    @Mock
    private TranscricaoRepository transcricaoRepository;
    @Mock
    private AnaliseResultadoRepository analiseRepository;

    @InjectMocks
    private TranscricaoInsightsService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsEveryTranscriptionAndPreservesConsolidatedAndLegacyInsights() {
        authenticate("admin-1", UserRole.ADMIN);
        TranscricaoRepository.InsightRow first = row("t-1", "Cliente A");
        TranscricaoRepository.InsightRow second = row("t-2", "Cliente B");
        AnaliseResultado firstAnalysis = AnaliseResultado.builder()
                .transcricaoId("t-1")
                .riscosConsolidados(List.of(RiscoConsolidado.builder()
                        .categoria("Churn")
                        .severidade(Severidade.ALTO)
                        .ocorrencias(2)
                        .build()))
                .riscos(List.of(Risco.builder()
                        .tipo(TipoRisco.CHURN)
                        .severidade(Severidade.ALTO)
                        .build()))
                .build();
        AnaliseResultado secondAnalysis = AnaliseResultado.builder()
                .transcricaoId("t-2")
                .oportunidadesConsolidadas(List.of(OportunidadeConsolidada.builder()
                        .categoria("Upsell")
                        .ocorrencias(3)
                        .build()))
                .oportunidades(List.of(Oportunidade.builder()
                        .tipo(TipoOportunidade.UPSELL)
                        .build()))
                .build();
        when(transcricaoRepository.findInsightRows()).thenReturn(List.of(first, second));
        when(analiseRepository.findAllForInsights()).thenReturn(List.of(firstAnalysis, secondAnalysis));

        List<TranscricaoInsightsResponse> result = service.listarTodos();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRiscosConsolidados().size());
        assertEquals(1, result.get(0).getRiscos().size());
        assertEquals(1, result.get(1).getOportunidadesConsolidadas().size());
        assertEquals(1, result.get(1).getOportunidades().size());
    }

    @Test
    void regularUserUsesOwnerFilteredProjectionQueries() {
        authenticate("user-7", UserRole.USER);
        when(transcricaoRepository.findInsightRowsByOwnerId("user-7")).thenReturn(List.of());
        when(analiseRepository.findAllByOwnerIdForInsights("user-7")).thenReturn(List.of());

        List<TranscricaoInsightsResponse> result = service.listarTodos();

        assertTrue(result.isEmpty());
        verify(transcricaoRepository).findInsightRowsByOwnerId("user-7");
        verify(analiseRepository).findAllByOwnerIdForInsights("user-7");
    }

    private static TranscricaoRepository.InsightRow row(String id, String cliente) {
        return new TranscricaoRepository.InsightRow() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getCliente() {
                return cliente;
            }
        };
    }

    private void authenticate(String id, UserRole role) {
        AuthenticatedUser user = new AuthenticatedUser(id, id + "@example.com", id, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
