package com.aigoalplanner.controller;

import com.aigoalplanner.dto.SubtaskDTO;
import com.aigoalplanner.service.SubtaskService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubtaskController {

    private static final Logger log = LoggerFactory.getLogger(SubtaskController.class);
    private final SubtaskService subtaskService;

    /**
     * GET /api/tasks/{taskId}/subtasks
     * Returns subtasks for a task.
     * If subtasks don't exist yet, calls Ollama to generate them (30-60s first time).
     */
    @GetMapping("/tasks/{taskId}/subtasks")
    public ResponseEntity<List<SubtaskDTO>> getSubtasks(@PathVariable Long taskId) {
        log.info("GET /api/tasks/{}/subtasks", taskId);
        return ResponseEntity.ok(subtaskService.getOrGenerateSubtasks(taskId));
    }

    /**
     * PUT /api/subtasks/{subtaskId}/complete
     * Mark a subtask complete or incomplete.
     *
     * Body: { "completed": true }
     */
    @PutMapping("/subtasks/{subtaskId}/complete")
    public ResponseEntity<SubtaskDTO> completeSubtask(
            @PathVariable Long subtaskId,
            @RequestBody Map<String, Boolean> body) {
        Boolean completed = body.get("completed");
        if (completed == null) {
            throw new IllegalArgumentException("'completed' field is required");
        }
        log.info("PUT /api/subtasks/{}/complete — completed={}", subtaskId, completed);
        return ResponseEntity.ok(subtaskService.updateSubtaskStatus(subtaskId, completed));
    }

    /**
     * GET /api/tasks/{taskId}/subtasks/status
     * Returns whether all subtasks for a task are completed.
     * Used by frontend to decide whether to show the quiz button.
     */
    @GetMapping("/tasks/{taskId}/subtasks/status")
    public ResponseEntity<Map<String, Object>> getSubtaskStatus(@PathVariable Long taskId) {
        boolean allDone = subtaskService.allSubtasksCompleted(taskId);
        return ResponseEntity.ok(Map.of(
            "taskId", taskId,
            "allCompleted", allDone
        ));
    }
}
