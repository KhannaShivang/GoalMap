package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuizDTO {
    private Long id;
    private Long taskId;
    private String taskDescription;
    private List<QuizQuestionDTO> questions;
    private int totalQuestions;
}
