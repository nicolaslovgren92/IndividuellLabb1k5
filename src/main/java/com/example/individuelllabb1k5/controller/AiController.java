package com.example.individuelllabb1k5.controller;


import com.example.individuelllabb1k5.dto.AiResponseDto;
import com.example.individuelllabb1k5.service.AiClientService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiClientService aiClientService;
    private final Validator validator;

    @Autowired
    public AiController(AiClientService aiClientService, Validator validator) {
        this.aiClientService = aiClientService;
        this.validator = validator;
    }

    @PostMapping
    public AiResponseDto analyzeGame(@RequestBody String gameName) {
        AiResponseDto result = aiClientService.analyzeGame(gameName);

        Set<ConstraintViolation<AiResponseDto>> violations = validator.validate(result);
        if (!violations.isEmpty()) {
            System.out.println("WARN: AI response failed validation: " + violations);
            AiResponseDto fallback = new AiResponseDto();
            fallback.setGameName(gameName);
            fallback.setGood(List.of("N/A"));
            fallback.setBad(List.of("N/A"));
            fallback.setScore(0);
            fallback.setSummary("AI response failed validation rules.");
            return fallback;
        }

        return result;
    }
}
