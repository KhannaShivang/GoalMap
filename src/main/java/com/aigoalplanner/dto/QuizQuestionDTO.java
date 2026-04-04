package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizQuestionDTO {
    private Long id;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer orderIndex;
    // correctAnswer is NOT included here — only sent in QuizResultResponse
}
