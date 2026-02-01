package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.dto.AuthLoginReq;
import com.example.web_crafter_java.dto.AuthRegisterReq;
import com.example.web_crafter_java.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRegisterReq req) {
        authService.register(req);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginReq req, HttpServletRequest request) {
        var user = authService.login(req);

        HttpSession session = request.getSession(true);
        session.setAttribute("WC_USER_ID", user.getId());

        // ✅ (선택) 프로젝트 스코프도 세션에 저장해두면 이후 인증/조회에 도움됨
        session.setAttribute("WC_WEB_ID", req.getWebId());

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "user", user
        ));
    }

    @GetMapping("/duplicate")
    public ResponseEntity<?> duplicate(
            @RequestParam Integer webId,      // ✅ 추가
            @RequestParam String field,
            @RequestParam String value
    ) {
        boolean available = authService.isAvailable(webId, field, value);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "available", available,
                "field", field
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}