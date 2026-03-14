package com.aigoalplanner.service;

import com.aigoalplanner.dto.AITaskItem;
import com.aigoalplanner.dto.ProgressResponse;
import com.aigoalplanner.dto.ResourceDTO;
import com.aigoalplanner.dto.TaskDTO;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.Roadmap;
import com.aigoalplanner.model.Task;
import com.aigoalplanner.model.Skill;
import com.aigoalplanner.repository.RoadmapRepository;
import com.aigoalplanner.repository.TaskRepository;
import com.aigoalplanner.repository.SkillRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor

public class ProgressService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProgressService.class);

    private final TaskRepository taskRepository;
    private final RoadmapRepository roadmapRepository;
    private final SkillRepository skillRepository;
    private final AIRecommendationService aiService;
    private final ResourceRecommendationService resourceService;

    // --------------------------------------------------------
    // FEATURE 3: Mark a task complete / incomplete
    // --------------------------------------------------------

    @Transactional
    public TaskDTO updateTaskStatus(Long taskId, boolean completed) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        task.setCompleted(completed);
        task.setCompletedAt(completed ? LocalDateTime.now() : null);
        Task saved = taskRepository.save(task);

        log.info("Task id={} marked completed={}", taskId, completed);
        return toTaskDTO(saved);
    }

    // --------------------------------------------------------
    // FEATURE 3: Get full progress for a roadmap
    // --------------------------------------------------------

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findByIdWithTasks(roadmapId)
            .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found: " + roadmapId));

        List<Task> allTasks = taskRepository.findByRoadmapIdWithSkill(roadmapId);
        List<Task> completed = allTasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).toList();
        List<Task> pending   = allTasks.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())).toList();

        double pct = allTasks.isEmpty() ? 0.0
            : Math.round((completed.size() * 100.0 / allTasks.size()) * 10.0) / 10.0;

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

    // --------------------------------------------------------
    // FEATURE 3: Adaptive recalculation — append AI-generated
    // next steps to an existing roadmap based on completed tasks
    // --------------------------------------------------------

    @Transactional
    public ProgressResponse recalculateRoadmap(Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findByIdWithTasks(roadmapId)
            .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found: " + roadmapId));

        List<Task> allTasks   = taskRepository.findByRoadmapIdWithSkill(roadmapId);
        List<Task> completed  = allTasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).toList();
        List<Task> pending    = allTasks.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())).toList();

        if (completed.isEmpty()) {
            log.info("No completed tasks yet for roadmapId={} — skipping recalculation", roadmapId);
            return getProgress(roadmapId);
        }

        // Only recalculate if user has made significant progress (>= 3 tasks done)
        // or has completed all existing pending tasks
        boolean shouldRecalculate = completed.size() >= 3 || pending.isEmpty();
        if (!shouldRecalculate) {
            log.info("Not enough progress yet to recalculate roadmap (completed={})", completed.size());
            return getProgress(roadmapId);
        }

        log.info("Recalculating roadmap id={} — {} tasks completed", roadmapId, completed.size());

        List<String> completedDescriptions = completed.stream()
                .map(Task::getDescription)
                .toList();

        int remainingMonths = roadmap.getGoal().getTargetDurationMonths();

        // Ask AI for next steps
        List<AITaskItem> newSteps = aiService.generateNextSteps(
            roadmap.getGoal().getGoalDescription(),
            completedDescriptions,
            remainingMonths
        );

        // Find current max priority
        int maxPriority = allTasks.stream()
                .mapToInt(Task::getPriority)
                .max()
                .orElse(0);

        // Persist new tasks
        List<Task> addedTasks = new ArrayList<>();
        for (AITaskItem item : newSteps) {
            // Skip if a task with the same description already exists
            boolean duplicate = allTasks.stream()
                .anyMatch(t -> t.getDescription().equalsIgnoreCase(item.getDescription()));
            if (duplicate) continue;

            Skill skill = resolveSkill(item.getSkillName());
            Task task = Task.builder()
                    .roadmap(roadmap)
                    .skill(skill)
                    .description(item.getDescription())
                    .priority(++maxPriority)
                    .completed(false)
                    .build();
            addedTasks.add(taskRepository.save(task));
        }

        log.info("Added {} new tasks to roadmap id={}", addedTasks.size(), roadmapId);
        return getProgress(roadmapId);
    }

    // --------------------------------------------------------
    // Private helpers
    // --------------------------------------------------------

    private Skill resolveSkill(String skillName) {
        if (skillName == null || skillName.isBlank()) return null;
        Optional<Skill> existing = skillRepository.findByNameIgnoreCase(skillName.trim());
        if (existing.isPresent()) return existing.get();

        Skill newSkill = Skill.builder()
                .name(skillName.trim())
                .description("Auto-created during adaptive recalculation")
                .category("General")
                .build();
        return skillRepository.save(newSkill);
    }

    private TaskDTO toTaskDTO(Task task) {
        List<ResourceDTO> resources = List.of();
        if (task.getSkill() != null) {
            try {
                resources = resourceService.getResourcesForSkill(task.getSkill().getId(), 3);
            } catch (Exception e) {
                log.warn("Could not load resources for skill {}: {}", task.getSkill().getName(), e.getMessage());
            }
        }
        return TaskDTO.builder()
                .id(task.getId())
                .description(task.getDescription())
                .priority(task.getPriority())
                .completed(task.getCompleted())
                .completedAt(task.getCompletedAt())
                .skillName(task.getSkill() != null ? task.getSkill().getName() : null)
                .resources(resources)
                .build();
    }
}
