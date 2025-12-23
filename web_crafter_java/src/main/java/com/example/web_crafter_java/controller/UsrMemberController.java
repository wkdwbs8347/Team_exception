package com.example.web_crafter_java.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.web_crafter_java.dto.LoginedMember;
import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.Req;
import com.example.web_crafter_java.dto.ResultData;
import com.example.web_crafter_java.service.MemberService;
import com.example.web_crafter_java.util.Util;

@RestController
@RequestMapping("/api")
public class UsrMemberController {
	
	private MemberService memberService;
	private Req req;
	
	public UsrMemberController(MemberService memberService, Req req) {
		this.memberService = memberService;
		this.req = req;
	}

	
	@GetMapping("/loginIdDupChk")
	@ResponseBody
	public ResultData loginIdDupChk(String loginId) {
		
		Member member = this.memberService.getMemberByLoginId(loginId);
		
		if (member != null) {
			return new ResultData<>("F-1", String.format("[ %s ]은(는) 이미 사용중인 아이디입니다", loginId));
		}
		return new ResultData<>("S-1", String.format("[ %s ]은(는) 사용 가능한 아이디입니다", loginId));
	}

	
	@PostMapping("/validLoginInfo")
	@ResponseBody
	public ResultData<Member> validLoginInfo(String loginId, String loginPw) {
		
		Member member = this.memberService.getMemberByLoginId(loginId);
		
		if (member == null) {
			return new ResultData<>("F-1", String.format("[ %s ]은(는) 존재하지 않는 아이디입니다", loginId));
		}
		
		if (!member.getLoginPw().equals(Util.encryptSHA256(loginPw))) {
			return new ResultData<>("F-2", "비밀번호가 일치하지 않습니다");
		}
		
		return new ResultData<>("S-1", "로그인 가능", member);
	}
	
	@PostMapping("/doLogin")
	@ResponseBody
	public String doLogin(int loginedMemberId, int loginedMemberAuthLevel, String loginId) {
		
		this.req.login(new LoginedMember(loginedMemberId, loginedMemberAuthLevel));
		
		return Util.jsReplace(String.format("[ %s ] 님 환영합니다~!", loginId), "/");
	}
	
	@GetMapping("/logout")
	@ResponseBody
	public String logout() {
		
		this.req.logout();
		
		return Util.jsReplace("정상적으로 로그아웃 되었습니다", "/");
	}
	
	@GetMapping("/getLoginId")
	@ResponseBody
	public String getLoginId() {
		return this.memberService.getLoginId(this.req.getLoginedMember().getId());
	}
	
	@GetMapping("/findLoginId")
	public String findLoginId() {
		return "usr/member/findLoginId";
	}
	
	@GetMapping("/doFindLoginId")
	@ResponseBody
	public ResultData doFindLoginId(String name, String email) {
		
		Member member = this.memberService.getMemberByNameAndEmail(name, email);
		
		if (member == null) {
			return new ResultData<>("F-1", "입력하신 정보와 일치하는 회원이 없습니다");
		}
		
		return new ResultData<>("S-1", String.format("회원님의 아이디는 [ %s ] 입니다", member.getLoginId()));
	}
	
	@GetMapping("/findLoginPw")
	public String findLoginPw() {
		return "usr/member/findLoginPw";
	}
	
	@GetMapping("/doFindLoginPw")
	@ResponseBody
	public ResultData doFindLoginPw(String loginId, String email) {
		
		Member member = this.memberService.getMemberByLoginId(loginId);
		
		if (member == null) {
			return new ResultData<>("F-1", "입력하신 아이디와 일치하는 회원이 없습니다");
		}
		
		if (member.getLoginId().equals(email) == false) {
			return new ResultData<>("F-2", "이메일이 일치하지 않습니다");
		}
		
		String tempPassword = Util.createTempPassword();
		
		try {
			this.memberService.sendPasswordRecoveryEmail(member, tempPassword);
		} catch (Exception e) {
			return new ResultData<>("F-3", "임시 패스워드 발송에 실패했습니다");
		}
		
		this.memberService.modifyPassword(member.getId(), Util.encryptSHA256(tempPassword));
		
		return new ResultData<>("S-1", "회원님의 이메일주소로 임시 패스워드가 발송되었습니다");
	}
	
	@GetMapping("/myPage")
	public String myPage(Model model) {
		
		Member member = this.memberService.getMemberById(this.req.getLoginedMember().getId());
		
		model.addAttribute("member", member);
		
		return "usr/member/myPage";
	}

	@GetMapping("/modify")
	public String modify(int id, String name, String email) {
		
		this.memberService.modifyMember(id, name, email);
		
		return "redirect:/usr/member/myPage";
	}
	
	@GetMapping("/modifyPwPop")
	public String modifyPwPop() {
		return "usr/member/modifyPwPop";
	}
	
	@PostMapping("/doModifyPw")
	@ResponseBody
	public String doModifyPw(String loginPw) {
		
		this.memberService.modifyPassword(req.getLoginedMember().getId(), Util.encryptSHA256(loginPw));
		
		return "비밀번호 변경이 완료되었습니다";
	}
}