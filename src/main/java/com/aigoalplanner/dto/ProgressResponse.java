package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgressResponse {
    private Long roadmapId;
    private Long userId;
    private String goalDescription;
    private int totalTasks;
    private int completedTasks;
    private int remainingTasks;
    private double completionPercentage;
    private List<TaskDTO> completedTaskList;
    private List<TaskDTO> pendingTaskList;
}
