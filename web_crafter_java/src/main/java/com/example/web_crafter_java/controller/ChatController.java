package com.example.web_crafter_java.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.web_crafter_java.dto.ChatMessage;
import com.example.web_crafter_java.service.ChatService;

import java.util.Map;

@Controller
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ChatService chatService; // 서비스 주입

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessage message) {
        // 1. DB에 메시지 저장
        chatService.saveMessage(message);
        
        // 2. 실시간 전송
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
    }
}