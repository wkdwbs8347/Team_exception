package com.example.web_crafter_java.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.web_crafter_java.dao.MemberDao;

@Service
public class MemberService {

	private MemberDao memberDao;
	private JavaMailSender javaMailSender;

	public MemberService(MemberDao memberDao, JavaMailSender javaMailSender) {
		this.memberDao = memberDao;
		this.javaMailSender = javaMailSender;
	}


}