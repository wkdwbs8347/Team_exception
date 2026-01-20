package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.config.UserAdapter;
import com.example.web_crafter_java.service.ProjectService;

import jakarta.servlet.http.HttpSession;

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
public ResponseEntity<?> create(HttpSession session) {

    Integer memberId = (Integer) session.getAttribute("loginedMemberId");
    if (memberId == null) {
        return ResponseEntity.status(401).body("로그인이 필요합니다.");
    }

    Integer webId = projectService.createProject(memberId);
    return ResponseEntity.ok(webId);
}

// ProjectController.java 에 추가

    @PutMapping("/{projectId}/name")
    public ResponseEntity<?> updateName(
            @PathVariable Integer projectId,
            @RequestBody java.util.Map<String, String> body, // JSON의 { name: "..." }를 받음
            HttpSession session) {
        
        Integer memberId = (Integer) session.getAttribute("loginedMemberId");
        if (memberId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        String newName = body.get("name");
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("이름을 입력해주세요.");
        }

        // 서비스에서 프로젝트 이름 업데이트 로직 실행
        projectService.updateProjectName(projectId, memberId, newName);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{webId}/data")
    public ResponseEntity<?> getProjectData(@PathVariable Integer webId) {
        try {
            // 서비스에서 null 체크가 포함된 데이터를 가져옵니다.
            com.example.web_crafter_java.dto.UserWebPage data = projectService.getProjectPageData(webId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            // 500 에러 발생 시 원인을 파악하기 위해 로그를 남깁니다.
            e.printStackTrace(); 
            return ResponseEntity.status(500).body("데이터 조회 실패");
        }
    }
}