package com.example.web_crafter_java.service; // ⚠️ 본인의 프로젝트 패키지명과 일치하는지 꼭 확인하세요!

// 👇 아까 만드신 Config 클래스 import (경로가 다르면 수정 필수)
import com.example.web_crafter_java.config.GeminiProperties;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource; // ClassPathResource
import java.nio.file.Files;                           // Files
import java.io.IOException;                           // IOException
import java.nio.charset.StandardCharsets;            // StandardCharsets
@Service
public class AiService {

    private final GeminiProperties geminiProperties;
    private final RestTemplate restTemplate;

    // 생성자 주입 (@Autowired 생략 가능)
    public AiService(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
        this.restTemplate = new RestTemplate();
    }

    public String getBlocklyXml(String userPrompt) {
        String requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiProperties.getKey();
        // 2. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. 요청 본문(Body) 만들기 (Gemini 전용 구조: contents -> parts -> text)
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();

        // 시스템 프롬프트 + 사용자 요청 결합
        String finalPrompt = getSystemPrompt() + "\n\nUser Request: " + userPrompt;

        // [🔍 디버깅 로그 1] 우리가 보내는 질문 확인
        System.out.println("\n========== [1. AI 요청 프롬프트] ==========");
        System.out.println(finalPrompt);
        System.out.println("===========================================");

        part.put("text", finalPrompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        // 4. API 호출
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(requestUrl, entity, Map.class);

            // [🔍 디버깅 로그 2] Gemini가 준 원본 응답 확인
            System.out.println("\n========== [2. Gemini 원본 응답] ==========");
            System.out.println(response.getBody());
            System.out.println("===========================================");

            // 5. 응답 파싱
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("candidates")) {
                return "<xml></xml>";
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> resContent = (Map<String, Object>) candidate.get("content");
                List<Map<String, Object>> resParts = (List<Map<String, Object>>) resContent.get("parts");
                
                // 텍스트 추출
                String text = (String) resParts.get(0).get("text");

                // 마크다운 제거
                String resultXml = cleanXml(text);

                // [🔍 디버깅 로그 3] 최종 추출된 XML
                System.out.println("\n========== [3. 최종 XML 결과] ==========");
                System.out.println(resultXml);
                System.out.println("========================================\n");

                return resultXml;
            }
            return "<xml></xml>";

        } catch (Exception e) {
            // [🔍 디버깅 로그 4] 에러 발생 시
            System.err.println("\n!!!! Gemini API 호출 중 오류 발생 !!!!");
            e.printStackTrace();
            return "<xml></xml>"; 
        }
    }

    // 🧹 마크다운 코드블록 제거 함수
    private String cleanXml(String text) {
        if (text == null) return "";
        return text.replace("```xml", "")
                   .replace("```", "")
                   .trim();
    }

    // 🧠 시스템 프롬프트 (여기를 잘 작성해야 블록이 잘 나옵니다)
    private String getSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("system-prompt.txt");
            // getFile() 대신 getInputStream() 사용
            byte[] content = resource.getInputStream().readAllBytes();
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 🚨 파일 읽기 실패 시 여기로 떨어집니다.
            System.err.println("시스템 프롬프트 파일 읽기 실패 (기본값 사용): " + e.getMessage());
            
            // 파일이 없을 때 대신 사용할 기본 프롬프트를 여기에 적으세요.
            return """
                당신은 'Web Crafter'의 AI 어시스턴트입니다.
                사용자의 요구사항을 Google Blockly XML 코드로 변환하세요.
                [규칙]
                1. 결과는 오직 <xml> 태그로 시작하고 끝나야 합니다.
                """;
        }
    }
}