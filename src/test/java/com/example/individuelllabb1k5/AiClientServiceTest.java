package com.example.individuelllabb1k5;

import com.example.individuelllabb1k5.dto.AiResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.profiles.active=test")
public class AiClientServiceTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void Setup() {
        objectMapper = new ObjectMapper();
    }


    @Test
    void parseResponse_validJson_shouldReturnAiResponseDto() throws Exception {
        String jsonResponse = """
                {
                  "gameName": "The Legend of Zelda: Breath of the Wild",
                  "good": ["Open-world exploration", "Innovative gameplay mechanics", "Stunning visuals"],
                  "bad": ["Weapon durability system can be frustrating", "Some side quests feel repetitive"],
                  "score": 97,
                  "summary": "A groundbreaking open-world adventure that redefines the Zelda franchise."
                }
                """;

        AiResponseDto responseDto = objectMapper.readValue(jsonResponse, AiResponseDto.class);

        assert responseDto.getGameName().equals("The Legend of Zelda: Breath of the Wild");
        assert responseDto.getGood().size() == 3;
        assert responseDto.getBad().size() == 2;
        assert responseDto.getScore() == 97;
        assert responseDto.getSummary().equals("A groundbreaking open-world adventure that redefines the Zelda franchise.");
    }

    @Test
    void parseResponse_invalidJson_shouldThrowException() {
        String invalidJsonResponse = """
                {
                  "gameName": "The Legend of Zelda: Breath of the Wild",
                  "good": ["Open-world exploration", "Innovative gameplay mechanics", "Stunning visuals"],
                  "bad": ["Weapon durability system can be frustrating", "Some side quests feel repetitive"],
                  "score": 97,
                  "summary": "A groundbreaking open-world adventure that redefines the Zelda franchise."
                """;

        try {
            objectMapper.readValue(invalidJsonResponse, AiResponseDto.class);
            assert false; // Should not reach here
        } catch (Exception e) {
            assert true; // Exception is expected
        }
    }

}
