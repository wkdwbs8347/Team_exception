package com.example.web_crafter_java.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 멤버 DTO
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Member {
	private Integer id; // PK
	private String name; // 이름
	private String email; // 이메일
	private String loginPw; // 비밀번호
	private String nickname; // 닉네임
	private String bio; // 소개글
	private String regDate; // 가입일 
	private String authPath; // 회원가입 종류 / ? 일반회원가입 : 소셜로그인
	private Integer status; // 회원유지 / 탈퇴유예 상태
}