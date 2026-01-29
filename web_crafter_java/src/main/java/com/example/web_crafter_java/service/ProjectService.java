package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.ProjectDao;
import com.example.web_crafter_java.dto.UserWeb;
import com.example.web_crafter_java.dto.UserWebPage;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.web_crafter_java.dto.ProjectExploreDto;
@Service
public class ProjectService {
    @Autowired
    private ProjectDao projectDao;

    @Transactional
public Integer createProject(Integer userId) {
    // 1. userWeb 생성 (프로젝트 본체)
    UserWeb web = new UserWeb();
    web.setUserId(userId);
    web.setTitle("Untitled Project");
    projectDao.insertUserWeb(web); 

    // 2. 초기 페이지들 생성 (Home, Login) [cite: 2026-01-21]
    // 💡 더 이상 "index" 하나만 만들지 않고, 필요한 페이지들을 각각 생성합니다.
    String[] initialPages = {"Home", "Login"};
    
    for (String pageName : initialPages) {
        UserWebPage page = new UserWebPage();
        page.setWebId(web.getId());
        page.setPageName(pageName); // Home 한번, Login 한번 [cite: 2026-01-21]
        
        // 초기 XML 데이터가 필요하다면 설정 (기본값)
        page.setLayoutData("<xml xmlns=\"https://developers.google.com/blockly/xml\"></xml>");
        page.setStyleData("{}");
        page.setLogicData("{}");
        
        projectDao.insertUserWebPage(page);
    }

    // 3. userWeb_member 방장 등록
    projectDao.insertProjectMember(web.getId(), userId, "OWNER");

    return web.getId();
}

    @Transactional
    public void updateProjectName(Integer projectId, Integer userId, String newName) {
        // 1. 해당 프로젝트의 소유주가 맞는지 확인하는 로직 (선택 사항이지만 보안상 추천)
        // 2. 이름 업데이트 수행
        projectDao.updateTitle(projectId, newName);
    }

// ProjectService.java 수정
public UserWebPage getProjectPageData(Integer webId, String pageName) {
    // 1. DAO를 통해 JOIN된 데이터를 가져옵니다 (이미 DAO에 JOIN 쿼리가 있습니다)
    UserWebPage page = projectDao.getPageData(webId, pageName);

    // 2. 만약 데이터가 없다면 빈 객체를 생성하되, 이름만은 DB에서 다시 확인합니다.
        if (page == null) {
        throw new IllegalArgumentException("존재하지 않는 페이지입니다.");
        }
    
    return page;
}

// 🔥 [신규] JSON 데이터 저장 메서드 추가
@Transactional
public void updateProjectData(Integer webId, String oldPageName ,UserWebPage pageData) {

    if ("mypage".equals(oldPageName)) {
        System.out.println("❌ mypage 저장 차단");
        return;
    }

    projectDao.updatePageData(webId, oldPageName, pageData);
}

public void deleteProject(Integer projectId, Integer memberId) {
    // 1. (선택사항) 삭제 권한 체크 로직 [cite: 2026-01-16]
    // 예: 이 프로젝트의 소유자가 memberId와 일치하는지 확인하는 로직을 여기에 넣을 수 있습니다.

    // 2. DAO 호출하여 DB 삭제 실행 [cite: 2026-01-21]
    projectDao.deleteProject(projectId);
}

public void insertNewPage(com.example.web_crafter_java.dto.UserWebPage pageData) {
    projectDao.insertNewPage(pageData); // DAO에 있는 INSERT 쿼리를 실행합니다.
}

// ✅ 페이지 목록 조회 서비스
    public java.util.List<UserWebPage> getPageList(Integer webId) {
        return projectDao.selectPagesByWebId(webId);
    }

    public void createPage(Integer webId, UserWebPage pageData) {
    pageData.setWebId(webId); // webId 세팅해주고
    this.insertNewPage(pageData); // 이미 있는 insertNewPage 실행!
}

public void inviteUser(Integer myId, Integer targetId, Integer webId) {
        // 1. 유효성 검사: 본인을 초대할 순 없음
        if (myId.equals(targetId)) {
            throw new RuntimeException("본인은 초대할 수 없습니다.");
        }

        // 2. 이미 멤버인지 확인 (DAO 호출)
        // (만약 DAO에 isMember가 없다면 추가해야 합니다)
        if (projectDao.isMember(webId, targetId) > 0) {
            throw new RuntimeException("이미 이 프로젝트의 멤버입니다.");
        }

        // 3. 초대장(알림) 발송
        projectDao.inviteMember(myId, targetId, webId);
    }

    /**
     * [초대 수락] 알림을 통해 프로젝트 멤버로 정식 등록합니다.
     * 트랜잭션 필수: 멤버 추가와 알림 삭제가 동시에 이루어져야 함
     */
    @Transactional
    public void acceptInvite(Integer myId, Integer notiId, Integer webId) {
        // 1. 멤버 테이블에 추가 (권한: EDITOR)
        projectDao.addMember(webId, myId);

        // 2. 처리된 알림 삭제 (더 이상 알림창에 안 뜨게)
        projectDao.deleteNotification(notiId);
    }

    public java.util.List<Integer> getProjectMemberIds(Integer webId) {
        return projectDao.selectMemberIds(webId);
    }

    // 초대 대기 중인 ID 목록
    public java.util.List<Integer> getPendingInviteIds(Integer webId) {
        return projectDao.selectPendingInviteIds(webId);
    }
    
    
    // =========================================================
    // 🔥 [핵심 수정] 탐색 페이지 로직 (MyBatis 버전으로 완벽 교체)
    // =========================================================
    @Transactional(readOnly = true)
    public List<ProjectExploreDto> getExploreProjects(String keyword, int page, int size) {
        
        // 1. MyBatis용 페이징 계산 (Offset = 페이지번호 * 개수)
        int offset = page * size;

        // 2. DAO 호출 (JPA Repository 아님!)
        List<ProjectExploreDto> projects = projectDao.selectExploreProjects(keyword, size, offset);

        // 3. 데이터 후처리 (Null 방지)
        if (projects != null) {
            for (ProjectExploreDto p : projects) {
                // DB에 태그가 없으므로 빈 리스트로 설정 (안 하면 프론트에서 에러남)
                if (p.getTechTags() == null) {
                    p.setTechTags(Collections.emptyList());
                }
            }
        } else {
            return Collections.emptyList();
        }

        return projects;
    }

    // 1. 내 프로젝트 전체 목록 조회 (대시보드용)
    public java.util.List<java.util.Map<String, Object>> getMyAllProjects(Integer userId) {
        return projectDao.selectMyAllProjects(userId);
    }

    // 2. 초대 거절 (알림 삭제)
    public void rejectInvite(Integer notiId) {
        projectDao.deleteNotification(notiId);
    }
}