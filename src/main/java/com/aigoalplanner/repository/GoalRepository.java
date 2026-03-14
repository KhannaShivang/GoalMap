package com.aigoalplanner.repository;

import com.aigoalplanner.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);
    List<Goal> findByUserIdOrderByCreatedAtAsc(Long userId);
    List<Goal> findByUserIdAndStatus(Long userId, Goal.GoalStatus status);
}