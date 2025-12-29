package com.example.web_crafter_java.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeInfo {
  private String code;
  private Instant expiresAt;
}
