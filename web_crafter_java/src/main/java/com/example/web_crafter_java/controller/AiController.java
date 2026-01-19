package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173") // Vue 프론트 주소 허용
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String mode = request.getOrDefault("mode", "gen"); // 기본값은 생성 모드

        System.out.println("🔍 요청 모드: " + mode + ", 내용: " + prompt);

        return aiService.generateResponse(prompt, mode);
    }
}