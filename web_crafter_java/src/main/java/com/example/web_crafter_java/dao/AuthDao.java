package com.example.web_crafter_java.dao;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AuthDao {

    @Select("SELECT COUNT(*) FROM wc_user WHERE web_id = #{webId} AND login_id = #{loginId}")
    int countByLoginId(@Param("webId") int webId, @Param("loginId") String loginId);

    @Select("SELECT COUNT(*) FROM wc_user WHERE web_id = #{webId} AND email = #{email}")
    int countByEmail(@Param("webId") int webId, @Param("email") String email);

    @Select("SELECT COUNT(*) FROM wc_user WHERE web_id = #{webId} AND nickname = #{nickname}")
    int countByNickname(@Param("webId") int webId, @Param("nickname") String nickname);

    @Insert("""
        INSERT INTO wc_user (web_id, login_id, email, nickname, name, password_hash, birth)
        VALUES (#{webId}, #{loginId}, #{email}, #{nickname}, #{name}, #{passwordHash}, #{birth})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(AuthUserRow row);

    @Select("""
        SELECT
          id,
          web_id AS webId,
          login_id AS loginId,
          email,
          nickname,
          name,
          password_hash AS passwordHash,
          birth
        FROM wc_user
        WHERE web_id = #{webId}
          AND login_id = #{loginId}
        LIMIT 1
    """)
    AuthUserRow findByLoginId(@Param("webId") int webId, @Param("loginId") String loginId);
}