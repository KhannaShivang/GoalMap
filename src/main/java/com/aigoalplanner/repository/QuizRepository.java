package com.aigoalplanner.repository;

import com.aigoalplanner.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByTaskId(Long taskId);

    boolean existsByTaskId(Long taskId);

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.task.id = :taskId")
    Optional<Quiz> findByTaskIdWithQuestions(@Param("taskId") Long taskId);

    void deleteByTaskId(Long taskId);
}
