package com.example.web_crafter_java.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.MemberLoginReq;
import com.example.web_crafter_java.dto.MemberRegisterReq;
import com.example.web_crafter_java.dto.MemberUpdateReq;
import com.example.web_crafter_java.service.MemberService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
	public ResponseEntity<?> login(@RequestBody MemberLoginReq req,
			HttpSession session,
			HttpServletResponse response) {
		try {
			Integer memberId = memberService.login(req.getEmail(), req.getPassword());
			session.setAttribute("loginedMemberId", memberId);

			// rememberMe면 토큰 발급 + 쿠키 세팅
			if (Boolean.TRUE.equals(req.getRememberMe())) {
				String rawToken = memberService.issueRememberToken(memberId);

				Cookie c = new Cookie("REMEMBER", rawToken);
				c.setHttpOnly(true);
				c.setPath("/");
				c.setMaxAge(60 * 60 * 24 * 7); // 7일
				// 운영 HTTPS면 true 권장
				// c.setSecure(true);
				response.addCookie(c);
			}

			return ResponseEntity.ok(Map.of("message", "로그인 성공"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "로그인 실패"));
		}
	}

	// 자동 로그인
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(HttpSession session,
			HttpServletRequest request) {

		Integer memberId = (Integer) session.getAttribute("loginedMemberId");
		if (memberId != null) {
			return ResponseEntity.ok(Map.of("message", "already logged in"));
		}

		String remember = null;
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if ("REMEMBER".equals(c.getName()))
					remember = c.getValue();
			}
		}

		Integer newMemberId = memberService.verifyRememberToken(remember);
		if (newMemberId == null) {
			return ResponseEntity.status(401).body(Map.of("message", "자동 로그인 토큰이 유효하지 않습니다."));
		}

		session.setAttribute("loginedMemberId", newMemberId);
		return ResponseEntity.ok(Map.of("message", "refreshed"));

	}

	// 로그아웃
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session,
			HttpServletRequest request,
			HttpServletResponse response) {

		// remember 토큰 revoke
		String remember = null;
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if ("REMEMBER".equals(c.getName()))
					remember = c.getValue();
			}
		}
		memberService.revokeRememberToken(remember);

		// 세션 종료
		session.invalidate();

		// 쿠키 삭제
		Cookie del = new Cookie("REMEMBER", "");
		del.setPath("/");
		del.setMaxAge(0);
		response.addCookie(del);

		return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
	}

	@PutMapping("/profile")
	public ResponseEntity<?> updateProfile(@RequestBody MemberUpdateReq req, HttpSession session) {
		Integer memberId = (Integer) session.getAttribute("loginedMemberId");

		if (memberId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "로그인이 필요합니다."));
		}

		try {
			Member updatedMember = memberService.updateProfile(memberId, req);
			return ResponseEntity.ok(updatedMember);

		} catch (ResponseStatusException e) {
			return ResponseEntity
					.status(e.getStatusCode())
					.body(Map.of("message", e.getReason()));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "수정 중 알 수 없는 오류가 발생했습니다."));
		}
	}


	// 로그인체크 및 사용자 정보(필요한 값만)
	@GetMapping("/me")
	public ResponseEntity<?> me(HttpSession session) {
    Integer memberId = (Integer) session.getAttribute("loginedMemberId");
    if (memberId == null) {
        return ResponseEntity.status(401)
                .body(Map.of("message", "로그인이 필요합니다."));
    }

    // 1. 회원 정보 및 통계 숫자 (이미 구현된 것)
    Member member = memberService.getMyPageData(memberId);

    // 2. 프로젝트 리스트 가져오기 (Service에 메서드 추가 필요)
    List<Map<String, Object>> myProjects = memberService.getMyProjects(memberId);
    List<Map<String, Object>> sharedProjects = memberService.getSharedProjects(memberId);

    // 3. 통합 응답 맵 구성
    Map<String, Object> response = new HashMap<>();
    response.put("member", member);
    response.put("myProjects", myProjects);
    response.put("sharedProjects", sharedProjects);

    return ResponseEntity.ok(response);
}

// 비밀번호 찾기: 임시 비밀번호 발급 + 이메일 발송
@PostMapping("/password/find")
public ResponseEntity<?> findPasswordAndSendTemp(@RequestBody Map<String, String> body) {
  try {
    String name = body.get("name");
    String email = body.get("email");

    memberService.sendTemporaryPassword(name, email);

    return ResponseEntity.ok(Map.of("message", "임시 비밀번호를 이메일로 전송했습니다."));
  } catch (IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
  } catch (Exception e) {
    e.printStackTrace();
    return ResponseEntity.internalServerError().body(Map.of("message", "임시 비밀번호 전송 실패"));
  }
}

}