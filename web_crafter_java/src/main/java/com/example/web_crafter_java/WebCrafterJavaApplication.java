package com.example.web_crafter_java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.web_crafter_java") // 👈 프로젝트 전체를 강제로 훑게 만듭니다. [cite: 2026-01-19]
public class WebCrafterJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebCrafterJavaApplication.class, args);
    }
}