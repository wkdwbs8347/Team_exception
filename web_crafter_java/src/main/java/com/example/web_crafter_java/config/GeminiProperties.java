package com.example.web_crafter_java.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data; // 롬복 사용 시

@Data // Getter, Setter 자동 생성 (롬복 없으면 직접 getter/setter 만드세요)
@Component
@ConfigurationProperties(prefix = "gemini.api") // yml의 gemini.api 밑에 있는 걸 가져옴
public class GeminiProperties {
    private String key;
    private String url;
}