package com.example.web_crafter_java.dto;

import lombok.Data;

@Data
public class RegisterReq {
    private String loginId;
    private String email;
    private String nickname;
    private String name;
    private String password;
    private String passwordConfirm;
    private String birth; // "YYYY-MM-DD"
}