package com.aigoalplanner.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AISubtaskItem {
    private int order;
    private String description;
}
