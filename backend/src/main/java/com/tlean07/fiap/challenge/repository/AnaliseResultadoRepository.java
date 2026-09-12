package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.Sentimento;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.OportunidadeConsolidada;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.RiscoConsolidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnaliseResultadoRepository extends JpaRepository<AnaliseResultado, String> {

    Optional<AnaliseResultado> findByTranscricaoId(String transcricaoId);
    Optional<AnaliseResultado> findByTranscricaoIdAndOwnerId(String transcricaoId, String ownerId);

    @Transactional
    void deleteByTranscricaoId(String transcricaoId);

    List<AnaliseResultado> findBySentimentoGeral(Sentimento sentimento);
    List<AnaliseResultado> findAllByOrderByDataAnaliseDesc();
    List<AnaliseResultado> findAllByOwnerIdOrderByDataAnaliseDesc(String ownerId);
    List<AnaliseResultado> findTop100ByOwnerIdOrderByDataAnaliseDesc(String ownerId);

    /*
     * Projecoes para o dashboard e os insights. Carregar a entidade inteira (CLOBs + 8 tabelas filhas)
     * para centenas de analises levava minutos no plano free; aqui cada consulta traz so as colunas ou
     * a tabela filha que os graficos usam.
     */

    interface DashboardRow {
        Sentimento getSentimentoGeral();

        Integer getTotalPartes();

        Integer getPartesAnalisadas();

        Integer getPartesNegativas();

        Integer getPartesPositivas();

        Integer getPartesNeutras();

        Integer getChurnsPotenciais();

        Integer getMencoesCriticas();

        Integer getAlertasConsolidados();
    }

    String DASHBOARD_SELECT = "select a.sentimentoGeral as sentimentoGeral,"
            + " a.metricasAgregadas.totalPartes as totalPartes,"
            + " a.metricasAgregadas.partesAnalisadas as partesAnalisadas,"
            + " a.metricasAgregadas.partesNegativas as partesNegativas,"
            + " a.metricasAgregadas.partesPositivas as partesPositivas,"
            + " a.metricasAgregadas.partesNeutras as partesNeutras,"
            + " a.metricasAgregadas.churnsPotenciais as churnsPotenciais,"
            + " a.metricasAgregadas.mencoesCriticas as mencoesCriticas,"
            + " a.metricasAgregadas.alertasConsolidados as alertasConsolidados"
            + " from AnaliseResultado a";

    @Query(DASHBOARD_SELECT)
    List<DashboardRow> findDashboardRows();

    @Query(DASHBOARD_SELECT + " where a.ownerId = :ownerId")
    List<DashboardRow> findDashboardRowsByOwnerId(@Param("ownerId") String ownerId);

    /** Contagens agregadas no banco para o dashboard: nenhuma linha de detalhe e nenhum CLOB trafegam. */
    interface ContagemRow {
        String getChave();

        Long getTotal();
    }

    @Query("select r.categoria as chave, count(r) as total from AnaliseResultado a join a.riscosConsolidados r group by r.categoria")
    List<ContagemRow> contarRiscosConsolidadosPorCategoria();

    @Query("select r.categoria as chave, count(r) as total from AnaliseResultado a join a.riscosConsolidados r where a.ownerId = :ownerId group by r.categoria")
    List<ContagemRow> contarRiscosConsolidadosPorCategoria(@Param("ownerId") String ownerId);

    /** Riscos brutos so das analises sem lista consolidada (mesma regra de preferencia do dashboard). */
    @Query("select cast(r.tipo as string) as chave, count(r) as total from AnaliseResultado a join a.riscos r where a.riscosConsolidados is empty group by r.tipo")
    List<ContagemRow> contarRiscosSemConsolidadoPorTipo();

    @Query("select cast(r.tipo as string) as chave, count(r) as total from AnaliseResultado a join a.riscos r where a.riscosConsolidados is empty and a.ownerId = :ownerId group by r.tipo")
    List<ContagemRow> contarRiscosSemConsolidadoPorTipo(@Param("ownerId") String ownerId);

    @Query("select o.categoria as chave, count(o) as total from AnaliseResultado a join a.oportunidadesConsolidadas o group by o.categoria")
    List<ContagemRow> contarOportunidadesConsolidadasPorCategoria();

    @Query("select o.categoria as chave, count(o) as total from AnaliseResultado a join a.oportunidadesConsolidadas o where a.ownerId = :ownerId group by o.categoria")
    List<ContagemRow> contarOportunidadesConsolidadasPorCategoria(@Param("ownerId") String ownerId);

    @Query("select cast(o.tipo as string) as chave, count(o) as total from AnaliseResultado a join a.oportunidades o where a.oportunidadesConsolidadas is empty group by o.tipo")
    List<ContagemRow> contarOportunidadesSemConsolidadoPorTipo();

    @Query("select cast(o.tipo as string) as chave, count(o) as total from AnaliseResultado a join a.oportunidades o where a.oportunidadesConsolidadas is empty and a.ownerId = :ownerId group by o.tipo")
    List<ContagemRow> contarOportunidadesSemConsolidadoPorTipo(@Param("ownerId") String ownerId);

    /** Resumo da analise para listas de transcricoes (sem as tabelas filhas). */
    interface ResumoRow {
        String getId();

        String getTranscricaoId();

        Sentimento getSentimentoGeral();

        String getResumo();

        String getPersonaIdentificada();

        String getBudgetIdentificado();

        Double getScoreConfiancaVendedor();

        Double getScoreSentimento();

        String getProximaAcao();

        String getSentimentoDetalhe();

        java.time.LocalDateTime getDataAnalise();
    }

    @Query("select a.id as id, a.transcricaoId as transcricaoId, a.sentimentoGeral as sentimentoGeral, a.resumo as resumo,"
            + " a.personaIdentificada as personaIdentificada, a.budgetIdentificado as budgetIdentificado,"
            + " a.scoreConfiancaVendedor as scoreConfiancaVendedor, a.scoreSentimento as scoreSentimento,"
            + " a.proximaAcao as proximaAcao, a.sentimentoDetalhe as sentimentoDetalhe, a.dataAnalise as dataAnalise"
            + " from AnaliseResultado a where a.transcricaoId in :ids")
    List<ResumoRow> findResumoByTranscricaoIdIn(@Param("ids") java.util.Collection<String> ids);

    interface ProdutoRow {
        String getTranscricaoId();

        com.tlean07.fiap.challenge.model.ProdutoIdentificado getProduto();
    }

    @Query("select a.transcricaoId as transcricaoId, p as produto from AnaliseResultado a join a.produtosIdentificados p where a.transcricaoId in :ids")
    List<ProdutoRow> findProdutoRowsByTranscricaoIdIn(@Param("ids") java.util.Collection<String> ids);

    interface RiscoRow {
        String getTranscricaoId();

        Risco getRisco();
    }

    interface OportunidadeRow {
        String getTranscricaoId();

        Oportunidade getOportunidade();
    }

    interface RiscoConsolidadoRow {
        String getTranscricaoId();

        RiscoConsolidado getRisco();
    }

    interface OportunidadeConsolidadaRow {
        String getTranscricaoId();

        OportunidadeConsolidada getOportunidade();
    }

    @Query("select a.transcricaoId as transcricaoId, r as risco from AnaliseResultado a join a.riscos r")
    List<RiscoRow> findRiscoRows();

    @Query("select a.transcricaoId as transcricaoId, r as risco from AnaliseResultado a join a.riscos r where a.ownerId = :ownerId")
    List<RiscoRow> findRiscoRowsByOwnerId(@Param("ownerId") String ownerId);

    @Query("select a.transcricaoId as transcricaoId, o as oportunidade from AnaliseResultado a join a.oportunidades o")
    List<OportunidadeRow> findOportunidadeRows();

    @Query("select a.transcricaoId as transcricaoId, o as oportunidade from AnaliseResultado a join a.oportunidades o where a.ownerId = :ownerId")
    List<OportunidadeRow> findOportunidadeRowsByOwnerId(@Param("ownerId") String ownerId);

    @Query("select a.transcricaoId as transcricaoId, r as risco from AnaliseResultado a join a.riscosConsolidados r")
    List<RiscoConsolidadoRow> findRiscoConsolidadoRows();

    @Query("select a.transcricaoId as transcricaoId, r as risco from AnaliseResultado a join a.riscosConsolidados r where a.ownerId = :ownerId")
    List<RiscoConsolidadoRow> findRiscoConsolidadoRowsByOwnerId(@Param("ownerId") String ownerId);

    @Query("select a.transcricaoId as transcricaoId, o as oportunidade from AnaliseResultado a join a.oportunidadesConsolidadas o")
    List<OportunidadeConsolidadaRow> findOportunidadeConsolidadaRows();

    @Query("select a.transcricaoId as transcricaoId, o as oportunidade from AnaliseResultado a join a.oportunidadesConsolidadas o where a.ownerId = :ownerId")
    List<OportunidadeConsolidadaRow> findOportunidadeConsolidadaRowsByOwnerId(@Param("ownerId") String ownerId);
}
