package com.example.web_crafter_java.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RememberTokenDao {

    @Insert("""
              INSERT INTO remember_token (user_id, token_hash, expires_at, revoked)
              VALUES (#{userId}, #{tokenHash}, #{expiresAt}, 0)
            """)
    int insert(@Param("userId") Integer userId,
            @Param("tokenHash") String tokenHash,
            @Param("expiresAt") String expiresAt);

    @Select("""
              SELECT user_id
              FROM remember_token
              WHERE token_hash = #{tokenHash}
                AND revoked = 0
                AND expires_at > NOW()
              LIMIT 1
            """)
    Integer findUserIdByValidTokenHash(@Param("tokenHash") String tokenHash);

    @Update("""
              UPDATE remember_token
              SET revoked = 1
              WHERE token_hash = #{tokenHash}
              AND revoked = 0
            """)
    int revokeByTokenHash(@Param("tokenHash") String tokenHash);

    @Update("""
              UPDATE remember_token
              SET revoked = 1
              WHERE user_id = #{userId}
              AND revoked = 0
            """)
    int revokeAllByUserId(@Param("userId") Integer userId);
}
