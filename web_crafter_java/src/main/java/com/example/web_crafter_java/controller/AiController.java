package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173") 
public class AiController {

    // 서비스 주입
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateBlocklyXml(@RequestBody Map<String, String> request) {
        String userPrompt = request.get("prompt");
        
        // 🔥 진짜 AI 호출!
        String generatedXml = aiService.getBlocklyXml(userPrompt);

        Map<String, String> response = new HashMap<>();
        response.put("xml", generatedXml);

        return ResponseEntity.ok(response);
    }
}