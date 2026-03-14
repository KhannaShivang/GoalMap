package com.aigoalplanner.service;

import com.aigoalplanner.dto.ResourceDTO;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.Resource;
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

    @Transactional(readOnly = true)
    public List<ResourceDTO> getResourcesForSkill(Long skillId, int limit) {
        skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillId));
        List<Resource> resources = resourceRepository.findBySkillId(skillId);
        if (resources.isEmpty()) log.warn("No resources found for skillId={}", skillId);
        return resources.stream().limit(limit).map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceDTO> findSimilarResources(String query, int limit) {
        log.debug("Running similarity search for query: {}", query);
        float[] queryEmbedding = embed(query);
        String pgVectorLiteral = toPgVectorLiteral(queryEmbedding);
        List<Resource> results = resourceRepository.findSimilarResources(pgVectorLiteral, limit);
        log.info("Similarity search returned {} resources", results.size());
        return results.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceDTO> findSimilarResourcesForSkill(Long skillId, String query, int limit) {
        skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillId));
        float[] queryEmbedding = embed(query);
        String pgVectorLiteral = toPgVectorLiteral(queryEmbedding);
        List<Resource> results = resourceRepository.findSimilarResourcesBySkill(skillId, pgVectorLiteral, limit);
        return results.stream().map(this::toDTO).toList();
    }

    @Transactional
    public int embedAllPendingResources() {
        List<Resource> pending = resourceRepository.findByEmbeddingIsNull();
        log.info("Embedding {} resources without embeddings...", pending.size());
        int count = 0;
        for (Resource resource : pending) {
            try {
                String text = resource.getTitle() + " "
                        + (resource.getSkill() != null ? resource.getSkill().getName() : "")
                        + " " + resource.getType().name() + " " + resource.getDifficulty().name();
                resource.setEmbedding(embed(text));
                resourceRepository.save(resource);
                count++;
            } catch (Exception e) {
                log.error("Failed to embed resource id={}: {}", resource.getId(), e.getMessage());
            }
        }
        log.info("Successfully embedded {} resources", count);
        return count;
    }

    private float[] embed(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            throw new RuntimeException("Embedding call failed. Is Ollama running with nomic-embed-text?", e);
        }
    }

    private String toPgVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private ResourceDTO toDTO(Resource r) {
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
