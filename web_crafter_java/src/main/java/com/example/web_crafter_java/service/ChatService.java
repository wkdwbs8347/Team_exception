package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.ChatDao;
import com.example.web_crafter_java.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatService {
    @Autowired
    private ChatDao chatDao;

    public void saveMessage(ChatMessage message) {
        chatDao.saveMessage(message);
    }

    public List<ChatMessage> getChatHistory(String roomId) {
        return chatDao.getMessagesByRoomId(roomId);
    }
}