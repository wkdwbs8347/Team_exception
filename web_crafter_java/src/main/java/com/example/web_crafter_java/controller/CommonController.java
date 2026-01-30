package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.dto.StatsDto;
import com.example.web_crafter_java.service.CommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/common") // 프론트엔드 api 설정에 따라 '/api/common' 일 수도 있습니다.
@RequiredArgsConstructor
public class CommonController {

    private final CommonService commonService;

    // ✅ 전체 통계 조회 API
    @GetMapping("/stats")
    public ResponseEntity<StatsDto> getPlatformStats() {
        StatsDto stats = commonService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }
}