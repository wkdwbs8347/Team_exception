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
        Integer webId = req.getWebId();
        if (webId == null) throw new IllegalArgumentException("WEB_ID_REQUIRED"); // ✅ 추가

        String loginId = trim(req.getLoginId());
        String password = trim(req.getPassword());
        String confirm = trim(req.getPasswordConfirm());

        if (!StringUtils.hasText(loginId)) throw new IllegalArgumentException("LOGIN_ID_REQUIRED");
        if (!StringUtils.hasText(password)) throw new IllegalArgumentException("PASSWORD_REQUIRED");
        if (StringUtils.hasText(confirm) && !password.equals(confirm))
            throw new IllegalArgumentException("PASSWORD_CONFIRM_MISMATCH");

        // ✅ webId 스코프
        if (authDao.countByLoginId(webId, loginId) > 0) throw new IllegalArgumentException("LOGIN_ID_TAKEN");

        String email = trim(req.getEmail());
        if (StringUtils.hasText(email) && authDao.countByEmail(webId, email) > 0)
            throw new IllegalArgumentException("EMAIL_TAKEN");

        String nickname = trim(req.getNickname());
        if (StringUtils.hasText(nickname) && authDao.countByNickname(webId, nickname) > 0)
            throw new IllegalArgumentException("NICKNAME_TAKEN");

        AuthUserRow row = new AuthUserRow();
        row.setWebId(webId); // ✅ 추가
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
        Integer webId = req.getWebId();
        if (webId == null) throw new IllegalArgumentException("WEB_ID_REQUIRED"); // ✅ 추가

        String loginId = trim(req.getLoginId());
        String password = trim(req.getPassword());

        if (!StringUtils.hasText(loginId)) throw new IllegalArgumentException("LOGIN_ID_REQUIRED");
        if (!StringUtils.hasText(password)) throw new IllegalArgumentException("PASSWORD_REQUIRED");

        // ✅ webId 스코프
        AuthUserRow user = authDao.findByLoginId(webId, loginId);
        if (user == null) throw new IllegalArgumentException("LOGIN_FAILED");

        if (!encoder.matches(password, user.getPasswordHash()))
            throw new IllegalArgumentException("LOGIN_FAILED");

        user.setPasswordHash(null);
        return user;
    }

    public boolean isAvailable(Integer webId, String field, String value) {
        if (webId == null) throw new IllegalArgumentException("WEB_ID_REQUIRED"); // ✅ 추가

        String f = trim(field);
        String v = trim(value);

        if (!StringUtils.hasText(f)) throw new IllegalArgumentException("FIELD_REQUIRED");
        if (!StringUtils.hasText(v)) throw new IllegalArgumentException("VALUE_REQUIRED");

        return switch (f) {
            case "loginId" -> authDao.countByLoginId(webId, v) == 0;
            case "email" -> authDao.countByEmail(webId, v) == 0;
            case "nickname" -> authDao.countByNickname(webId, v) == 0;
            default -> throw new IllegalArgumentException("FIELD_NOT_ALLOWED");
        };
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}