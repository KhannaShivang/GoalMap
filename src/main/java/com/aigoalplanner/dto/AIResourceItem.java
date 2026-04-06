package com.aigoalplanner.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResourceItem {
    private String title;
    private String url;
    private String type;        // ARTICLE, COURSE, VIDEO, DOCUMENTATION, PROJECT
    private String difficulty;  // BEGINNER, INTERMEDIATE, ADVANCED
    private String description;
}
