package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceDTO {
    private Long id;
    private String title;
    private String link;
    private String type;
    private String difficulty;
    private String skillName;
}
