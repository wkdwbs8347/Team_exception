package com.example.web_crafter_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectExploreDto {
    private Long id;
    private String title;
    private String ownerNickname;
    private LocalDateTime updateDate;
    private int views;
    
    // 🔥 [수정] DB의 'preview_html' 컬럼과 매핑될 필드
    // 더 이상 htmlContent(페이지조인)가 아니라, 프로젝트 자체의 썸네일 HTML입니다.
    private String previewHtml; 
}