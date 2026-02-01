package com.example.web_crafter_java.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private int id;
    private String roomId;    // 방 식별자 (예: 1_2)
    private int senderId;     // 보낸 사람 (user.id)
    private String content;   // 메시지 내용
    private String regDate;   // 등록일
}