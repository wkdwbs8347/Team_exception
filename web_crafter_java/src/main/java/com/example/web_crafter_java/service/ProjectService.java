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
}