package com.aigoalplanner.dto;

import com.aigoalplanner.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String currentSkills;
    private User.ExperienceLevel experienceLevel;
    private LocalDateTime createdAt;
}
