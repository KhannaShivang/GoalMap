package com.aigoalplanner.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SubtaskDTO {
    private Long id;
    private String description;
    private Integer orderIndex;
    private Boolean completed;
    private LocalDateTime completedAt;
}
