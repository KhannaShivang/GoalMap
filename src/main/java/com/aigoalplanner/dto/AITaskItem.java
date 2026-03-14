package com.aigoalplanner.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AITaskItem {
    private int step;
    private String description;
    private String skillName;
}
