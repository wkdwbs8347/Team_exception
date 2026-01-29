package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.FriendDao;
import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.Notification; // ✅ DTO import 확인
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FriendService {
    
    private final FriendDao friendDao;

    // 유저 검색
    public List<Member> searchUser(String keyword, Integer myId) {
        return friendDao.searchUser(keyword, myId);
    }
    
    // 내 친구 목록
    public List<Member> getFriendList(Integer myId) {
        return friendDao.getMyFriends(myId);
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
        noti.setType("FRIEND_REQ"); // 알림 타입
        // 친구 요청은 relId가 딱히 필요 없으므로 null 또는 0
        friendDao.insertNotification(noti);
    }

    // 🔥 [수정] 알림 목록 가져오기 (Map -> Notification DTO로 변경)
    // DAO에서 DTO로 받기로 했으므로, 여기서도 DTO로 받아야 합니다.
    public List<Notification> getNotificationList(Integer myId) {
        return friendDao.getMyNotifications(myId);
    }

    // 요청 수락
    @Transactional
    public void acceptRequest(Integer myId, Integer senderId, Integer notiId) {
        friendDao.acceptFriendRequest(senderId, myId);
        friendDao.deleteNotification(notiId); // 알림 삭제
    }

    // 요청 거절
    @Transactional
    public void rejectRequest(Integer myId, Integer senderId, Integer notiId) {
        friendDao.rejectFriendRequest(senderId, myId);
        friendDao.deleteNotification(notiId); // 알림 삭제
    }

    @Transactional
    public void deleteFriend(Integer myId, Integer friendId) {
        // 친구 관계는 양방향이므로 안전하게 둘 다 삭제 시도
        friendDao.deleteFriendship(myId, friendId);
        friendDao.deleteFriendship(friendId, myId);
    }
}