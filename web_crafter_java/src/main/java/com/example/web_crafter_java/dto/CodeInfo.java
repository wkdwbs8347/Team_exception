package com.example.web_crafter_java.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeInfo {
  private String code;  // 이메일 인증코드
  private Instant expiresAt; // 해당 인증코드가 만료되는 시간 (현재 5분으로 설정해뒀음)
}
