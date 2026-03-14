package com.aigoalplanner.repository;

import com.aigoalplanner.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByRoadmapIdOrderByPriorityAsc(Long roadmapId);

    List<Task> findByRoadmapIdAndCompleted(Long roadmapId, Boolean completed);

    long countByRoadmapId(Long roadmapId);

    long countByRoadmapIdAndCompleted(Long roadmapId, Boolean completed);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.skill WHERE t.roadmap.id = :roadmapId ORDER BY t.priority ASC")
    List<Task> findByRoadmapIdWithSkill(@Param("roadmapId") Long roadmapId);

    @Query("SELECT t FROM Task t WHERE t.roadmap.user.id = :userId AND t.completed = true ORDER BY t.completedAt DESC")
    List<Task> findCompletedTasksByUserId(@Param("userId") Long userId);
}
