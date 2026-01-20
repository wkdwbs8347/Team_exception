package com.example.web_crafter_java.dao;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AuthUserRow {
    private Long id;
    private String loginId;
    private String email;
    private String nickname;
    private String name;
    private String passwordHash;
    private LocalDate birth;
}