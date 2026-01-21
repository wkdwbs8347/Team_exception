package com.example.web_crafter_java.dto;

import lombok.Data;

@Data
public class AuthLoginReq {
    private String loginId;
    private String password;
}