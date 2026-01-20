package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.AuthDao;
import com.example.web_crafter_java.dao.AuthUserRow;
import com.example.web_crafter_java.dto.AuthRegisterReq;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthDao authDao;
    private final BCryptPasswordEncoder encoder;

    public void register(AuthRegisterReq req) {
        String loginId = trim(req.getLoginId());
        String password = trim(req.getPassword());

        if (!StringUtils.hasText(loginId)) throw new IllegalArgumentException("아이디를 입력하세요.");
        if (!StringUtils.hasText(password)) throw new IllegalArgumentException("비밀번호를 입력하세요.");

        if (authDao.countByLoginId(loginId) > 0) throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");

        String email = trim(req.getEmail());
        if (StringUtils.hasText(email) && authDao.countByEmail(email) > 0)
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");

        String nickname = trim(req.getNickname());
        if (StringUtils.hasText(nickname) && authDao.countByNickname(nickname) > 0)
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");

        String confirm = trim(req.getPasswordConfirm());
        if (StringUtils.hasText(confirm) && !password.equals(confirm))
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");

        AuthUserRow row = new AuthUserRow();
        row.setLoginId(loginId);
        row.setEmail(StringUtils.hasText(email) ? email : null);
        row.setNickname(StringUtils.hasText(nickname) ? nickname : null);
        row.setName(StringUtils.hasText(req.getName()) ? trim(req.getName()) : null);
        row.setPasswordHash(encoder.encode(password));

        String birth = trim(req.getBirth());
        if (StringUtils.hasText(birth)) {
            row.setBirth(LocalDate.parse(birth)); // "YYYY-MM-DD"
        }

        authDao.insertUser(row);
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}