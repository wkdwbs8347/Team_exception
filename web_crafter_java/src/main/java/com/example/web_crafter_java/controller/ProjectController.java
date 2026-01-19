package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.config.UserAdapter;
import com.example.web_crafter_java.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    
    @Autowired
    private ProjectService projectService;

@PostMapping("/create")
public ResponseEntity<Integer> create() {  // ← @AuthenticationPrincipal 완전 제거
    System.out.println("========================================");
    System.out.println("프로젝트 생성 API 호출됨!");
    System.out.println("========================================");
    
    Integer userId = 1;  // 테스트용 하드코딩
    Integer webId = projectService.createProject(userId);
    
    System.out.println("생성된 webId: " + webId);
    
    return ResponseEntity.ok(webId);
}
}