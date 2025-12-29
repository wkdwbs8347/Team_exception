package com.example.web_crafter_java.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.web_crafter_java.dto.Member;

@Mapper
public interface MemberDao {

  // 회원가입 시 닉네임 중복체크
  @Select("""
          SELECT COUNT(*)
          FROM `user`
          WHERE nickname = #{nickname}
      """)
  int countByNickname(@Param("nickname") String nickname);

  // 회원가입 시 이메일 중복 검증
  @Select("SELECT id FROM `user` WHERE email = #{email} LIMIT 1")
  Integer existsByEmail(String email);

  // 회원가입
  @Insert("""
          INSERT INTO `user`
          (`name`, email, loginPw, nickname, `status`)
          VALUES
          (#{name}, #{email}, #{loginPw}, #{nickname}, #{status})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Member m);

  // 로그인용: 이메일로 회원 조회 (비밀번호 비교용)
  @Select("""
          SELECT id, email, loginPw, nickname, status
          FROM `user`
          WHERE email = #{email}
          LIMIT 1
      """)
  Member findByEmail(String email);

  // 로그인 후 내 정보 조회 (비밀번호 제외)
  @Select("""
          SELECT id, name, email, nickname, regDate, authPath, status
          FROM `user`
          WHERE id = #{id}
      """)
  Member findByIdWithoutPassword(Integer id);

}