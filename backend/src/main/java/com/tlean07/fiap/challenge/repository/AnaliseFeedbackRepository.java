package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.model.AnaliseFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnaliseFeedbackRepository extends MongoRepository<AnaliseFeedback, String> {
    List<AnaliseFeedback> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    void deleteByTranscricaoId(String transcricaoId);
}
