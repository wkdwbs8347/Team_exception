package com.example.web_crafter_java.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.web_crafter_java.service.MemberService;

@RestController
@RequestMapping("/api")
public class MemberController {
	
	private MemberService memberService;
	
	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	
	
}