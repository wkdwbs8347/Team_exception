package com.example.web_crafter_java.dao;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AuthDao {

    @Select("SELECT COUNT(*) FROM wc_user WHERE login_id = #{loginId}")
    int countByLoginId(@Param("loginId") String loginId);

    @Select("SELECT COUNT(*) FROM wc_user WHERE email = #{email}")
    int countByEmail(@Param("email") String email);

    @Select("SELECT COUNT(*) FROM wc_user WHERE nickname = #{nickname}")
    int countByNickname(@Param("nickname") String nickname);

    @Insert("""
        INSERT INTO wc_user (login_id, email, nickname, name, password_hash, birth)
        VALUES (#{loginId}, #{email}, #{nickname}, #{name}, #{passwordHash}, #{birth})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(AuthUserRow row);

    // ✅ 로그인 조회
    @Select("""
        SELECT
          id,
          login_id AS loginId,
          email,
          nickname,
          name,
          password_hash AS passwordHash,
          birth
        FROM wc_user
        WHERE login_id = #{loginId}
        LIMIT 1
    """)
    AuthUserRow findByLoginId(@Param("loginId") String loginId);
}