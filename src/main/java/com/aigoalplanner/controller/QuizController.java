package com.aigoalplanner.controller;

import com.aigoalplanner.dto.QuizDTO;
import com.aigoalplanner.dto.QuizResultResponse;
import com.aigoalplanner.dto.QuizSubmitRequest;
import com.aigoalplanner.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);
    private final QuizService quizService;

    /**
     * GET /api/tasks/{taskId}/quiz
     * Always generates a FRESH quiz via AI.
     * Requires all subtasks completed.
     * Correct answers are NOT included in the response — only sent after submission.
     */
    @GetMapping("/{taskId}/quiz")
    public ResponseEntity<QuizDTO> getQuiz(@PathVariable Long taskId) {
        log.info("GET /api/tasks/{}/quiz (fresh generation)", taskId);
        return ResponseEntity.ok(quizService.generateFreshQuiz(taskId));
    }

    /**
     * GET /api/tasks/{taskId}/quiz/retake
     * Returns the last submitted quiz for re-practice (without regenerating).
     * Use this after taking the quiz to retake the same questions.
     */
    @GetMapping("/{taskId}/quiz/retake")
    public ResponseEntity<QuizDTO> retakeQuiz(@PathVariable Long taskId) {
        log.info("GET /api/tasks/{}/quiz/retake", taskId);
        return ResponseEntity.ok(quizService.getLastQuiz(taskId));
    }

    /**
     * POST /api/tasks/{taskId}/quiz/submit
     * Submit answers and get score + correct answers + explanations.
     *
     * Body:
     * {
     *   "answers": {
     *     "1": "A",
     *     "2": "C",
     *     "3": "B",
     *     "4": "D",
     *     "5": "A"
     *   }
     * }
     * Keys are question IDs, values are "A", "B", "C", or "D".
     */
    @PostMapping("/{taskId}/quiz/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable Long taskId,
            @Valid @RequestBody QuizSubmitRequest request) {
        log.info("POST /api/tasks/{}/quiz/submit", taskId);
        return ResponseEntity.ok(quizService.submitQuiz(taskId, request.getAnswers()));
    }
}
