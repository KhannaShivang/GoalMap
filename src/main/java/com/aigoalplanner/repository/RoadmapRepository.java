package com.aigoalplanner.repository;

import com.aigoalplanner.model.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    List<Roadmap> findByUserId(Long userId);

    List<Roadmap> findByGoalId(Long goalId);

    @Query("SELECT r FROM Roadmap r LEFT JOIN FETCH r.tasks WHERE r.id = :id")
    Optional<Roadmap> findByIdWithTasks(@Param("id") Long id);

    @Query("SELECT r FROM Roadmap r LEFT JOIN FETCH r.tasks WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Roadmap> findByUserIdWithTasks(@Param("userId") Long userId);

    Optional<Roadmap> findTopByGoalIdOrderByCreatedAtDesc(Long goalId);
}
