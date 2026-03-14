package com.aigoalplanner.repository;

import com.aigoalplanner.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findByCategory(String category);

    @Query("SELECT s FROM Skill s LEFT JOIN FETCH s.dependencies d LEFT JOIN FETCH d.dependsOn WHERE s.id = :id")
    Optional<Skill> findByIdWithDependencies(@Param("id") Long id);

    boolean existsByNameIgnoreCase(String name);
}
