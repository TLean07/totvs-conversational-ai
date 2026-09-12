package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, String> {
    List<Alerta> findByStatus(StatusAlerta status);
    List<Alerta> findByPrioridade(Severidade prioridade);
    List<Alerta> findByStatusOrderByPrioridadeAscDataCriacaoDesc(StatusAlerta status);
    List<Alerta> findTop10ByStatusOrderByPrioridadeAscDataCriacaoDesc(StatusAlerta status);
    List<Alerta> findByAnaliseId(String analiseId);

    @Transactional
    void deleteByAnaliseId(String analiseId);

    long countByStatus(StatusAlerta status);
    List<Alerta> findByOwnerId(String ownerId);
    List<Alerta> findByOwnerIdAndStatusOrderByPrioridadeAscDataCriacaoDesc(String ownerId, StatusAlerta status);
    List<Alerta> findTop10ByOwnerIdAndStatusOrderByPrioridadeAscDataCriacaoDesc(String ownerId, StatusAlerta status);
    long countByOwnerIdAndStatus(String ownerId, StatusAlerta status);
    Optional<Alerta> findByIdAndOwnerId(String id, String ownerId);
}
