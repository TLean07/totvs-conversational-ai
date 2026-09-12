package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TranscricaoRepository extends JpaRepository<Transcricao, String> {
    List<Transcricao> findByStatus(StatusTranscricao status);
    List<Transcricao> findByClienteContainingIgnoreCase(String cliente);
    List<Transcricao> findAllByOrderByDataCriacaoDesc();
    Page<Transcricao> findAllByOrderByDataCriacaoDesc(Pageable pageable);
    List<Transcricao> findByOwnerIdOrderByDataCriacaoDesc(String ownerId);
    Page<Transcricao> findByOwnerIdOrderByDataCriacaoDesc(String ownerId, Pageable pageable);
    List<Transcricao> findTop5ByOrderByDataCriacaoDesc();
    List<Transcricao> findTop5ByOwnerIdOrderByDataCriacaoDesc(String ownerId);

    /** Projecao leve para o dashboard/insights: nao carrega os CLOBs de texto nem as listas. */
    interface InsightRow {
        String getId();

        String getCliente();
    }

    @Query("select t.id as id, t.cliente as cliente from Transcricao t order by t.dataCriacao desc")
    List<InsightRow> findInsightRows();

    @Query("select t.id as id, t.cliente as cliente from Transcricao t where t.ownerId = :ownerId order by t.dataCriacao desc")
    List<InsightRow> findInsightRowsByOwnerId(@Param("ownerId") String ownerId);

    /** Ids de reuniao ja importados, para a importacao checar duplicados em memoria. */
    @Query("select t.meetingId from Transcricao t where t.meetingId is not null")
    Set<String> findAllMeetingIds();

    List<Transcricao> findTop100ByStatusOrderByDataCriacaoAsc(StatusTranscricao status);

    List<Transcricao> findByOwnerIdAndStatus(String ownerId, StatusTranscricao status);
    List<Transcricao> findByOwnerIdAndClienteContainingIgnoreCase(String ownerId, String cliente);
    Optional<Transcricao> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByMeetingId(String meetingId);
    long countByStatus(StatusTranscricao status);
    long countByOwnerId(String ownerId);
    long countByOwnerIdAndStatus(String ownerId, StatusTranscricao status);
}
