package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.model.AnaliseFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AnaliseFeedbackRepository extends JpaRepository<AnaliseFeedback, String> {
    List<AnaliseFeedback> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<AnaliseFeedback> findByTranscricaoIdOrderByCreatedAtDesc(String transcricaoId);
    List<AnaliseFeedback> findByTranscricaoIdAndOwnerIdOrderByCreatedAtDesc(String transcricaoId, String ownerId);

    @Transactional
    void deleteByTranscricaoId(String transcricaoId);
}
