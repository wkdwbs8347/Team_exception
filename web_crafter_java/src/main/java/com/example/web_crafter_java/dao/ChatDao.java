package com.example.web_crafter_java.dao;

import com.example.web_crafter_java.dto.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChatDao {
    // 메시지 저장
    @Insert("INSERT INTO chat_message (room_id, sender_id, content, regDate) " +
            "VALUES (#{roomId}, #{senderId}, #{content}, NOW())")
    void saveMessage(ChatMessage message);

    // 이전 대화 내역 불러오기 (최근 50개)
    @Select("SELECT * FROM chat_message WHERE room_id = #{roomId} ORDER BY regDate ASC LIMIT 50")
    List<ChatMessage> getMessagesByRoomId(String roomId);
}