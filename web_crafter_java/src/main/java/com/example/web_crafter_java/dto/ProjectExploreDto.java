package com.example.web_crafter_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectExploreDto {
    private Long id;
    private String title;
    private String ownerNickname;
    private LocalDateTime updateDate;
    private List<String> techTags;
    private int likes;
    private int views;
    
    // 🔥 [추가] DB에서 가져온 코드를 담을 필드
    private String htmlContent; // layoutData
    private String cssContent;  // styleData
}