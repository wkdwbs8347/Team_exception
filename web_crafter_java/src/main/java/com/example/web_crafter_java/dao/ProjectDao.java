package com.example.web_crafter_java.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.web_crafter_java.dto.UserWeb;
// ✅ 페이지 객체 import (실제 패키지 경로에 맞게 수정하세요)
import com.example.web_crafter_java.dto.UserWebPage; 

@Mapper
public interface ProjectDao {
    
    void insertUserWeb(UserWeb userWeb);
    
    // ✅ 수정: 개별 파라미터 대신 객체 하나를 통째로 받도록 변경
    void insertUserWebPage(UserWebPage page); 
    
    void insertProjectMember(@Param("webId") Integer webId,
                             @Param("userId") Integer userId,
                             @Param("role") String role);
}