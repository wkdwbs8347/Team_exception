package com.example.web_crafter_java.service;

import com.example.web_crafter_java.config.GeminiProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class AiService {

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;

    public AiService(GeminiProperties geminiProperties) {
            this.geminiProperties = geminiProperties;
            
            // [수정] 타임아웃 설정 추가 (이거 없으면 무한 대기함)
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);  // 연결 시도: 5초 지나면 에러 뱉고 끊기
            factory.setReadTimeout(60000);    // 응답 대기: 60초 지나면 에러 뱉고 끊기 (AI는 생각하는 시간이 깁니다)
            
            this.restTemplate = new RestTemplate(factory);
        }

    // [메인 메서드]
    public Map<String, String> generateResponse(String userPrompt, String mode, Boolean isEditMode, Map<String, String> context) {
        
        // 1. 시스템 프롬프트 설정
        String systemInstruction;
        if ("chat".equals(mode)) {
            systemInstruction = "당신은 Web Crafter의 친절한 코딩 멘토입니다. 한국어로 답변하세요.";
        } else {
            systemInstruction = getSystemPromptFromFile();
        }

        // 2. 최종 프롬프트 조립
        StringBuilder finalPrompt = new StringBuilder();
        finalPrompt.append(systemInstruction).append("\n\n");

        // ✅ 수정 모드일 때만 기존 코드를 프롬프트에 추가
        if (Boolean.TRUE.equals(isEditMode) && context != null) {
            finalPrompt.append("--- [CURRENT XML CONTEXT (DO NOT REMOVE IDs)] ---\n");
            finalPrompt.append("Structure XML: ").append(context.getOrDefault("structure", "")).append("\n");
            finalPrompt.append("Style XML: ").append(context.getOrDefault("style", "")).append("\n");
            finalPrompt.append("Logic XML: ").append(context.getOrDefault("logic", "")).append("\n");
            finalPrompt.append("-----------------------------\n");
            finalPrompt.append("위의 XML 코드를 바탕으로, 사용자의 요청('").append(userPrompt).append("')에 맞춰 수정된 XML만 출력하세요.\n");
        } else {
            finalPrompt.append("User Request: ").append(userPrompt);
        }

        // 3. API 호출
        System.out.println("🚀 Gemini API 호출 중...");
        String aiResponse = callGeminiApi(finalPrompt.toString());

        // 4. 결과 반환
        Map<String, String> result = new HashMap<>();
        if ("chat".equals(mode)) {
            result.put("message", aiResponse);
        } else {
            String cleanXml = cleanXml(aiResponse);
            System.out.println("🤖 생성된 XML 길이: " + cleanXml.length()); // 로그 확인용
            result.put("xml", cleanXml);
            result.put("message", isEditMode ? "수정이 완료되었습니다." : "생성이 완료되었습니다.");
        }
        
        return result;
    }
    // Gemini API 호출 로직
    private String callGeminiApi(String prompt) {
        // 현재 사용 중인 모델명으로 URL 설정 (gemini-1.5-flash 권장)
        String requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiProperties.getKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Request Body 구성 (Gemini 규격)
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();

        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        // 답변 짤림 방지 및 정확도 설정
        Map<String, Object> config = new HashMap<>();
        config.put("maxOutputTokens", 8192);
        config.put("temperature", 0.2);
        requestBody.put("generationConfig", config);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(requestUrl, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> resContent = (Map<String, Object>) firstCandidate.get("content");
                    List<Map<String, Object>> resParts = (List<Map<String, Object>>) resContent.get("parts");
                    return (String) resParts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "오류가 발생했습니다: " + e.getMessage();
        }
        return "";
    }

    private String cleanXml(String text) {
        if (text == null) return "";
        int start = text.indexOf("<xml");
        int end = text.lastIndexOf("</xml>");
        if (start != -1 && end != -1) {
            return text.substring(start, end + 6);
        }
        return text.replace("```xml", "").replace("```", "").trim();
    }

    private String getSystemPromptFromFile() {
        try {
            ClassPathResource resource = new ClassPathResource("system-prompt.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "당신은 Web Crafter AI입니다. Google Blockly XML을 생성하세요.";
        }
    }
}