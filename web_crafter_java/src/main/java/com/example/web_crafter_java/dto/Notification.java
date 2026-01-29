package com.example.web_crafter_java.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {
    private Integer id;
    private Integer receiverId; // 받는 사람
    private Integer senderId;   // 보낸 사람
    private String type;        // FRIEND_REQ (친구 요청), PROJECT_INVITE (프로젝트 초대)
    private Integer relId;      // 관련 ID (프로젝트 ID 등)
    private Boolean isRead;
    private LocalDateTime regDate;
    private String senderName;
}