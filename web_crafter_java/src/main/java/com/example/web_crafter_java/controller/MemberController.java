package com.example.web_crafter_java.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.MemberLoginReq;
import com.example.web_crafter_java.dto.MemberRegisterReq;
import com.example.web_crafter_java.service.MemberService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/member")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	// 닉네임 중복체크
	@GetMapping("/nicknameCheck")
	public ResponseEntity<?> nicknameCheck(@RequestParam("nickname") String nickname) {
		boolean available = this.memberService.isNicknameAvailable(nickname);
		return ResponseEntity.ok(Map.of(
				"available", available,
				"nickname", nickname));
	}

	// 이메일 인증번호 전송
	@PostMapping("/emailSend")
	public ResponseEntity<?> emailSend(@RequestBody Map<String, String> body) {
		String email = body.get("email");
		this.memberService.sendEmailVerificationCode(email);
		return ResponseEntity.ok(Map.of("message", "인증번호가 전송되었습니다."));
	}

	// 이메일 인증번호 확인
	@PostMapping("/emailVerify")
	public ResponseEntity<?> emailVerify(@RequestBody Map<String, String> body) {
		String email = body.get("email");
		String code = body.get("code");

		boolean ok = this.memberService.verifyEmailCode(email, code);

		if (!ok) {
			return ResponseEntity.badRequest()
					.body(Map.of("message", "인증번호가 일치하지 않거나 만료되었습니다."));
		}

		return ResponseEntity.ok(Map.of("message", "인증이 완료되었습니다."));
	}

	// 회원가입
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody MemberRegisterReq req) {
		try {
			this.memberService.register(req);
			return ResponseEntity.ok(Map.of("message", "회원가입이 완료되었습니다."));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "회원가입 실패"));
		}
	}

	// 로그인
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody MemberLoginReq req, HttpSession session) {
		try {
			Integer memberId = memberService.login(req.getEmail(), req.getPassword());

			// login 성공 시 "인증 + 세션 저장"
			session.setAttribute("loginedMemberId", memberId);

			return ResponseEntity.ok(Map.of("message", "로그인 성공"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "로그인 실패"));
		}
	}

	// 로그인된 사용자 정보(필요한 값만)
	@GetMapping("/me")
	public ResponseEntity<?> me(HttpSession session) {
		Integer memberId = (Integer) session.getAttribute("loginedMemberId");
		if (memberId == null) {
			return ResponseEntity.status(401)
					.body(Map.of("message", "로그인이 필요합니다."));
		}

		Member member = memberService.getMe(memberId);

		return ResponseEntity.ok(member);
	}

}