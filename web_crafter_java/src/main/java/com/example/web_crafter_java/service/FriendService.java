package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.FriendDao;
import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate; // ✅ 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FriendService {
    
    private final FriendDao friendDao;
    private final SimpMessagingTemplate messagingTemplate; // ✅ 실시간 알림용 템플릿 주입
    // 🔥 온라인 유저 메모리 저장소
    private final Set<Integer> onlineUsers = ConcurrentHashMap.newKeySet();


    // 유저 검색
    public List<Member> searchUser(String keyword, Integer myId) {
        return friendDao.searchUser(keyword, myId);
    }
    
    // 내 친구 목록
    public List<Member> getFriendList(Integer myId) {
    List<Member> list = friendDao.getMyFriends(myId);

    if (list != null) {
        for (Member m : list) {
            Integer fid = m.getId();
            m.setConnectStatus(isOnline(fid) ? "online" : "offline"); // ✅ 핵심
        }
    }

    return list;
}

    // 친구 요청 + 알림 전송
    @Transactional
    public void sendRequest(Integer myId, Integer targetId) {
        // 1. 이미 친구인지 확인
        if (friendDao.existsRelation(myId, targetId) > 0) {
            throw new RuntimeException("이미 요청했거나 친구 관계입니다.");
        }

        // 2. 친구 테이블에 '대기(PENDING)' 상태로 저장
        friendDao.insertFriendRequest(myId, targetId);

        // 3. 상대방 알림함에 '초대장' 넣기
        Notification noti = new Notification();
        noti.setReceiverId(targetId);
        noti.setSenderId(myId);
        noti.setType("FRIEND_REQ"); 
        friendDao.insertNotification(noti);

        // 🚀 [실시간 추가] 상대방에게 알림 갱신 신호 보냄
        updateRealtimeNotifications(targetId);
    }

    // 알림 목록 가져오기
    public List<Notification> getNotificationList(Integer myId) {
        return friendDao.getMyNotifications(myId);
    }

    // 요청 수락
    @Transactional
    public void acceptRequest(Integer myId, Integer senderId, Integer notiId) {
        friendDao.acceptFriendRequest(senderId, myId);
        friendDao.deleteNotification(notiId); // 알림 삭제

        // 🚀 [실시간 추가] 수락 후 내 알림창 갱신
        updateRealtimeNotifications(myId);
    }

    // 요청 거절
    @Transactional
    public void rejectRequest(Integer myId, Integer senderId, Integer notiId) {
        friendDao.rejectFriendRequest(senderId, myId);
        friendDao.deleteNotification(notiId); // 알림 삭제

        // 🚀 [실시간 추가] 거절 후 내 알림창 갱신
        updateRealtimeNotifications(myId);
    }

    @Transactional
    public void deleteFriend(Integer myId, Integer friendId) {
        friendDao.deleteFriendship(myId, friendId);
        friendDao.deleteFriendship(friendId, myId);
    }

    /**
     * 🚀 공통 메서드: 특정 사용자에게 실시간으로 알림 목록 전송
     */
    private void updateRealtimeNotifications(Integer userId) {
        // 1. 해당 유저의 최신 알림 리스트를 DB에서 조회
        List<Notification> updatedList = friendDao.getMyNotifications(userId);
        
        // 2. [수정] 명시적인 Object 캐스팅으로 Null Type Safety 경고 해결
        // messagingTemplate은 내부적으로 @NonNull Object를 기대하기 때문입니다.
        Object payload = (updatedList != null) ? updatedList : java.util.Collections.emptyList();
        
        // WebSocket 경로 전송
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", payload);
    }

    public void setOnline(Integer userId) {
        if (userId != null) onlineUsers.add(userId);
    }

    public void setOffline(Integer userId) {
        if (userId != null) onlineUsers.remove(userId);
    }

    public boolean isOnline(Integer userId) {
        return userId != null && onlineUsers.contains(userId);
    }

    public List<Integer> getFriendIds(Integer myId) {
    return friendDao.getFriendIds(myId);
    }
}