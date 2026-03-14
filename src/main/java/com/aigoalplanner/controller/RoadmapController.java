package com.aigoalplanner.controller;

import com.aigoalplanner.dto.RoadmapGenerateRequest;
import com.aigoalplanner.dto.RoadmapResponse;
import com.aigoalplanner.service.RoadmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

    private static final Logger log = LoggerFactory.getLogger(RoadmapController.class);
    private final RoadmapService roadmapService;

    @PostMapping("/generate")
    public ResponseEntity<RoadmapResponse> generateRoadmap(
            @Valid @RequestBody RoadmapGenerateRequest request) {
        log.info("POST /api/roadmaps/generate — userId={} goalId={}",
                request.getUserId(), request.getGoalId());
        RoadmapResponse response = roadmapService.generateRoadmap(
                request.getUserId(), request.getGoalId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoadmapResponse> getRoadmap(@PathVariable Long id) {
        return ResponseEntity.ok(roadmapService.getRoadmapById(id));
    }

    @GetMapping
    public ResponseEntity<List<RoadmapResponse>> getRoadmapsByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(roadmapService.getRoadmapsByUser(userId));
    }
}