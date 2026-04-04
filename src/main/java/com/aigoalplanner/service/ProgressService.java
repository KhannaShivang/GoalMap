package com.aigoalplanner.service;

import com.aigoalplanner.dto.ProgressResponse;
import com.aigoalplanner.dto.TaskDTO;
import com.aigoalplanner.dto.AITaskItem;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.Roadmap;
import com.aigoalplanner.model.Task;
import com.aigoalplanner.model.Skill;
import com.aigoalplanner.repository.RoadmapRepository;
import com.aigoalplanner.repository.TaskRepository;
import com.aigoalplanner.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressService.class);

    private final TaskRepository taskRepository;
    private final RoadmapRepository roadmapRepository;
    private final SkillRepository skillRepository;
    private final AIRecommendationService aiService;

    @Transactional
    public TaskDTO updateTaskStatus(Long taskId, Boolean completed) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        task.setCompleted(completed);
        task.setCompletedAt(completed ? LocalDateTime.now() : null);
        taskRepository.save(task);
        log.info("Task id={} marked completed={}", taskId, completed);
        return toTaskDTO(task);
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
            .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found: " + roadmapId));

        List<Task> allTasks  = taskRepository.findByRoadmapIdWithSkill(roadmapId);
        List<Task> completed = allTasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).toList();
        List<Task> pending   = allTasks.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())).toList();

        double pct = allTasks.isEmpty() ? 0.0
            : Math.round((completed.size() * 100.0 / allTasks.size()) * 10) / 10.0;

        return ProgressResponse.builder()
                .roadmapId(roadmapId)
                .userId(roadmap.getUser().getId())
                .goalDescription(roadmap.getGoal().getGoalDescription())
                .totalTasks(allTasks.size())
                .completedTasks(completed.size())
                .remainingTasks(pending.size())
                .completionPercentage(pct)
                .completedTaskList(completed.stream().map(this::toTaskDTO).toList())
                .pendingTaskList(pending.stream().map(this::toTaskDTO).toList())
                .build();
    }

    @Transactional
    public ProgressResponse recalculateRoadmap(Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
            .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found: " + roadmapId));

        List<Task> allTasks  = taskRepository.findByRoadmapIdWithSkill(roadmapId);
        List<Task> completed = allTasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).toList();
        List<Task> pending   = allTasks.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())).toList();

        if (completed.isEmpty()) {
            throw new IllegalArgumentException("Complete at least one task first.");
        }

        boolean shouldRecalculate = completed.size() >= 3 || pending.isEmpty();
        if (!shouldRecalculate) {
            log.info("Not enough progress to recalculate (completed={})", completed.size());
            return getProgress(roadmapId);
        }

        log.info("Recalculating roadmap id={} — {} tasks completed", roadmapId, completed.size());

        List<String> completedDescriptions = completed.stream().map(Task::getDescription).toList();
        int remainingMonths = roadmap.getGoal().getTargetDurationMonths();

        List<AITaskItem> newSteps = aiService.generateNextSteps(
            roadmap.getGoal().getGoalDescription(), completedDescriptions, remainingMonths);

        int maxPriority = allTasks.stream().mapToInt(Task::getPriority).max().orElse(0);

        for (AITaskItem item : newSteps) {
            boolean duplicate = allTasks.stream()
                .anyMatch(t -> t.getDescription().equalsIgnoreCase(item.getDescription()));
            if (duplicate) continue;

            Task task = Task.builder()
                    .roadmap(roadmap)
                    .skill(resolveSkill(item.getSkillName()))
                    .description(item.getDescription())
                    .priority(++maxPriority)
                    .completed(false)
                    .build();
            taskRepository.save(task);
        }

        log.info("Added {} new tasks to roadmap id={}", newSteps.size(), roadmapId);
        return getProgress(roadmapId);
    }

    private Skill resolveSkill(String skillName) {
        if (skillName == null || skillName.isBlank()) return null;
        Optional<Skill> existing = skillRepository.findByNameIgnoreCase(skillName.trim());
        if (existing.isPresent()) return existing.get();
        return skillRepository.save(Skill.builder()
                .name(skillName.trim())
                .description("Auto-created from AI")
                .category("General")
                .build());
    }

    private TaskDTO toTaskDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .description(task.getDescription())
                .priority(task.getPriority())
                .completed(task.getCompleted())
                .completedAt(task.getCompletedAt())
                .skillName(task.getSkill() != null ? task.getSkill().getName() : null)
                .resources(List.of())
                .build();
    }
}
