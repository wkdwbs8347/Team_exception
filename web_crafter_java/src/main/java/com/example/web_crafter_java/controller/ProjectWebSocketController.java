package com.example.web_crafter_java.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload; // Payload 어노테이션 추가 권장
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class ProjectWebSocketController {

    // 클라이언트가 보낼 주소: /app/project/{webId}/block-move
    @MessageMapping("/project/{webId}/block-move")
    // 구독자에게 보낼 주소: /topic/project/{webId}/block-updates
    @SendTo("/topic/project/{webId}/block-updates")
    public Map<String, Object> handleBlockMove(
            @DestinationVariable String webId,
            @Payload Map<String, Object> moveData // @Payload 명시 (안전성 확보)
    ) {
        // 🚀 서버 콘솔에 이 로그가 찍히면 통신 성공입니다!
        System.out.println("⚡ [WebSocket] WebID: " + webId + " | Payload: " + moveData);
        
        return moveData;
    }
}