package com.aigoalplanner.controller;

import com.aigoalplanner.dto.ProgressResponse;
import com.aigoalplanner.dto.TaskDTO;
import com.aigoalplanner.dto.TaskUpdateRequest;
import com.aigoalplanner.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProgressController {

    private static final Logger log = LoggerFactory.getLogger(ProgressController.class);
    private final ProgressService progressService;

    @PutMapping("/tasks/{taskId}/complete")
    public ResponseEntity<TaskDTO> updateTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        log.info("PUT /api/tasks/{}/complete — completed={}", taskId, request.getCompleted());
        return ResponseEntity.ok(progressService.updateTaskStatus(taskId, request.getCompleted()));
    }

    @GetMapping("/roadmaps/{roadmapId}/progress")
    public ResponseEntity<ProgressResponse> getProgress(@PathVariable Long roadmapId) {
        return ResponseEntity.ok(progressService.getProgress(roadmapId));
    }

    @PostMapping("/roadmaps/{roadmapId}/recalculate")
    public ResponseEntity<ProgressResponse> recalculateRoadmap(@PathVariable Long roadmapId) {
        log.info("POST /api/roadmaps/{}/recalculate", roadmapId);
        ProgressResponse result = progressService.recalculateRoadmap(roadmapId);
        return ResponseEntity.ok(result);
    }
}