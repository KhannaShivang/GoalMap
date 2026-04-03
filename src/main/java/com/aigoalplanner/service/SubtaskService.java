package com.aigoalplanner.service;

import com.aigoalplanner.dto.AISubtaskItem;
import com.aigoalplanner.dto.SubtaskDTO;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.Subtask;
import com.aigoalplanner.model.Task;
import com.aigoalplanner.repository.SubtaskRepository;
import com.aigoalplanner.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubtaskService {

    private static final Logger log = LoggerFactory.getLogger(SubtaskService.class);

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;
    private final AIRecommendationService aiService;

    // --------------------------------------------------------
    // Get subtasks — generate if not exists
    // --------------------------------------------------------

    @Transactional
    public List<SubtaskDTO> getOrGenerateSubtasks(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        // If subtasks already exist, return them
        if (subtaskRepository.existsByTaskId(taskId)) {
            log.debug("Returning existing subtasks for taskId={}", taskId);
            return subtaskRepository.findByTaskIdOrderByOrderIndexAsc(taskId)
                    .stream().map(this::toDTO).toList();
        }

        // Generate via AI
        log.info("Generating subtasks for taskId={} description={}",
                 taskId, task.getDescription());

        String skillName = task.getSkill() != null ? task.getSkill().getName() : null;
        List<AISubtaskItem> aiItems = aiService.generateSubtasks(
                task.getDescription(), skillName);

        // Persist
        List<Subtask> saved = aiItems.stream().map(item -> {
            Subtask subtask = Subtask.builder()
                    .task(task)
                    .description(item.getDescription())
                    .orderIndex(item.getOrder())
                    .completed(false)
                    .build();
            return subtaskRepository.save(subtask);
        }).toList();

        log.info("Saved {} subtasks for taskId={}", saved.size(), taskId);
        return saved.stream().map(this::toDTO).toList();
    }

    // --------------------------------------------------------
    // Mark subtask complete / incomplete
    // --------------------------------------------------------

    @Transactional
    public SubtaskDTO updateSubtaskStatus(Long subtaskId, Boolean completed) {
        Subtask subtask = subtaskRepository.findById(subtaskId)
            .orElseThrow(() -> new ResourceNotFoundException("Subtask not found: " + subtaskId));

        subtask.setCompleted(completed);
        subtask.setCompletedAt(completed ? LocalDateTime.now() : null);
        subtaskRepository.save(subtask);

        log.info("Subtask id={} marked completed={}", subtaskId, completed);
        return toDTO(subtask);
    }

    // --------------------------------------------------------
    // Check if all subtasks for a task are complete
    // --------------------------------------------------------

    @Transactional(readOnly = true)
    public boolean allSubtasksCompleted(Long taskId) {
        long total     = subtaskRepository.countByTaskId(taskId);
        long completed = subtaskRepository.countByTaskIdAndCompleted(taskId, true);
        return total > 0 && total == completed;
    }

    // --------------------------------------------------------
    // Helper
    // --------------------------------------------------------

    private SubtaskDTO toDTO(Subtask s) {
        return SubtaskDTO.builder()
                .id(s.getId())
                .description(s.getDescription())
                .orderIndex(s.getOrderIndex())
                .completed(s.getCompleted())
                .completedAt(s.getCompletedAt())
                .build();
    }
}
