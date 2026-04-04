package com.aigoalplanner.service;

import com.aigoalplanner.dto.AITaskItem;
import com.aigoalplanner.dto.AISubtaskItem;
import com.aigoalplanner.dto.AIQuizItem;
import com.aigoalplanner.exception.GlobalExceptionHandler.AIServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AIRecommendationService.class);
    private final ChatClient chatClient;

    // ============================================================
    // FEATURE 1: Generate roadmap tasks
    // ============================================================

    public List<AITaskItem> generateRoadmap(String userProfile, String currentSkills,
                                             String goalDescription, int targetMonths) {
        BeanOutputConverter<List<AITaskItem>> converter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<AITaskItem>>() {});

        String prompt = """
                You are an expert career and learning roadmap planner.

                USER PROFILE: %s
                CURRENT SKILLS: %s
                GOAL: %s
                TARGET TIMELINE: %d months

                Generate exactly 10 structured learning steps ordered from foundational to advanced.
                Each step must have a "step" number, a short "description" under 100 characters,
                and a "skillName" that is a real specific technology or concept.

                %s
                """.formatted(userProfile, currentSkills, goalDescription,
                               targetMonths, converter.getFormat());

        log.debug("Generating roadmap tasks...");
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            List<AITaskItem> items = converter.convert(raw);
            log.info("Generated {} roadmap tasks", items.size());
            return items;
        } catch (Exception e) {
            throw new AIServiceException("Roadmap generation failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // FEATURE 3: Adaptive next steps
    // ============================================================

    public List<AITaskItem> generateNextSteps(String goalDescription,
                                               List<String> completedTaskDescriptions,
                                               int remainingMonths) {
        BeanOutputConverter<List<AITaskItem>> converter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<AITaskItem>>() {});

        String completedList = String.join("\n",
            completedTaskDescriptions.stream().map(t -> "- " + t).toList());

        String prompt = """
                You are an expert career and learning roadmap planner.

                GOAL: %s
                COMPLETED TASKS:
                %s
                REMAINING TIME: %d months

                Based on what the user has already completed, generate the next 5 learning steps.
                Do NOT repeat any completed tasks.
                Each step needs a "step" number, "description" under 100 chars, and "skillName".

                %s
                """.formatted(goalDescription, completedList, remainingMonths,
                               converter.getFormat());

        log.debug("Generating next steps...");
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            List<AITaskItem> items = converter.convert(raw);
            log.info("Generated {} next steps", items.size());
            return items;
        } catch (Exception e) {
            throw new AIServiceException("Next steps generation failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // FEATURE: Generate subtasks for a task (on demand)
    // ============================================================

    public List<AISubtaskItem> generateSubtasks(String taskDescription, String skillName) {
        BeanOutputConverter<List<AISubtaskItem>> converter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<AISubtaskItem>>() {});

        String prompt = """
                You are an expert programming and technology teacher.

                TASK TO LEARN: %s
                SKILL: %s

                Break this learning task into 4 to 6 specific, actionable subtasks.
                Each subtask should be a concrete thing to learn or practice.

                Examples of good subtasks for "Learn conditional statements in Java":
                - Understand if and else syntax
                - Learn else-if chaining for multiple conditions
                - Practice nested if statements
                - Learn the ternary operator shorthand
                - Understand switch statements and when to use them

                Each subtask must have an "order" number (starting at 1) and a
                "description" that is clear and specific, under 80 characters.

                %s
                """.formatted(taskDescription, skillName != null ? skillName : "",
                               converter.getFormat());

        log.debug("Generating subtasks for task: {}", taskDescription);
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            List<AISubtaskItem> items = converter.convert(raw);
            log.info("Generated {} subtasks for: {}", items.size(), taskDescription);
            return items;
        } catch (Exception e) {
            throw new AIServiceException("Subtask generation failed: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // FEATURE: Generate quiz for a task (on demand)
    // ============================================================

    public List<AIQuizItem> generateQuiz(String taskDescription, String skillName) {
        BeanOutputConverter<List<AIQuizItem>> converter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<AIQuizItem>>() {});

        String prompt = """
                You are an expert programming teacher creating a high-quality quiz.

                TOPIC: %s
                SKILL: %s

                Create 8 to 15 multiple choice questions appropriate for the topic complexity.
                - Simple/fundamental topics: 8-10 questions
                - Moderate topics: 10-12 questions
                - Complex/advanced topics: 12-15 questions

                STRUCTURE REQUIREMENTS:
                - The "question" field must contain ONLY the question text itself
                - DO NOT include options (A/B/C/D) as part of the question text
                - Each option (optionA, optionB, optionC, optionD) must be a separate field
                - Options should be plausible — avoid obviously wrong answers

                QUESTION QUALITY RULES:
                - First 30%%: Easy — test basic concepts and definitions
                - Middle 40%%: Medium — test application and understanding
                - Last 30%%: Hard — test edge cases, debugging, or complex scenarios
                - Use real-world scenarios and code snippets where relevant
                - Include common misconceptions as distractor options
                - Each explanation must teach something — explain WHY the answer is correct

                OUTPUT FORMAT:
                - correctAnswer must be exactly one of: "A", "B", "C", or "D" (uppercase)
                - explanation should be 1-3 sentences explaining the reasoning
                - order must be a number starting at 1

                EXAMPLE OF GOOD QUESTION FORMAT:
                {
                  "order": 1,
                  "question": "What is the time complexity of binary search?",
                  "optionA": "O(n)",
                  "optionB": "O(log n)",
                  "optionC": "O(n log n)",
                  "optionD": "O(1)",
                  "correctAnswer": "B",
                  "explanation": "Binary search divides the search space in half each iteration, giving O(log n) complexity."
                }

                %s
                """.formatted(taskDescription, skillName != null ? skillName : "",
                               converter.getFormat());

        log.debug("Generating quiz for task: {}", taskDescription);
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            List<AIQuizItem> items = converter.convert(raw);
            if (items == null || items.isEmpty()) {
                throw new AIServiceException("AI returned empty quiz", null);
            }
            log.info("Generated {} quiz questions for: {}", items.size(), taskDescription);
            return items;
        } catch (Exception e) {
            throw new AIServiceException("Quiz generation failed: " + e.getMessage(), e);
        }
    }
}
