package com.aigoalplanner.dto;

import com.aigoalplanner.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    private String currentSkills;

    private User.ExperienceLevel experienceLevel = User.ExperienceLevel.BEGINNER;
}
