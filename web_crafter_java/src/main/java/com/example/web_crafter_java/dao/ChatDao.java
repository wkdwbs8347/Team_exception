package com.example.web_crafter_java.dao;

import com.example.web_crafter_java.dto.ChatMessage;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ChatDao {

    // 메시지 저장 (DB에 receiver_id 없음)
    @Insert("""
        INSERT INTO chat_message (room_id, sender_id, content, regDate)
        VALUES (#{roomId}, #{senderId}, #{content}, NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void saveMessage(ChatMessage message);

    // ✅ roomId 바인딩 확실하게 @Param 사용
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
    List<ChatMessage> getMessagesByRoomId(@Param("roomId") String roomId);

    @Delete("""
        DELETE FROM chat_message
        WHERE room_id = #{roomId}
    """)
    int deleteChatRoom(@Param("roomId") String roomId);
}
