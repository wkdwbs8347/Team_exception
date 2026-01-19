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

@Service
public class AiService {

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;

    public AiService(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
        this.restTemplate = new RestTemplate();
    }

    // 🔥 메인 메서드: 모드에 따라 프롬프트 전략 변경
    public Map<String, String> generateResponse(String userPrompt, String mode) {
        String systemInstruction;
        
        if ("chat".equals(mode)) {
            // [대화 모드] 친절한 멘토 역할
            systemInstruction = "당신은 Web Crafter의 친절한 코딩 멘토입니다. 사용자의 웹 개발 질문에 한국어로 친절하게 답변해주세요. 코드를 직접 짜주기보다는 개념을 설명해주세요. XML은 생성하지 마세요.";
        } else {
            // [생성 모드] 블록 XML 생성기 역할 (파일에서 읽어옴)
            systemInstruction = getSystemPromptFromFile();
        }

        String finalPrompt = systemInstruction + "\n\nUser Request: " + userPrompt;
        String aiResponse = callGeminiApi(finalPrompt);

        // 결과 반환 맵 생성
        Map<String, String> result = new HashMap<>();
        
        if ("chat".equals(mode)) {
            result.put("message", aiResponse); // 채팅 응답
        } else {
            // XML만 깔끔하게 추출
            String cleanXml = cleanXml(aiResponse);
            result.put("xml", cleanXml);
            result.put("message", "요청하신 기능을 블록으로 생성했습니다.");
        }
        
        return result;
    }

    // Gemini API 호출 로직 (공통)
    private String callGeminiApi(String prompt) {
        String requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiProperties.getKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Request Body 구성
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

    // 마크다운 제거
    private String cleanXml(String text) {
        if (text == null) return "";
        int start = text.indexOf("<xml");
        int end = text.lastIndexOf("</xml>");
        if (start != -1 && end != -1) {
            return text.substring(start, end + 6);
        }
        return text.replace("```xml", "").replace("```", "").trim();
    }

    // 시스템 프롬프트 파일 읽기
    private String getSystemPromptFromFile() {
        try {
            ClassPathResource resource = new ClassPathResource("system-prompt.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "당신은 Web Crafter AI입니다. Google Blockly XML을 생성하세요.";
        }
    }
}