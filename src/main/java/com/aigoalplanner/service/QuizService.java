package com.aigoalplanner.service;

import com.aigoalplanner.dto.*;
import com.aigoalplanner.exception.GlobalExceptionHandler.AIServiceException;
import com.aigoalplanner.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.aigoalplanner.model.*;
import com.aigoalplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final AIRecommendationService aiService;

    // --------------------------------------------------------
    // Get quiz — always generate fresh quiz
    // --------------------------------------------------------

    @Transactional
    public QuizDTO generateFreshQuiz(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        // Check if all subtasks are done first
        long total     = subtaskRepository.countByTaskId(taskId);
        long completed = subtaskRepository.countByTaskIdAndCompleted(taskId, true);

        if (total > 0 && completed < total) {
            throw new IllegalArgumentException(
                "Complete all subtasks before taking the quiz. " +
                completed + "/" + total + " subtasks done.");
        }

        // Generate via AI — always create a new quiz
        log.info("Generating fresh quiz for taskId={} description={}", taskId, task.getDescription());
        String skillName = task.getSkill() != null ? task.getSkill().getName() : null;

        List<AIQuizItem> aiItems = aiService.generateQuiz(task.getDescription(), skillName);
        if (aiItems == null || aiItems.isEmpty()) {
            throw new AIServiceException("AI returned empty quiz", null);
        }

        // Delete old quizzes for this task to keep DB clean
        quizRepository.deleteByTaskId(taskId);

        // Persist new quiz + questions
        Quiz quiz = Quiz.builder().task(task).build();
        Quiz savedQuiz = quizRepository.save(quiz);

        for (AIQuizItem item : aiItems) {
            QuizQuestion q = QuizQuestion.builder()
                    .quiz(savedQuiz)
                    .question(item.getQuestion())
                    .optionA(item.getOptionA())
                    .optionB(item.getOptionB())
                    .optionC(item.getOptionC())
                    .optionD(item.getOptionD())
                    .correctAnswer(item.getCorrectAnswer().toUpperCase())
                    .explanation(item.getExplanation())
                    .orderIndex(item.getOrder())
                    .build();
            questionRepository.save(q);
        }

        Quiz fullQuiz = quizRepository.findByTaskIdWithQuestions(taskId).orElseThrow();
        log.info("Saved fresh quiz with {} questions for taskId={}", aiItems.size(), taskId);
        return toDTO(fullQuiz, task);
    }

    // --------------------------------------------------------
    // Get last submitted quiz for retake (without regenerating)
    // --------------------------------------------------------

    @Transactional(readOnly = true)
    public QuizDTO getLastQuiz(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        Quiz quiz = quizRepository.findByTaskIdWithQuestions(taskId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No quiz found for task: " + taskId + ". Generate one first."));

        return toDTO(quiz, task);
    }

    // --------------------------------------------------------
    // Submit answers and get result
    // --------------------------------------------------------

    @Transactional(readOnly = true)
    public QuizResultResponse submitQuiz(Long taskId, Map<Long, String> answers) {
        Quiz quiz = quizRepository.findByTaskIdWithQuestions(taskId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Quiz not found for task: " + taskId));

        List<QuizQuestion> questions = quiz.getQuestions();
        List<QuizResultResponse.QuestionResult> results = new ArrayList<>();
        int correct = 0;

        for (QuizQuestion q : questions) {
            String userAnswer = answers.getOrDefault(q.getId(), "").toUpperCase();
            boolean isCorrect = q.getCorrectAnswer().equals(userAnswer);
            if (isCorrect) correct++;

            results.add(QuizResultResponse.QuestionResult.builder()
                    .questionId(q.getId())
                    .question(q.getQuestion())
                    .yourAnswer(userAnswer.isEmpty() ? "Not answered" : userAnswer)
                    .correctAnswer(q.getCorrectAnswer())
                    .explanation(q.getExplanation())
                    .correct(isCorrect)
                    .build());
        }

        int score = questions.isEmpty() ? 0 :
            (int) Math.round((correct * 100.0) / questions.size());
        boolean passed = score >= 70;

        log.info("Quiz submitted for taskId={} score={}% passed={}", taskId, score, passed);

        return QuizResultResponse.builder()
                .totalQuestions(questions.size())
                .correctAnswers(correct)
                .score(score)
                .passed(passed)
                .results(results)
                .build();
    }

    // --------------------------------------------------------
    // Helper
    // --------------------------------------------------------

    private QuizDTO toDTO(Quiz quiz, Task task) {
        List<QuizQuestionDTO> questionDTOs = quiz.getQuestions().stream()
            .map(q -> QuizQuestionDTO.builder()
                    .id(q.getId())
                    .question(q.getQuestion())
                    .optionA(q.getOptionA())
                    .optionB(q.getOptionB())
                    .optionC(q.getOptionC())
                    .optionD(q.getOptionD())
                    .orderIndex(q.getOrderIndex())
                    // correctAnswer intentionally NOT included
                    .build())
            .toList();

        return QuizDTO.builder()
                .id(quiz.getId())
                .taskId(task.getId())
                .taskDescription(task.getDescription())
                .questions(questionDTOs)
                .totalQuestions(questionDTOs.size())
                .build();
    }
}
