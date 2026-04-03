package com.aigoalplanner.repository;

import com.aigoalplanner.model.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTaskIdOrderByOrderIndexAsc(Long taskId);
    long countByTaskId(Long taskId);
    long countByTaskIdAndCompleted(Long taskId, Boolean completed);
    boolean existsByTaskId(Long taskId);
}
