package com.example.web_crafter_java.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.example.web_crafter_java.dao.MemberDao;
import com.example.web_crafter_java.dao.RememberTokenDao;
import com.example.web_crafter_java.dto.CodeInfo;
import com.example.web_crafter_java.dto.Member;
import com.example.web_crafter_java.dto.MemberRegisterReq;
import com.example.web_crafter_java.dto.MemberUpdateReq;

@Service
public class MemberService {

	private final MemberDao memberDao;
	private final JavaMailSender javaMailSender;
	private final BCryptPasswordEncoder encoder;
	private final RememberTokenDao rememberTokenDao;

	public MemberService(MemberDao memberDao, JavaMailSender javaMailSender, BCryptPasswordEncoder encoder,
			RememberTokenDao rememberTokenDao) {
		this.memberDao = memberDao;
		this.javaMailSender = javaMailSender;
		this.encoder = encoder;
		this.rememberTokenDao = rememberTokenDao;
	}

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private String generateRawToken() {
		byte[] b = new byte[64];
		SECURE_RANDOM.nextBytes(b);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
	}

	private String sha256Base64(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// remember 토큰 발급 (7일)
	public String issueRememberToken(Integer userId) {
		String raw = generateRawToken();
		String hash = sha256Base64(raw);

		String expiresAt = LocalDateTime.now().plusDays(7).format(DT);

		// 한 유저는 1개만 유지하고 싶으면 기존 토큰 revoke
		rememberTokenDao.revokeAllByUserId(userId);

		rememberTokenDao.insert(userId, hash, expiresAt);
		return raw; // 쿠키에 넣을 값(원문)
	}

	// 쿠키로 들어온 remember 토큰 검증 → userId 반환
	public Integer verifyRememberToken(String rawToken) {
		if (rawToken == null || rawToken.isBlank())
			return null;
		String hash = sha256Base64(rawToken);
		return rememberTokenDao.findUserIdByValidTokenHash(hash);
	}

	// 로그아웃 시 토큰 폐기
	public void revokeRememberToken(String rawToken) {
		if (rawToken == null || rawToken.isBlank())
			return;
		String hash = sha256Base64(rawToken);
		rememberTokenDao.revokeByTokenHash(hash);
	}

	@Transactional(readOnly = true)
	public boolean isNicknameAvailable(String nickname) {
		if (nickname == null)
			return false;

		String nick = nickname.trim();

		// 프론트에서 검증하지만 여기서도 한번 더 안전하게 검증
		if (nick.isEmpty())
			return false;
		if (nick.length() < 2 || nick.length() > 20)
			return false;
		if (nick.contains(" "))
			return false;

		// 닉네임이 DB에 존재하면 사용 불가
		int count = this.memberDao.countByNickname(nick);
		return count == 0;
	}

	// email -> (code, expiresAt) 저장소
	private final ConcurrentHashMap<String, CodeInfo> codeStore = new ConcurrentHashMap<>();
	private final Random random = new Random();

	// 이메일 인증번호 전송
	public void sendEmailVerificationCode(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("이메일을 입력해주세요.");
		}

		// 인증번호 형식
		String code = String.format("%06d", random.nextInt(1_000_000));

		// 5분 만료
		codeStore.put(email, new CodeInfo(code, Instant.now().plusSeconds(300)));

		// 이메일 발송
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("[Web Crafter] 이메일 인증번호");
		message.setText("""
				안녕하세요. Web Crafter 인증번호 입니다.

				인증번호: %s

				인증번호를 5분안에 입력해주세요.
				""".formatted(code));

		javaMailSender.send(message);
	}

	// 이메일 인증번호 검증
	public boolean verifyEmailCode(String email, String code) {
		if (email == null || email.isBlank())
			return false;
		if (code == null || code.isBlank())
			return false;

		CodeInfo info = codeStore.get(email);
		if (info == null)
			return false;

		if (Instant.now().isAfter(info.getExpiresAt())) {
			codeStore.remove(email);
			return false;
		}

		boolean ok = info.getCode().equals(code.trim());
		if (ok) {
			codeStore.remove(email); // 1회성
		}

		return ok;
	}

	// 회원가입
	public void register(MemberRegisterReq req) {

		// 이메일 중복 방어
		if (memberDao.existsByEmail(req.getEmail()) != null) {
			throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
		}

		Member m = new Member();
		m.setName(req.getFirstName() + req.getLastName()); // 성 + 이름 합침
		m.setEmail(req.getEmail());
		m.setNickname(req.getNickname());
		m.setLoginPw(encoder.encode(req.getPassword())); // 비밀번호 암호화
		m.setStatus(1);

		this.memberDao.insert(m);
	}

	// 로그인 → memberId 반환
	public Integer login(String email, String password) {
		Member member = memberDao.findByEmail(email);

		if (member == null) {
			throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
		}

		if (!encoder.matches(password, member.getLoginPw())) {
			throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
		}

		if (member.getStatus() != null && member.getStatus() == 0) {
			throw new IllegalArgumentException("탈퇴 처리된 회원입니다.");
		}

		return (Integer) member.getId();
	}

	// 로그인된 사용자 정보 (비밀번호 제외)
	public Member getMe(Integer memberId) {
		return memberDao.findByIdWithoutPassword(memberId);
	}

	public Member getMyPageData(Integer memberId) {
    // MemberDao에 추가한 통계 쿼리 메서드 호출
    	return memberDao.getMyPageStats(memberId);
	}

@Transactional
public Member updateProfile(Integer id, MemberUpdateReq req) {

    Member currentMember = memberDao.findByIdWithPassword(id);
    if (currentMember == null) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."
        );
    }

    // 🔐 비밀번호 변경
    if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {

        if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "현재 비밀번호를 입력해주세요."
            );
        }

        if (!encoder.matches(req.getCurrentPassword(), currentMember.getLoginPw())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."
            );
        }

        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "새 비밀번호는 기존 비밀번호와 달라야 합니다."
            );
        }

        if (req.getNewPassword().length() < 8) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "비밀번호는 최소 8자 이상이어야 합니다."
            );
        }

        memberDao.updatePassword(id, encoder.encode(req.getNewPassword()));
    }

    // ✏️ 프로필 수정
    String nickname = req.getNickname() != null
            ? req.getNickname()
            : currentMember.getNickname();

    String bio = req.getBio() != null
            ? req.getBio()
            : currentMember.getBio();

    memberDao.updateProfile(id, nickname, bio);

    return memberDao.findByIdWithoutPassword(id);
}

// 📂 내가 만든 프로젝트 목록 가져오기
    public List<Map<String, Object>> getMyProjects(Integer userId) {
        return memberDao.getMyProjects(userId);
    }

    // 🤝 초대받은 협업 프로젝트 목록 가져오기
    public List<Map<String, Object>> getSharedProjects(Integer userId) {
        return memberDao.getSharedProjects(userId);
    }

}