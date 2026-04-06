package com.aigoalplanner.service;

import com.aigoalplanner.dto.AIResourceItem;
import com.aigoalplanner.dto.ResourceDTO;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.Resource;
import com.aigoalplanner.model.Skill;
import com.aigoalplanner.repository.ResourceRepository;
import com.aigoalplanner.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ResourceRecommendationService.class);

    private final ResourceRepository resourceRepository;
    private final SkillRepository skillRepository;
    private final EmbeddingModel embeddingModel;
    private final AIRecommendationService aiService;

    // --------------------------------------------------------
    // Get resources for a skill.
    // If none exist in DB → ask AI to generate them → save → return.
    // --------------------------------------------------------

    @Transactional
    public List<ResourceDTO> getResourcesForSkill(Long skillId, int limit) {
        Skill skill = skillRepository.findById(skillId)
            .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillId));

        List<Resource> existing = resourceRepository.findBySkillId(skillId);

        if (!existing.isEmpty()) {
            log.debug("Returning {} existing resources for skill={}", existing.size(), skill.getName());
            return existing.stream().limit(limit).map(this::toDTO).toList();
        }

        // No resources yet — generate via AI
        log.info("No resources for skill='{}' — generating via AI", skill.getName());
        List<AIResourceItem> aiItems = aiService.generateResources(skill.getName());

        List<Resource> saved = aiItems.stream().map(item -> {
            Resource.ResourceType type = parseType(item.getType());
            Resource.Difficulty diff   = parseDifficulty(item.getDifficulty());

            Resource resource = Resource.builder()
                    .title(item.getTitle())
                    .link(item.getUrl())
                    .type(type)
                    .skill(skill)
                    .difficulty(diff)
                    .build();
            return resourceRepository.save(resource);
        }).toList();

        log.info("Saved {} AI-generated resources for skill='{}'", saved.size(), skill.getName());

        // Embed in background — don't block the response
        embedResourcesAsync(saved);

        return saved.stream().limit(limit).map(this::toDTO).toList();
    }

    // --------------------------------------------------------
    // Vector similarity search — returns empty on failure, never crashes
    // --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ResourceDTO> findSimilarResources(String query, int limit) {
        log.debug("Similarity search: '{}'", query);
        try {
            float[] embedding      = embed(query);
            String pgVector        = toPgVectorLiteral(embedding);
            List<Resource> results = resourceRepository.findSimilarResources(pgVector, limit);
            log.info("Similarity search returned {} results", results.size());
            return results.stream().map(this::toDTO).toList();
        } catch (Exception e) {
            log.warn("Similarity search failed: {} — returning empty list", e.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<ResourceDTO> findSimilarResourcesForSkill(Long skillId, String query, int limit) {
        skillRepository.findById(skillId)
            .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillId));
        try {
            float[] embedding      = embed(query);
            String pgVector        = toPgVectorLiteral(embedding);
            return resourceRepository
                .findSimilarResourcesBySkill(skillId, pgVector, limit)
                .stream().map(this::toDTO).toList();
        } catch (Exception e) {
            log.warn("Skill similarity search failed: {} — returning empty list", e.getMessage());
            return List.of();
        }
    }

    // --------------------------------------------------------
    // Embed all resources without embeddings
    // --------------------------------------------------------

    @Transactional
    public int embedAllPendingResources() {
        List<Resource> pending = resourceRepository.findByEmbeddingIsNull();
        log.info("Embedding {} resources...", pending.size());
        int count = 0;
        for (Resource resource : pending) {
            try {
                String text = resource.getTitle() + " "
                    + (resource.getSkill() != null ? resource.getSkill().getName() : "")
                    + " " + resource.getType().name()
                    + " " + resource.getDifficulty().name();
                resource.setEmbedding(embed(text));
                resourceRepository.save(resource);
                count++;
            } catch (Exception e) {
                log.error("Failed to embed resource id={}: {}", resource.getId(), e.getMessage());
            }
        }
        log.info("Embedded {} resources", count);
        return count;
    }

    // --------------------------------------------------------
    // Helpers
    // --------------------------------------------------------

    private void embedResourcesAsync(List<Resource> resources) {
        // Embed in a separate thread so the API response isn't delayed
        new Thread(() -> {
            for (Resource r : resources) {
                try {
                    String text = r.getTitle() + " "
                        + (r.getSkill() != null ? r.getSkill().getName() : "")
                        + " " + r.getType().name() + " " + r.getDifficulty().name();
                    r.setEmbedding(embed(text));
                    resourceRepository.save(r);
                } catch (Exception e) {
                    log.warn("Background embed failed for id={}: {}", r.getId(), e.getMessage());
                }
            }
            log.info("Background embedding complete for {} resources", resources.size());
        }, "resource-embedder").start();
    }

    private float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    private String toPgVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private Resource.ResourceType parseType(String type) {
        if (type == null) return Resource.ResourceType.ARTICLE;
        try { return Resource.ResourceType.valueOf(type.toUpperCase()); }
        catch (Exception e) { return Resource.ResourceType.ARTICLE; }
    }

    private Resource.Difficulty parseDifficulty(String diff) {
        if (diff == null) return Resource.Difficulty.BEGINNER;
        try { return Resource.Difficulty.valueOf(diff.toUpperCase()); }
        catch (Exception e) { return Resource.Difficulty.BEGINNER; }
    }

    public ResourceDTO toDTO(Resource r) {
        return ResourceDTO.builder()
                .id(r.getId())
                .title(r.getTitle())
                .link(r.getLink())
                .type(r.getType().name())
                .difficulty(r.getDifficulty().name())
                .skillName(r.getSkill() != null ? r.getSkill().getName() : null)
                .build();
    }
}
