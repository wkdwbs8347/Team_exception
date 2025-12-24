package com.example.web_crafter_java.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Member {
	private int id;
	private String name;
	private String regDate;
	private String updateDate;
	private String loginId;
	private String loginPw;
	private int status;
}