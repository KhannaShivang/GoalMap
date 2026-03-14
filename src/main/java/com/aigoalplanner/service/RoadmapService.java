package com.aigoalplanner.service;

import com.aigoalplanner.dto.AITaskItem;
import com.aigoalplanner.dto.ResourceDTO;
import com.aigoalplanner.dto.RoadmapResponse;
import com.aigoalplanner.dto.TaskDTO;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.*;
import com.aigoalplanner.repository.RoadmapRepository;
import com.aigoalplanner.repository.SkillRepository;
import com.aigoalplanner.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private static final Logger log = LoggerFactory.getLogger(RoadmapService.class);

    private final RoadmapRepository roadmapRepository;
    private final TaskRepository taskRepository;
    private final SkillRepository skillRepository;
    private final GoalService goalService;
    private final UserService userService;
    private final AIRecommendationService aiService;
    private final ResourceRecommendationService resourceService;

    @Transactional
    public RoadmapResponse generateRoadmap(Long userId, Long goalId) {
        // Validate user exists
        userService.findUserOrThrow(userId);

        // Validate goal exists
        Goal goal = goalService.findGoalOrThrow(goalId);

        // Validate this goal actually belongs to this user
        if (!goal.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                "Goal " + goalId + " does not belong to user " + userId);
        }

        User user = goal.getUser();
        log.info("Generating roadmap for userId={} goalId={}", userId, goalId);

        String userProfile = buildUserProfile(user);
        String currentSkills = user.getCurrentSkills() != null ? user.getCurrentSkills() : "Not specified";

        List<AITaskItem> aiTasks = aiService.generateRoadmap(
                userProfile, currentSkills, goal.getGoalDescription(), goal.getTargetDurationMonths());

        Roadmap roadmap = Roadmap.builder().goal(goal).user(user).build();
        Roadmap saved = roadmapRepository.save(roadmap);

        List<Task> tasks = new ArrayList<>();
        for (AITaskItem item : aiTasks) {
            Skill skill = resolveSkill(item.getSkillName());
            Task task = Task.builder()
                    .roadmap(saved)
                    .skill(skill)
                    .description(item.getDescription())
                    .priority(item.getStep())
                    .completed(false)
                    .build();
            tasks.add(taskRepository.save(task));
        }

        saved.getTasks().addAll(tasks);
        log.info("Roadmap id={} created with {} tasks", saved.getId(), tasks.size());
        return toResponse(saved, tasks);
    }

    @Transactional(readOnly = true)
    public RoadmapResponse getRoadmapById(Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findByIdWithTasks(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found: " + roadmapId));
        List<Task> tasks = taskRepository.findByRoadmapIdWithSkill(roadmapId);
        return toResponse(roadmap, tasks);
    }

    @Transactional(readOnly = true)
    public List<RoadmapResponse> getRoadmapsByUser(Long userId) {
        userService.findUserOrThrow(userId);
        return roadmapRepository.findByUserIdWithTasks(userId).stream().map(r -> {
            List<Task> tasks = taskRepository.findByRoadmapIdWithSkill(r.getId());
            return toResponse(r, tasks);
        }).toList();
    }

    private String buildUserProfile(User user) {
        return String.format("%s experience level, %s",
                user.getExperienceLevel().name().toLowerCase(),
                user.getCurrentSkills() != null ? user.getCurrentSkills() : "background not specified");
    }

    private Skill resolveSkill(String skillName) {
        if (skillName == null || skillName.isBlank()) return null;
        Optional<Skill> existing = skillRepository.findByNameIgnoreCase(skillName.trim());
        if (existing.isPresent()) return existing.get();
        Skill newSkill = Skill.builder()
                .name(skillName.trim())
                .description("Auto-created from AI roadmap generation")
                .category("General")
                .build();
        return skillRepository.save(newSkill);
    }

    private RoadmapResponse toResponse(Roadmap roadmap, List<Task> tasks) {
        List<TaskDTO> taskDTOs = tasks.stream().map(this::toTaskDTO).toList();
        long completedCount = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        return RoadmapResponse.builder()
                .id(roadmap.getId())
                .goalId(roadmap.getGoal().getId())
                .userId(roadmap.getUser().getId())
                .goalDescription(roadmap.getGoal().getGoalDescription())
                .tasks(taskDTOs)
                .totalTasks(tasks.size())
                .completedTasks((int) completedCount)
                .createdAt(roadmap.getCreatedAt())
                .build();
    }

    private TaskDTO toTaskDTO(Task task) {
        List<ResourceDTO> resources = List.of();
        if (task.getSkill() != null) {
            try {
                // First try direct skill ID lookup
                resources = resourceService.getResourcesForSkill(task.getSkill().getId(), 3);

                // If empty, fall back to similarity search using skill name
                if (resources.isEmpty()) {
                    resources = resourceService.findSimilarResources(task.getSkill().getName(), 3);
                }
            } catch (Exception e) {
                log.warn("Could not fetch resources for skill={}: {}", task.getSkill().getName(), e.getMessage());
                // Last resort: search by task description
                try {
                    resources = resourceService.findSimilarResources(task.getDescription(), 3);
                } catch (Exception ignored) {}
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