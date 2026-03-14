package com.aigoalplanner.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GoalRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "Goal description is required")
    @Size(min = 10, max = 2000, message = "Goal description must be between 10 and 2000 characters")
    private String goalDescription;

    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 24, message = "Duration cannot exceed 24 months")
    private Integer targetDurationMonths = 12;
}
