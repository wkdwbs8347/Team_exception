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
        public ResponseEntity<?> getProjectData(
                @PathVariable Integer webId,
                // 1. URL 파라미터에서 pageName을 읽어옵니다. (없으면 기본값 "index")
                @RequestParam(value = "pageName") String pageName
        ) {
            try {
                // 2. 이제 정의된 pageName 변수를 서비스에 넘겨줄 수 있습니다.
                com.example.web_crafter_java.dto.UserWebPage data = projectService.getProjectPageData(webId, pageName); 
                return ResponseEntity.ok(data);
            } catch (Exception e) {
                e.printStackTrace(); 
                return ResponseEntity.status(500).body("데이터 조회 실패");
            }
        }

    // 🔥 [신규] 프로젝트 데이터 저장 API
    @PutMapping("/{webId}/data")
    public ResponseEntity<?> updateProjectData(
            @PathVariable Integer webId,
            @RequestParam String oldPageName,
            @RequestBody com.example.web_crafter_java.dto.UserWebPage pageData,
            HttpSession session) {
        
        Integer memberId = (Integer) session.getAttribute("loginedMemberId");
        if (memberId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            pageData.setWebId(webId);
            projectService.updateProjectData(webId, oldPageName ,pageData);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("데이터 저장 실패");
        }
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable Integer projectId, HttpSession session) {
        // 보안을 위해 세션 체크 추가 [cite: 2026-01-16]
        
        Integer memberId = (Integer) session.getAttribute("loginedMemberId");
        if (memberId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            // projectDao 대신 projectService를 호출합니다. [cite: 2026-01-21]
            projectService.deleteProject(projectId, memberId); 
            return ResponseEntity.ok().body("프로젝트가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("삭제 실패: " + e.getMessage());
        }
    }

    // ProjectController.java 수정
@PostMapping("/{webId}/pages")
public ResponseEntity<?> createNewPage(
        @PathVariable Integer webId,
        @RequestBody com.example.web_crafter_java.dto.UserWebPage pageData,
        HttpSession session) {
    
    Integer memberId = (Integer) session.getAttribute("loginedMemberId");
    if (memberId == null) {
        return ResponseEntity.status(401).body("로그인이 필요합니다.");
    }

   try {
            // 서비스에서 목록 가져오기 (Service, DAO, Mapper가 준비되어 있어야 함)
            java.util.List<com.example.web_crafter_java.dto.UserWebPage> pages = projectService.getPageList(webId);
            
            // 목록 반환 (프론트엔드의 allPages 변수로 들어갑니다)
            return ResponseEntity.ok(pages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("목록 조회 실패: " + e.getMessage());
        }
}

}