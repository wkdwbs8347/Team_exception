package com.example.web_crafter_java.dto; // 패키지명은 본인 프로젝트에 맞게

import lombok.Data;

@Data
public class StatsDto {
    private long userCount;      // 가입 유저 수
    private long projectCount;   // 생성된 프로젝트 수
    private long totalViews;     // 누적 조회수 (hit 합계)
}