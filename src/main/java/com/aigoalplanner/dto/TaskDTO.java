package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskDTO {
    private Long id;
    private String description;
    private Integer priority;
    private Boolean completed;
    private LocalDateTime completedAt;
    private String skillName;
    private List<ResourceDTO> resources;
}
