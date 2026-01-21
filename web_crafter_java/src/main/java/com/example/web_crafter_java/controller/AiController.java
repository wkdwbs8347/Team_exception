package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173") 
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate")
    // 🔥 [핵심 수정] String, String이 아니라 Object로 받아야 Boolean과 Map을 소화합니다.
    public Map<String, String> generate(@RequestBody Map<String, Object> request) {
        
        // 1. 안전하게 데이터 꺼내기
        String prompt = (String) request.get("prompt");
        String mode = (String) request.getOrDefault("mode", "gen");
        
        // 2. Boolean 처리 (형변환 안전장치)
        Object isEditObj = request.get("isEditMode");
        Boolean isEditMode = false;
        if (isEditObj instanceof Boolean) {
            isEditMode = (Boolean) isEditObj;
        }

        // 3. Context(Map) 처리 (형변환 안전장치)
        Map<String, String> context = null;
        if (request.get("context") instanceof Map) {
            context = (Map<String, String>) request.get("context");
        }

        System.out.println("🔍 요청 도착: " + prompt);
        System.out.println("👉 모드: " + mode + " / 수정여부: " + isEditMode);

        // 4. 서비스로 넘기기
        return aiService.generateResponse(prompt, mode, isEditMode, context);
    }
}