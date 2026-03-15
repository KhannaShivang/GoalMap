package com.aigoalplanner.dto;

import com.aigoalplanner.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {

    // used for signup only
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // used for signup only
    private String currentSkills;

    // used for signup only
    private User.ExperienceLevel experienceLevel;
}
