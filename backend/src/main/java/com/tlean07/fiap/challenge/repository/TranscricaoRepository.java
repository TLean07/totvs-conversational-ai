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

    /* Antes projetava apenas id/cliente/listas consolidadas; no Oracle a consulta base e a mesma. */
    @Query("select t from Transcricao t order by t.dataCriacao desc")
    List<Transcricao> findAllForInsights();

    @Query("select t from Transcricao t where t.ownerId = :ownerId order by t.dataCriacao desc")
    List<Transcricao> findAllByOwnerIdForInsights(@Param("ownerId") String ownerId);

    List<Transcricao> findByOwnerIdAndStatus(String ownerId, StatusTranscricao status);
    List<Transcricao> findByOwnerIdAndClienteContainingIgnoreCase(String ownerId, String cliente);
    Optional<Transcricao> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByMeetingId(String meetingId);
    long countByStatus(StatusTranscricao status);
    long countByOwnerId(String ownerId);
    long countByOwnerIdAndStatus(String ownerId, StatusTranscricao status);
}
