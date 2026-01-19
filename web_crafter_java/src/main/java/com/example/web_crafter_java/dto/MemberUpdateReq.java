package com.example.web_crafter_java.dto; // 본인의 실제 패키지 경로로 확인해주세요!

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateReq {
    private String nickname;        // 수정할 닉네임
    private String bio;             // 수정할 상태 메시지
    private String currentPassword; // 본인 확인용 현재 비밀번호
    private String newPassword;     // 변경할 새 비밀번호
}