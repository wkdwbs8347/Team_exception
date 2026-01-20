package com.example.web_crafter_java.dao;

import org.apache.ibatis.annotations.*;
import com.example.web_crafter_java.dto.UserWeb;
import com.example.web_crafter_java.dto.UserWebPage;

@Mapper
public interface ProjectDao {
    
    void insertUserWeb(UserWeb userWeb);
    void insertUserWebPage(UserWebPage page); 
    void insertProjectMember(@Param("webId") Integer webId,
                             @Param("userId") Integer userId,
                             @Param("role") String role);

    @Update("""
        UPDATE userWeb 
        SET title = #{newName}, updateDate = NOW() 
        WHERE id = #{projectId}
    """)
    void updateTitle(@Param("projectId") Integer projectId, @Param("newName") String newName);

    // --- ⬇️ 여기서부터 새로 추가되는 코드입니다 ⬇️ ---

    /**
     * 1. 특정 프로젝트의 페이지 데이터 조회
     * webId가 일치하는 데이터만 가져오기 때문에 프로젝트별 데이터 분리가 가능해집니다.
     */
    @Select("""
        SELECT 
            p.*, 
            w.title AS title 
        FROM userWeb_pages p
        JOIN userWeb w ON p.webId = w.id
        WHERE p.webId = #{webId} AND p.pageName = 'index'
    """)
    UserWebPage getPageData(@Param("webId") Integer webId);

    /**
     * 2. 특정 프로젝트의 페이지 데이터(블록 등) 업데이트
     * WHERE webId 조건이 있어야 다른 프로젝트의 데이터를 덮어씌우지 않습니다.
     */
    @Update("""
        UPDATE userWeb_pages 
        SET layoutData = #{layoutData}, 
            styleData = #{styleData}, 
            logicData = #{logicData}, 
            updateDate = NOW()
        WHERE webId = #{webId}
    """)
    void updatePageData(UserWebPage page);

    /**
     * 3. 프로젝트 삭제
     * userWeb 테이블에서 삭제하면 DB의 외래키 설정(ON DELETE CASCADE)에 의해
     * 관련된 페이지와 멤버 정보도 자동으로 삭제됩니다.
     */
    @Delete("DELETE FROM userWeb WHERE id = #{projectId}")
    void deleteProject(@Param("projectId") Integer projectId);
}