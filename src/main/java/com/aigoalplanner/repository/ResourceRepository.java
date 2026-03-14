package com.aigoalplanner.repository;

import com.aigoalplanner.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findBySkillId(Long skillId);

    List<Resource> findBySkillIdAndDifficulty(Long skillId, Resource.Difficulty difficulty);

    @Query("SELECT r FROM Resource r WHERE r.skill.id = :skillId AND r.embedding IS NOT NULL")
    List<Resource> findBySkillIdWithEmbedding(@Param("skillId") Long skillId);

    // Native query for pgvector cosine similarity search
    @Query(value = """
        SELECT r.* FROM resources r
        WHERE r.embedding IS NOT NULL
        ORDER BY r.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Resource> findSimilarResources(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("limit") int limit
    );

    // Similarity search scoped to a specific skill
    @Query(value = """
        SELECT r.* FROM resources r
        WHERE r.skill_id = :skillId AND r.embedding IS NOT NULL
        ORDER BY r.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Resource> findSimilarResourcesBySkill(
        @Param("skillId") Long skillId,
        @Param("queryEmbedding") String queryEmbedding,
        @Param("limit") int limit
    );

    List<Resource> findByEmbeddingIsNull();
}
