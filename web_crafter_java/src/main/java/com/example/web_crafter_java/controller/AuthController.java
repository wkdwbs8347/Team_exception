package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.dto.AuthRegisterReq;
import com.example.web_crafter_java.service.AuthService;
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
        return ResponseEntity.ok(Map.of("ok", true, "message", "회원가입 완료"));
    }
}