package com.example.web_crafter_java.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserWeb {
    private Integer id;
    private Integer userId;
    private String title;
    private String thumbnailUrl;
    private int hit;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
}