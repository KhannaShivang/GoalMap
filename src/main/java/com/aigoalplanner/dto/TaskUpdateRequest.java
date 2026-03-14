package com.aigoalplanner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskUpdateRequest {

    @NotNull(message = "completed field is required")
    private Boolean completed;
}
