package com.aigoalplanner.service;

import com.aigoalplanner.dto.AITaskItem;
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

    // --------------------------------------------------------
    // FEATURE 1: Generate initial roadmap
    // --------------------------------------------------------

    public List<AITaskItem> generateRoadmap(String userProfile, String currentSkills,
                                             String goalDescription, int targetMonths) {

        // BeanOutputConverter tells Spring AI exactly what shape to return
        // and automatically appends format instructions to the prompt
        BeanOutputConverter<List<AITaskItem>> converter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<AITaskItem>>() {});

        String prompt = buildRoadmapPrompt(userProfile, currentSkills, goalDescription,
                                            targetMonths, converter.getFormat());

        log.debug("Sending roadmap generation prompt to Ollama...");

        try {
            String rawResponse = chatClient.prompt().user(prompt).call().content();
            log.debug("Raw AI response length={}", rawResponse.length());
            List<AITaskItem> items = converter.convert(rawResponse);
            log.info("AI generated {} roadmap tasks", items.size());
            return items;
        } catch (Exception e) {
            throw new AIServiceException("Roadmap generation failed: " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------
    // FEATURE 3: Adaptive next steps after progress
    // --------------------------------------------------------

    public List<AITaskItem> generateNextSteps(String goalDescription,
                                               List<String> completedTaskDescriptions,
                                               int remainingMonths) {

        BeanOutputConverter<List<AITaskItem>> converter =
            new BeanOutputConverter<>(new ParameterizedTypeReference<List<AITaskItem>>() {});

        String prompt = buildAdaptivePrompt(goalDescription, completedTaskDescriptions,
                                             remainingMonths, converter.getFormat());

        log.debug("Sending adaptive prompt to Ollama...");

        try {
            String rawResponse = chatClient.prompt().user(prompt).call().content();
            List<AITaskItem> items = converter.convert(rawResponse);
            log.info("AI generated {} next-step tasks", items.size());
            return items;
        } catch (Exception e) {
            throw new AIServiceException("Next steps generation failed: " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------
    // Prompt builders
    // --------------------------------------------------------

    private String buildRoadmapPrompt(String userProfile, String currentSkills,
                                       String goalDescription, int targetMonths,
                                       String outputFormat) {
        return """
                You are an expert career and learning roadmap planner.

                USER PROFILE: %s
                CURRENT SKILLS: %s
                GOAL: %s
                TARGET TIMELINE: %d months

                Generate exactly 10 structured learning steps ordered from foundational to advanced.
                Each step must have a "step" number, a short "description" under 100 characters,
                and a "skillName" that is a real specific technology or concept.

                %s
                """.formatted(userProfile, currentSkills, goalDescription, targetMonths, outputFormat);
    }

    private String buildAdaptivePrompt(String goalDescription,
                                        List<String> completedTasks,
                                        int remainingMonths,
                                        String outputFormat) {
        String completedList = String.join("\n", completedTasks.stream().map(t -> "- " + t).toList());
        return """
                You are an expert career and learning roadmap planner.

                GOAL: %s
                COMPLETED TASKS:
                %s
                REMAINING TIME: %d months

                Based on what the user has already completed, generate the next 5 learning steps.
                Do NOT repeat any completed tasks.
                Each step needs a "step" number, "description" under 100 chars, and "skillName".

                %s
                """.formatted(goalDescription, completedList, remainingMonths, outputFormat);
    }
}