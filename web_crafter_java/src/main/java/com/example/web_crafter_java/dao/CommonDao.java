package com.example.web_crafter_java.dao;

import com.example.web_crafter_java.dto.StatsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommonDao {

    /**
     * ✅ 플랫폼 전체 통계 조회
     * - `user` 테이블 백틱 처리 필수
     * - 마지막 괄호 및 콤마 위치 주의
     */
    @Select("""
        SELECT 
            (SELECT COUNT(*) FROM `user`) as userCount,
            (SELECT COUNT(*) FROM userWeb) as projectCount,
            (SELECT COALESCE(SUM(hit), 0) FROM userWeb) as totalViews
    """) // 👈 여기 닫는 괄호나 세미콜론이 SQL 문법에 맞게 잘 닫혔는지 확인
    StatsDto selectPlatformStats();
}