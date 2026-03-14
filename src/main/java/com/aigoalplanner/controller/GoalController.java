package com.aigoalplanner.controller;

import com.aigoalplanner.dto.GoalRequest;
import com.aigoalplanner.dto.GoalResponse;
import com.aigoalplanner.model.Goal;
import com.aigoalplanner.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private static final Logger log = LoggerFactory.getLogger(GoalController.class);
    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        log.info("POST /api/goals — userId={}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.createGoal(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoal(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getGoalById(id));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getGoalsByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(goalService.getGoalsByUser(userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<GoalResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Goal.GoalStatus status) {
        return ResponseEntity.ok(goalService.updateGoalStatus(id, status));
    }
}
