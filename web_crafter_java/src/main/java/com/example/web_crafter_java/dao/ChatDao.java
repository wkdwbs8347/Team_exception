package com.example.web_crafter_java.dao;

import com.example.web_crafter_java.dto.ChatMessage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ChatDao {

    // 메시지 저장
    @Insert("""
        INSERT INTO chat_message (room_id, sender_id, content, regDate)
        VALUES (#{roomId}, #{senderId}, #{content}, NOW())
    """)
    void saveMessage(ChatMessage message);

    // 이전 대화 내역 불러오기
    @Select("""
        SELECT id,
               room_id AS roomId,
               sender_id AS senderId,
               content,
               regDate
        FROM chat_message
        WHERE room_id = #{roomId}
        ORDER BY regDate ASC
        LIMIT 50
    """)
    List<ChatMessage> getMessagesByRoomId(String roomId);

    // ✅ 채팅방 전체 삭제 (친구 삭제 시 호출)
    @Delete("""
        DELETE FROM chat_message
        WHERE room_id = #{roomId}
    """)
    int deleteChatRoom(String roomId);
}
