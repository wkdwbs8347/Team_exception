package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.CommonDao;
import com.example.web_crafter_java.dto.StatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final CommonDao commonDao;

    public StatsDto getPlatformStats() {
        // DB에서 통계 정보 조회
        return commonDao.selectPlatformStats();
    }
}