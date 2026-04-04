package com.aigoalplanner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class QuizSubmitRequest {
    // Map of questionId → answer (A, B, C, or D)
    @NotNull(message = "Answers are required")
    private Map<Long, String> answers;
}
