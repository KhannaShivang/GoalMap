package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuizResultResponse {
    private int totalQuestions;
    private int correctAnswers;
    private int score;           // percentage 0-100
    private boolean passed;      // true if score >= 60
    private List<QuestionResult> results;

    @Data
    @Builder
    public static class QuestionResult {
        private Long questionId;
        private String question;
        private String yourAnswer;
        private String correctAnswer;
        private String explanation;
        private boolean correct;
    }
}
