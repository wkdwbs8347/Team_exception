package com.example.web_crafter_java.config;

import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.web_crafter_java.service.FriendService;

@Component
public class PresenceEventListener {

    private final FriendService friendService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceEventListener(FriendService friendService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.friendService = friendService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        Integer userId = getOrStoreUserId(acc);   // ✅ 변경
        if (userId == null) return;

        // ✅ online 등록
        friendService.setOnline(userId);

        // ✅ 내 친구들에게 내가 online 됐다고 방송
        List<Integer> friendIds = friendService.getFriendIds(userId);
        for (Integer fid : friendIds) {
            messagingTemplate.convertAndSend(
                "/topic/user/" + fid + "/presence",
                Map.of("userId", userId, "status", "online")
            );
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        Integer userId = getOrStoreUserId(acc);   // ✅ 변경 (disconnect에서 session으로 읽힘)
        if (userId == null) return;

        // ✅ offline 등록
        friendService.setOffline(userId);

        // ✅ 내 친구들에게 내가 offline 됐다고 방송
        List<Integer> friendIds = friendService.getFriendIds(userId);
        for (Integer fid : friendIds) {
            messagingTemplate.convertAndSend(
                "/topic/user/" + fid + "/presence",
                Map.of("userId", userId, "status", "offline")
            );
        }
    }

    // ✅ 핵심: 1) 헤더(x-user-id)에서 먼저 찾고 2) 세션에 저장해두고 3) 다음엔 세션에서 꺼냄
    private Integer getOrStoreUserId(StompHeaderAccessor acc) {
        // 1) 세션에 이미 저장돼 있으면 그걸 사용
        if (acc.getSessionAttributes() != null) {
            Object v = acc.getSessionAttributes().get("userId");

            Integer fromSession = toInt(v);
            if (fromSession != null) return fromSession;
        }

        // 2) 프론트가 connect 헤더로 보내는 값 읽기
        String header = acc.getFirstNativeHeader("x-user-id");
        Integer fromHeader = toInt(header);
        if (fromHeader == null) return null;

        // 3) disconnect에서도 쓰게 세션에 저장
        if (acc.getSessionAttributes() != null) {
            acc.getSessionAttributes().put("userId", fromHeader);
        }

        return fromHeader;
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); }
            catch (Exception e) { return null; }
        }
        return null;
    }
}
