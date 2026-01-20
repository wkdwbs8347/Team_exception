package com.example.web_crafter_java.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 마이페이지 통계 전용 DTO
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberStats {
    // 기본 정보 (SQL에서 조회하는 것들)
    private String nickname;
    private String bio;

    // 통계 정보 (SQL의 alias 이름과 일치해야 함)
    private int myProjectCount;      // 내 프로젝트 수
    private int sharedProjectCount;  // 공유받은 프로젝트 수
    private int unreadNotiCount;     // 읽지 않은 알림 개수
}