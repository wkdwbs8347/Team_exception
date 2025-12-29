package com.example.web_crafter_java.dto;

import lombok.Data;

// 회원가입 요청 전용 DTO
@Data
public class MemberRegisterReq {
  private String firstName;
  private String lastName;
  private String nickname;
  private String email;
  private String password;
}
