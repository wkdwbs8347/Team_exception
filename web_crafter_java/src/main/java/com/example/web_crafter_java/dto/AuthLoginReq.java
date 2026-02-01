package com.example.web_crafter_java.dto;

import lombok.Data;

@Data
public class AuthLoginReq {
    private Integer webId; 
    private String loginId;
    private String password;
}