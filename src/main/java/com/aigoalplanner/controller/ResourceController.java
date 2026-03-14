package com.aigoalplanner.controller;

import com.aigoalplanner.dto.ResourceDTO;
import com.aigoalplanner.service.ResourceRecommendationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);
    private final ResourceRecommendationService resourceService;

    @GetMapping("/{skillId}")
    public ResponseEntity<List<ResourceDTO>> getResourcesBySkill(
            @PathVariable Long skillId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(resourceService.getResourcesForSkill(skillId, limit));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceDTO>> searchResources(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Vector search — query='{}' limit={}", query, limit);
        return ResponseEntity.ok(resourceService.findSimilarResources(query, limit));
    }

    @GetMapping("/{skillId}/search")
    public ResponseEntity<List<ResourceDTO>> searchResourcesBySkill(
            @PathVariable Long skillId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(resourceService.findSimilarResourcesForSkill(skillId, query, limit));
    }

    @PostMapping("/embed")
    public ResponseEntity<Map<String, Object>> embedResources() {
        log.info("POST /api/resources/embed — starting batch embedding");
        int count = resourceService.embedAllPendingResources();
        return ResponseEntity.ok(Map.of("message", "Embedding complete", "resourcesEmbedded", count));
    }
}
