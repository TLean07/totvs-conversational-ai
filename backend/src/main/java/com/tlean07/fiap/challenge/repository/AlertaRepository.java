package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.model.Alerta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends MongoRepository<Alerta, String> {
    List<Alerta> findByStatus(StatusAlerta status);
    List<Alerta> findByPrioridade(Severidade prioridade);
    List<Alerta> findByStatusOrderByPrioridadeAscDataCriacaoDesc(StatusAlerta status);
    long countByStatus(StatusAlerta status);
}
