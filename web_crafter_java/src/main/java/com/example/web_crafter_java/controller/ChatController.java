package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.dto.ChatMessage;
import com.example.web_crafter_java.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    // ----------------------------------------------------------
    // 1. 실시간 채팅 처리 (WebSocket)
    // 프론트: stompClient.send("/app/chat/send", {}, payload)
    // ----------------------------------------------------------
    @MessageMapping("/chat/send")
public void sendMessage(ChatMessage message) {

    // ✅ sender / receiver 기준으로 roomId 강제 정렬
    int a = message.getSenderId();
    int b = message.getReceiverId();

    String roomId = Math.min(a, b) + "_" + Math.max(a, b);

    // ✅ message 안에도 다시 넣어줘야 DB에 이 값으로 저장됨
    message.setRoomId(roomId);

    chatService.saveMessage(message);

    // 방 토픽
    messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);

    try {
        String[] ids = roomId.split("_");
        int u1 = Integer.parseInt(ids[0]);
        int u2 = Integer.parseInt(ids[1]);

        int senderId = message.getSenderId();
        int receiverId = (u1 == senderId) ? u2 : u1;

        // ✅ Principal 필요없는 "유저별 토픽"으로 전송
        messagingTemplate.convertAndSend("/topic/user/" + receiverId + "/chat", message);
        messagingTemplate.convertAndSend("/topic/user/" + senderId + "/chat", message);

        // (선택) 빨간점 알림도 유지
        messagingTemplate.convertAndSend("/topic/notifications/" + receiverId, message);

    } catch (Exception e) {
        System.out.println("채팅 전송 실패: " + e.getMessage());
    }
}


    // ----------------------------------------------------------
    // 2. 과거 내역 조회 API (HTTP)
    // ----------------------------------------------------------
    @GetMapping("/api/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String roomId) {
        return chatService.getChatHistory(roomId);
    }
}
