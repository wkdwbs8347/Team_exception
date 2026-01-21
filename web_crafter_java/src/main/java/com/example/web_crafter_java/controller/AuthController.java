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

        // ✅ cookie(session) 기반
        HttpSession session = request.getSession(true);
        session.setAttribute("WC_USER_ID", user.getId());

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "user", user
        ));
    }

    @GetMapping("/duplicate")
    public ResponseEntity<?> duplicate(@RequestParam String field, @RequestParam String value) {
        boolean available = authService.isAvailable(field, value);
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