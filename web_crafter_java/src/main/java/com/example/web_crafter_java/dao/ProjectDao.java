package com.example.web_crafter_java.dao;

import java.util.List;

import org.apache.ibatis.annotations.*;

import com.example.web_crafter_java.dto.ProjectExploreDto;
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
    // ✅ 2. [추가] 미리보기 HTML 저장 (대표 이미지용 - userWeb 테이블)
    // 페이지 저장할 때 같이 호출해주면 됩니다.
    @Update("""
        UPDATE userWeb
        SET preview_html = #{previewHtml},
            updateDate = NOW()
        WHERE id = #{webId}
    """)
    void updateProjectPreview(@Param("webId") Integer webId, @Param("previewHtml") String previewHtml);
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

/* 1. [초대 체크] 이미 멤버인지 확인 (중복 초대 방지) */
    @Select("""
        SELECT COUNT(*) 
        FROM userWeb_member 
        WHERE webId = #{webId} AND userId = #{userId}
    """)
    int isMember(@Param("webId") Integer webId, @Param("userId") Integer userId);

    /* 2. [초대 발송] 알림 테이블에 초대장 저장 */
    @Insert("""
        INSERT INTO notification (receiverId, senderId, type, relId, isRead, regDate)
        VALUES (#{targetId}, #{myId}, 'PROJECT_INVITE', #{webId}, 0, NOW())
    """)
    void inviteMember(@Param("myId") Integer myId, @Param("targetId") Integer targetId, @Param("webId") Integer webId);

    /* 3. [초대 수락] 멤버 명단에 추가 (권한은 기본적으로 'EDITOR') */
    @Insert("""
        INSERT INTO userWeb_member (webId, userId, role, regDate)
        VALUES (#{webId}, #{userId}, 'EDITOR', NOW())
    """)
    void addMember(@Param("webId") Integer webId, @Param("userId") Integer userId);

    /* 4. [초대 완료] 처리된 알림 삭제 */
    @Delete("DELETE FROM notification WHERE id = #{notiId}")
    void deleteNotification(@Param("notiId") Integer notiId);

    @Select("SELECT userId FROM userWeb_member WHERE webId = #{webId}")
    java.util.List<Integer> selectMemberIds(Integer webId);

    // 이미 초대를 보낸 사람(수신자) ID 조회
    @Select("""
        SELECT receiverId 
        FROM notification 
        WHERE relId = #{webId} 
          AND type = 'PROJECT_INVITE'
    """)
    java.util.List<Integer> selectPendingInviteIds(Integer webId);

    @Select("""
        SELECT w.id, w.title, w.updateDate, m.role 
        FROM userWeb w
        JOIN userWeb_member m ON w.id = m.webId
        WHERE m.userId = #{userId}
        ORDER BY w.updateDate DESC
    """)
    java.util.List<java.util.Map<String, Object>> selectMyAllProjects(Integer userId);
    /**
    // ProjectDao.java

    /**
     * ✅ [탐색 페이지] 프로젝트 정보 + 'Home' 페이지의 HTML/CSS 코드까지 한 번에 조회
     */
/**
     * ✅ [탐색 페이지] 프로젝트 목록 조회 (최적화 + 정렬 고정)
     */
    @Select("""
        <script>
        SELECT 
            w.id, 
            w.title, 
            w.hit as views, 
            w.updateDate, 
            u.nickname as ownerNickname,
            w.preview_html as previewHtml 
        FROM userWeb w
        JOIN user u ON w.userId = u.id
        WHERE 1=1
        <if test='keyword != null and keyword != ""'>
            AND (w.title LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        
        -- 🔥 [핵심 수정] 정렬 기준 강화
        -- 1순위: 최근 수정된 순서 (updateDate DESC)
        -- 2순위: (시간이 같을 경우) 최신 글 ID 순서 (id DESC) -> 이걸 넣어야 순서가 안 바뀜!
        ORDER BY w.updateDate DESC, w.id DESC
        
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<ProjectExploreDto> selectExploreProjects(
        @Param("keyword") String keyword, 
        @Param("limit") int limit, 
        @Param("offset") int offset
    );

    // 1. 현재 멤버 수 세기
    @Select("SELECT COUNT(*) FROM userWeb_member WHERE webId = #{webId}")
    int countMembers(Integer webId);

    // 2. 아직 수락 안 한(대기 중인) 초대장 개수 세기
    @Select("""
        SELECT COUNT(*) 
        FROM notification 
        WHERE relId = #{webId} 
          AND type = 'PROJECT_INVITE'
    """)
    int countPendingInvites(Integer webId);

    // [추방] 멤버 삭제 쿼리
    @Delete("DELETE FROM userWeb_member WHERE webId = #{webId} AND userId = #{targetId}")
    void deleteMember(@Param("webId") Integer webId, @Param("targetId") Integer targetId);

    // [권한 확인] 특정 유저의 역할(Role) 조회
    @Select("SELECT role FROM userWeb_member WHERE webId = #{webId} AND userId = #{userId}")
    String getMemberRole(@Param("webId") Integer webId, @Param("userId") Integer userId);
    // ProjectDao.java 인터페이스 내부에 추가

    // 1. 프로젝트 제목만 가져오기
    @Select("SELECT title FROM userWeb WHERE id = #{webId}")
    String getProjectTitle(Integer webId);

    // 2. 미리보기 HTML만 가져오기 (복사 용도)
    @Select("SELECT preview_html FROM userWeb WHERE id = #{webId}")
    String getPreviewHtml(Integer webId);

    // ProjectDao.java
    @Update("UPDATE `userWeb` SET `hit` = `hit` + 1 WHERE `id` = #{webId}")
    void increaseHit(@Param("webId") Integer webId);
}