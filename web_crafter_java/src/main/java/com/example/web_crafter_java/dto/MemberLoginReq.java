package com.example.web_crafter_java.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 로그인 요청 전용 DTO
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberLoginReq {
  private String email;
  private String password;
}
