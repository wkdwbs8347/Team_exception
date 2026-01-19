package com.example.web_crafter_java.config; // 본인의 실제 폴더 경로에 맞게 수정

import com.example.web_crafter_java.dto.Member; // Member DTO 경로 확인
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class UserAdapter extends User {
    private final Member member;

    public UserAdapter(Member member) {
        super(member.getEmail(), member.getLoginPw(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.member = member;
    }
}