package com.example.web_crafter_java.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Integer id;
    private String roomId;
    private Integer senderId;
    private Integer receiverId;
    private String content;
    private LocalDateTime regDate;
}
