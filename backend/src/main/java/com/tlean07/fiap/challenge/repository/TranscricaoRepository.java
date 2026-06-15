package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscricaoRepository extends MongoRepository<Transcricao, String> {
    List<Transcricao> findByStatus(StatusTranscricao status);
    List<Transcricao> findByClienteContainingIgnoreCase(String cliente);
    List<Transcricao> findAllByOrderByDataCriacaoDesc();
    Page<Transcricao> findAllByOrderByDataCriacaoDesc(Pageable pageable);
    List<Transcricao> findByOwnerIdOrderByDataCriacaoDesc(String ownerId);
    Page<Transcricao> findByOwnerIdOrderByDataCriacaoDesc(String ownerId, Pageable pageable);
    List<Transcricao> findTop5ByOrderByDataCriacaoDesc();
    List<Transcricao> findTop5ByOwnerIdOrderByDataCriacaoDesc(String ownerId);
    @Query(value = "{}", fields = "{'_id': 1, 'cliente': 1, 'riscosConsolidados': 1, 'oportunidadesConsolidadas': 1}",
            sort = "{'dataCriacao': -1}")
    List<Transcricao> findAllForInsights();
    @Query(value = "{'ownerId': ?0}", fields = "{'_id': 1, 'cliente': 1, 'riscosConsolidados': 1, 'oportunidadesConsolidadas': 1}",
            sort = "{'dataCriacao': -1}")
    List<Transcricao> findAllByOwnerIdForInsights(String ownerId);
    List<Transcricao> findByOwnerIdAndStatus(String ownerId, StatusTranscricao status);
    List<Transcricao> findByOwnerIdAndClienteContainingIgnoreCase(String ownerId, String cliente);
    java.util.Optional<Transcricao> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByMeetingId(String meetingId);
    long countByStatus(StatusTranscricao status);
    long countByOwnerId(String ownerId);
    long countByOwnerIdAndStatus(String ownerId, StatusTranscricao status);
}
