package com.aigoalplanner.service;

import com.aigoalplanner.dto.GoalRequest;
import com.aigoalplanner.dto.GoalResponse;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.Goal;
import com.aigoalplanner.model.User;
import com.aigoalplanner.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);
    private final GoalRepository goalRepository;
    private final UserService userService;

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        User user = userService.findUserOrThrow(request.getUserId());
        Goal goal = Goal.builder()
                .user(user)
                .goalDescription(request.getGoalDescription())
                .targetDurationMonths(request.getTargetDurationMonths())
                .status(Goal.GoalStatus.ACTIVE)
                .build();
        Goal saved = goalRepository.save(goal);
        log.info("Created goal id={} for userId={}", saved.getId(), user.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id) {
        return toResponse(findGoalOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsByUser(Long userId) {
        userService.findUserOrThrow(userId);
        List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<GoalResponse> responses = new java.util.ArrayList<>();
        for (int i = 0; i < goals.size(); i++) {
            responses.add(toResponse(goals.get(i), i + 1));
        }
        return responses;
    }

    @Transactional
    public GoalResponse updateGoalStatus(Long id, Goal.GoalStatus status) {
        Goal goal = findGoalOrThrow(id);
        goal.setStatus(status);
        return toResponse(goalRepository.save(goal));
    }

    public Goal findGoalOrThrow(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id));
    }

    private GoalResponse toResponse(Goal goal) {
        return toResponse(goal, 0);
    }

    private GoalResponse toResponse(Goal goal, int userGoalNumber) {
        return GoalResponse.builder()
                .id(goal.getId())
                .userId(goal.getUser().getId())
                .userGoalNumber(userGoalNumber)
                .goalDescription(goal.getGoalDescription())
                .targetDurationMonths(goal.getTargetDurationMonths())
                .status(goal.getStatus().name())
                .createdAt(goal.getCreatedAt())
                .build();
    }
}