package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoadmapResponse {
    private Long id;
    private Long goalId;
    private Long userId;
    private String goalDescription;
    private List<TaskDTO> tasks;
    private int totalTasks;
    private int completedTasks;
    private LocalDateTime createdAt;
}
