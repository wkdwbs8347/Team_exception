package com.example.web_crafter_java.dao;

import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.Notification;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface FriendDao {

    // 1. 친구 검색
    @Select("""
        SELECT id, nickname, email, bio, status 
        FROM `user` 
        WHERE (nickname = #{keyword} OR email = #{keyword}) 
        AND id != #{myId}
    """)
    List<Member> searchUser(@Param("keyword") String keyword, @Param("myId") Integer myId);

    // 2. 친구 요청 저장
    @Insert("""
        INSERT INTO friend (requesterId, receiverId, status)
        VALUES (#{myId}, #{targetId}, 'PENDING')
    """)
    void insertFriendRequest(@Param("myId") Integer myId, @Param("targetId") Integer targetId);

    // 3. 중복 체크
    @Select("""
        SELECT COUNT(*) FROM friend 
        WHERE (requesterId = #{myId} AND receiverId = #{targetId})
           OR (requesterId = #{targetId} AND receiverId = #{myId})
    """)
    int existsRelation(@Param("myId") Integer myId, @Param("targetId") Integer targetId);

    // 4. 알림 저장
    @Insert("""
        INSERT INTO notification (receiverId, senderId, type, isRead, regDate)
        VALUES (#{receiverId}, #{senderId}, #{type}, 0, NOW())
    """)
    void insertNotification(Notification dto);
    
    // 🔥 [수정 1] 내 친구 목록 조회 (쿼리 강화)
    // 내가 신청했든(requester), 내가 받았든(receiver) 상관없이 상대방 정보를 정확히 가져옵니다.
    @Select("""
        SELECT 
            u.id, 
            u.nickname, 
            u.email, 
            u.bio, 
            -- ✅ 1. DB의 is_login을 자바 변수명 isLogin으로 매핑 (AS 사용)
            u.is_login AS isLogin, 
            -- ✅ 2. 실시간 상태에 따라 'online' 또는 'offline' 문자열 반환
            CASE 
                WHEN u.is_login = 1 THEN 'online' 
                ELSE 'offline' 
            END AS connectStatus 
        FROM friend f
        INNER JOIN `user` u ON u.id = (
            CASE 
                WHEN f.requesterId = #{myId} THEN f.receiverId
                WHEN f.receiverId = #{myId} THEN f.requesterId
            END
        )
        WHERE (f.requesterId = #{myId} OR f.receiverId = #{myId})
          AND f.status = 'ACCEPTED'
    """)
    List<Member> getMyFriends(@Param("myId") Integer myId);

// 6. 내 알림 목록 조회 (수정됨: member -> `user`)
    @Select("""
        SELECT 
            n.id, 
            n.receiverId, 
            n.senderId, 
            u.nickname AS senderName,  -- ✅ 'user' 테이블 별칭 u 사용
            n.type, 
            n.relId,                   
            n.isRead, 
            n.regDate
        FROM notification n
        JOIN `user` u ON n.senderId = u.id  -- 👈 여기가 핵심! (member -> `user`로 수정)
        WHERE n.receiverId = #{myId}
        ORDER BY n.regDate DESC
    """)
    java.util.List<com.example.web_crafter_java.dto.Notification> getMyNotifications(Integer myId);

    // 🔥 [수정 2] 친구 요청 수락 (강력한 업데이트)
    // 순서가 바뀌어도, 누가 보냈든 상관없이 둘 사이의 관계라면 무조건 수락 처리합니다.
    @Update("""
        UPDATE friend 
        SET status = 'ACCEPTED' 
        WHERE (requesterId = #{senderId} AND receiverId = #{myId})
           OR (requesterId = #{myId} AND receiverId = #{senderId})
    """)
    void acceptFriendRequest(@Param("senderId") Integer senderId, @Param("myId") Integer myId);

    // 8. 친구 요청 거절
    @Delete("""
        DELETE FROM friend 
        WHERE requesterId = #{senderId} AND receiverId = #{myId}
    """)
    void rejectFriendRequest(@Param("senderId") Integer senderId, @Param("myId") Integer myId);

    // 9. 알림 삭제
    @Delete("DELETE FROM notification WHERE id = #{notiId}")
    void deleteNotification(Integer notiId);

    @Delete("""
        DELETE FROM friend 
        WHERE (requesterId = #{myId} AND receiverId = #{targetId})
           OR (requesterId = #{targetId} AND receiverId = #{myId})
    """)
    void deleteFriendship(@Param("myId") Integer myId, @Param("targetId") Integer targetId);

}