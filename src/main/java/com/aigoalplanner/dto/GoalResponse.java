package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GoalResponse {
    private Long id;
    private Long userId;
    private int userGoalNumber;   // 1st goal for this user, 2nd goal, etc.
    private String goalDescription;
    private Integer targetDurationMonths;
    private String status;
    private LocalDateTime createdAt;
}