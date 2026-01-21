package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.AuthDao;
import com.example.web_crafter_java.dao.AuthUserRow;
import com.example.web_crafter_java.dto.AuthLoginReq;
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
        String confirm = trim(req.getPasswordConfirm());

        if (!StringUtils.hasText(loginId)) throw new IllegalArgumentException("LOGIN_ID_REQUIRED");
        if (!StringUtils.hasText(password)) throw new IllegalArgumentException("PASSWORD_REQUIRED");
        if (StringUtils.hasText(confirm) && !password.equals(confirm))
            throw new IllegalArgumentException("PASSWORD_CONFIRM_MISMATCH");

        if (authDao.countByLoginId(loginId) > 0) throw new IllegalArgumentException("LOGIN_ID_TAKEN");

        String email = trim(req.getEmail());
        if (StringUtils.hasText(email) && authDao.countByEmail(email) > 0)
            throw new IllegalArgumentException("EMAIL_TAKEN");

        String nickname = trim(req.getNickname());
        if (StringUtils.hasText(nickname) && authDao.countByNickname(nickname) > 0)
            throw new IllegalArgumentException("NICKNAME_TAKEN");

        AuthUserRow row = new AuthUserRow();
        row.setLoginId(loginId);
        row.setEmail(StringUtils.hasText(email) ? email : null);
        row.setNickname(StringUtils.hasText(nickname) ? nickname : null);
        row.setName(StringUtils.hasText(req.getName()) ? trim(req.getName()) : null);
        row.setPasswordHash(encoder.encode(password));

        String birth = trim(req.getBirth());
        if (StringUtils.hasText(birth)) row.setBirth(LocalDate.parse(birth));

        authDao.insertUser(row);
    }

    public AuthUserRow login(AuthLoginReq req) {
        String loginId = trim(req.getLoginId());
        String password = trim(req.getPassword());

        if (!StringUtils.hasText(loginId)) throw new IllegalArgumentException("LOGIN_ID_REQUIRED");
        if (!StringUtils.hasText(password)) throw new IllegalArgumentException("PASSWORD_REQUIRED");

        AuthUserRow user = authDao.findByLoginId(loginId);
        if (user == null) throw new IllegalArgumentException("LOGIN_FAILED");

        if (!encoder.matches(password, user.getPasswordHash()))
            throw new IllegalArgumentException("LOGIN_FAILED");

        // 응답용으로 비번해시 제거
        user.setPasswordHash(null);
        return user;
    }

    public boolean isAvailable(String field, String value) {
        String f = trim(field);
        String v = trim(value);

        if (!StringUtils.hasText(f)) throw new IllegalArgumentException("FIELD_REQUIRED");
        if (!StringUtils.hasText(v)) throw new IllegalArgumentException("VALUE_REQUIRED");

        return switch (f) {
            case "loginId" -> authDao.countByLoginId(v) == 0;
            case "email" -> authDao.countByEmail(v) == 0;
            case "nickname" -> authDao.countByNickname(v) == 0;
            default -> throw new IllegalArgumentException("FIELD_NOT_ALLOWED");
        };
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}