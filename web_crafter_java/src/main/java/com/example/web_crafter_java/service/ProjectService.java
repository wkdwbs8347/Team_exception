package com.example.web_crafter_java.service;

import com.example.web_crafter_java.dao.ProjectDao;
import com.example.web_crafter_java.dto.UserWeb;
import com.example.web_crafter_java.dto.UserWebPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    @Autowired
    private ProjectDao projectDao;

    @Transactional
    public Integer createProject(Integer userId) {
        // 1. userWeb 생성
        UserWeb web = new UserWeb();
        web.setUserId(userId);
        web.setTitle("Untitled Project");
        projectDao.insertUserWeb(web); 

        // 2. userWeb_pages 기본 생성
        UserWebPage page = new UserWebPage();
        page.setWebId(web.getId());
        page.setPageName("index");
        projectDao.insertUserWebPage(page);

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
public UserWebPage getProjectPageData(Integer webId) {
    // 1. DAO를 통해 JOIN된 데이터를 가져옵니다 (이미 DAO에 JOIN 쿼리가 있습니다)
    UserWebPage page = projectDao.getPageData(webId);

    // 2. 만약 데이터가 없다면 빈 객체를 생성하되, 이름만은 DB에서 다시 확인합니다.
    if (page == null) {
        page = new UserWebPage();
        page.setWebId(webId);
        // p.webId가 아닌 userWeb(w) 테이블의 제목을 가져오는 로직이 필요할 수 있습니다.
        // 하지만 DAO가 정상 작동한다면 page 객체 안에 이미 title이 들어있어야 합니다.
        page.setPageName("index");
        page.setLayoutData("<xml></xml>");
    }
    
    return page;
}
}