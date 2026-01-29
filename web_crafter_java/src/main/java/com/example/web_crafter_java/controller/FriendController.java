package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.Notification; // ✅ DTO import 필수!
import com.example.web_crafter_java.service.FriendService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // ✅ [수정] 세션에서 '로그인한 ID(숫자)'만 딱 꺼내는 메소드
    private Integer getLoginId(HttpSession session) {
        Integer memberId = (Integer) session.getAttribute("loginedMemberId");
        if (memberId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return memberId;
    }

    // 1. 내 친구 목록 조회
    @GetMapping("/list")
    public List<Member> getMyFriends(HttpSession session) {
        return friendService.getFriendList(getLoginId(session));
    }

    // 2. 유저 검색
    @GetMapping("/search")
    public List<Member> searchUser(@RequestParam("keyword") String keyword, HttpSession session) {
        return friendService.searchUser(keyword, getLoginId(session));
    }

    // 3. 친구 요청 보내기
    @PostMapping("/request")
    public String sendRequest(@RequestBody Map<String, Integer> body, HttpSession session) {
        Integer targetId = body.get("targetId");
        friendService.sendRequest(getLoginId(session), targetId);
        return "요청 전송 완료";
    }

    // 4. 알림 목록 조회 API (🔥 수정됨: Map -> Notification)
    @GetMapping("/notifications")
    public List<Notification> getNotifications(HttpSession session) {
        return friendService.getNotificationList(getLoginId(session));
    }

    // 5. 친구 수락 API
    @PostMapping("/accept")
    public String acceptFriend(@RequestBody Map<String, Integer> body, HttpSession session) {
        Integer myId = getLoginId(session);
        Integer senderId = body.get("senderId");
        Integer notiId = body.get("notiId");

        friendService.acceptRequest(myId, senderId, notiId);
        return "수락되었습니다.";
    }

    // 6. 친구 거절 API
    @PostMapping("/reject")
    public String rejectFriend(@RequestBody Map<String, Integer> body, HttpSession session) {
        friendService.rejectRequest(getLoginId(session), body.get("senderId"), body.get("notiId"));
        return "거절되었습니다.";
    }

    @DeleteMapping("/{friendId}")
    public String deleteFriend(@PathVariable("friendId") Integer friendId, HttpSession session) {
        Integer myId = getLoginId(session);
        friendService.deleteFriend(myId, friendId);
        return "삭제되었습니다.";
    }
}