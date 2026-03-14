package com.aigoalplanner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoadmapGenerateRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "goalId is required")
    private Long goalId;
}