package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.FriendDao;
import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendDao friendDao;
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<Integer> onlineUsers = ConcurrentHashMap.newKeySet();
    private final ChatService chatService;

    public List<Member> searchUser(String keyword, Integer myId) {
        return friendDao.searchUser(keyword, myId);
    }

    public List<Member> getFriendList(Integer myId) {
        List<Member> list = friendDao.getMyFriends(myId);

        if (list != null) {
            for (Member m : list) {
                Integer fid = m.getId();
                m.setConnectStatus(isOnline(fid) ? "online" : "offline");
            }
        }
        return list;
    }

    @Transactional
    public void sendRequest(Integer myId, Integer targetId) {
        if (friendDao.existsRelation(myId, targetId) > 0) {
            throw new RuntimeException("이미 요청했거나 친구 관계입니다.");
        }

        friendDao.insertFriendRequest(myId, targetId);

        Notification noti = new Notification();
        noti.setReceiverId(targetId);
        noti.setSenderId(myId);
        noti.setType("FRIEND_REQ");
        friendDao.insertNotification(noti);

        // 알림 갱신
        pushNotiRefresh(targetId);
    }

    public List<Notification> getNotificationList(Integer myId) {
        return friendDao.getMyNotifications(myId);
    }

    @Transactional
    public void acceptRequest(Integer myId, Integer senderId, Integer notiId) {
        friendDao.acceptFriendRequest(senderId, myId);
        friendDao.deleteNotification(notiId);

        // ✅ 친구목록 갱신 (양쪽 다)
        pushFriendsRefresh(myId);
        pushFriendsRefresh(senderId);

        // ✅ 알림 갱신 (내 것만)
        pushNotiRefresh(myId);
    }

    @Transactional
    public void rejectRequest(Integer myId, Integer senderId, Integer notiId) {
        friendDao.rejectFriendRequest(senderId, myId);
        friendDao.deleteNotification(notiId);

        // 알림 갱신
        pushNotiRefresh(myId);
    }

    @Transactional
    public void deleteFriend(Integer myId, Integer friendId) {

        // ✅ 1) 친구 관계 삭제 (OR 쿼리라 1번이면 끝)
        friendDao.deleteFriendship(myId, friendId);

        // ✅ 2) roomId 만들기
        int a = Math.min(myId, friendId);
        int b = Math.max(myId, friendId);
        String roomId = a + "_" + b;

        // ✅ 3) 채팅 내역 삭제
        chatService.deleteChatRoom(roomId);

        // ✅ 4) ✅ 친구목록 갱신 신호 (양쪽)
        pushFriendsRefresh(myId);
        pushFriendsRefresh(friendId);

        // (선택) 알림도 갱신
        pushNotiRefresh(myId);
        pushNotiRefresh(friendId);
    }

    // ✅ 알림 리스트 갱신
    private void pushNotiRefresh(Integer userId) {
        List<Notification> updatedList = friendDao.getMyNotifications(userId);
        Object payload = (updatedList != null) ? updatedList : java.util.Collections.emptyList();
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", payload);
    }

    // ✅ 친구 리스트 갱신 신호
    private void pushFriendsRefresh(Integer userId) {
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/friends", "refresh");
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
