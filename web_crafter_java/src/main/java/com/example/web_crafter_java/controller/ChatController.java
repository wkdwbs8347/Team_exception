package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.dto.ChatMessage;
import com.example.web_crafter_java.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessage message) {

        // ✅ roomId 강제 정렬
        int a = message.getSenderId();
        int b = message.getReceiverId();
        String roomId = Math.min(a, b) + "_" + Math.max(a, b);
        message.setRoomId(roomId);

        // ✅ 프론트 즉시 렌더용 regDate 세팅(웹소켓으로 나갈 때 null 방지)
        if (message.getRegDate() == null) {
            message.setRegDate(LocalDateTime.now());
        }

        // DB 저장 (saveMessage 후 message.id 채워짐)
        chatService.saveMessage(message);

        // ✅ 채팅 메시지는 "유저별 채널" 1개로만 전송 (중복 방지 핵심)
        int senderId = message.getSenderId();
        int receiverId = (Math.min(a, b) == senderId) ? Math.max(a, b) : Math.min(a, b);

        messagingTemplate.convertAndSend("/topic/user/" + receiverId + "/chat", message);
        messagingTemplate.convertAndSend("/topic/user/" + senderId + "/chat", message);

        // ✅ 빨간점/알림은 별도로(여기서는 채팅 저장 금지)
        messagingTemplate.convertAndSend("/topic/notifications/" + receiverId, 
            java.util.Map.of("senderId", senderId, "roomId", roomId)
        );
    }

    @GetMapping("/api/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String roomId) {
        // roomId 들어오는게 3_2여도 2_3로 강제
        try {
            String[] sp = roomId.split("_");
            int a = Integer.parseInt(sp[0]);
            int b = Integer.parseInt(sp[1]);
            roomId = Math.min(a, b) + "_" + Math.max(a, b);
        } catch (Exception ignore) {}

        return chatService.getChatHistory(roomId);
    }
    
}
