package com.example.web_crafter_java.dao;

import org.apache.ibatis.annotations.*;
import com.example.web_crafter_java.dto.UserWeb;
import com.example.web_crafter_java.dto.UserWebPage;

@Mapper
public interface ProjectDao {
    
/* 1. 프로젝트 메인 생성: userWeb 테이블 */
    @Insert("""
        INSERT INTO userWeb (userId, title, regDate, updateDate)
        VALUES (#{userId}, #{title}, NOW(), NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id") // 생성된 PK(id)를 다시 가져옴 [cite: 2026-01-16]
    void insertUserWeb(UserWeb userWeb);

    /* 2. 초기 페이지 생성: userWeb_pages 테이블 */
    @Insert("""
        INSERT INTO userWeb_pages (webId, pageName, layoutData, styleData, logicData, regDate, updateDate)
        VALUES (#{webId}, #{pageName}, #{layoutData}, #{styleData}, #{logicData}, NOW(), NOW())
    """)
    void insertUserWebPage(UserWebPage page); 

    /* 3. 멤버 권한 등록: userWeb_member 테이블 */
    @Insert("""
        INSERT INTO userWeb_member (webId, userId, role, regDate)
        VALUES (#{webId}, #{userId}, #{role}, NOW())
    """)
    void insertProjectMember(@Param("webId") Integer webId, @Param("userId") Integer userId, @Param("role") String role);

    @Update("""
        UPDATE userWeb 
        SET title = #{newName}, updateDate = NOW() 
        WHERE id = #{projectId}
    """)
    void updateTitle(@Param("projectId") Integer projectId, @Param("newName") String newName);

    /**
     * 1. 특정 프로젝트의 페이지 데이터 조회
     * webId가 일치하는 데이터만 가져오기 때문에 프로젝트별 데이터 분리가 가능해집니다.
     */
    @Select("""
        SELECT p.*, w.title
        FROM userWeb_pages p
        JOIN userWeb w ON p.webId = w.id
        WHERE p.webId = #{webId}
        AND p.pageName = #{pageName}
    """)
    UserWebPage getPageData(@Param("webId") Integer webId, @Param("pageName") String pageName);


    // 이 방식이 '이름 변경'과 '다중 페이지' 대응에 최적화된 최종형입니다. [cite: 2026-01-21]
    @Update("""
        UPDATE userWeb_pages 
        SET layoutData = #{pageData.layoutData}, 
            styleData = #{pageData.styleData}, 
            logicData = #{pageData.logicData}, 
            pageName = #{pageData.pageName},
            updateDate = NOW()
        WHERE webId = #{webId} AND pageName = #{oldPageName}
    """)
    void updatePageData(
        @Param("webId") Integer webId, @Param("oldPageName") String oldPageName, @Param("pageData") UserWebPage pageData);

    /**
     * 3. 프로젝트 삭제
     * userWeb 테이블에서 삭제하면 DB의 외래키 설정(ON DELETE CASCADE)에 의해
     * 관련된 페이지와 멤버 정보도 자동으로 삭제됩니다.
     */
    @Delete("DELETE FROM userWeb WHERE id = #{projectId}")
    void deleteProject(@Param("projectId") Integer projectId);

@Insert("""
        INSERT INTO userWeb_pages 
            (webId, pageName, layoutData, styleData, logicData, regDate, updateDate)
        VALUES 
            (#{webId}, #{pageName}, #{layoutData}, #{styleData}, #{logicData}, NOW(), NOW())
    """)
    void insertNewPage(UserWebPage pageData);

// ProjectDao.java 인터페이스 내부

@Delete("""
    DELETE FROM userWeb_pages 
    WHERE webId = #{webId} AND pageName = #{pageName}
""")
void deletePageByName(@Param("webId") Integer webId, @Param("pageName") String pageName);

@Select("""
        SELECT id, webId, pageName
        FROM userWeb_pages
        WHERE webId = #{webId}
    """)
    java.util.List<UserWebPage> selectPagesByWebId(Integer webId);

}