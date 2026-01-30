package com.example.web_crafter_java.controller;

import com.example.web_crafter_java.config.UserAdapter;
import com.example.web_crafter_java.dto.UserWebPage;
import com.example.web_crafter_java.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

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
    // ProjectController.java

    // 🔥 [수정] 프로젝트 데이터 저장 API (previewHtml 추가 수신)
    @PutMapping("/{webId}/data")
    public ResponseEntity<?> updateProjectData(
            @PathVariable Integer webId,
            @RequestParam String oldPageName,
            @RequestBody Map<String, Object> payload, // 👈 DTO 대신 Map으로 받아서 유연하게 처리
            HttpSession session) {
        
        Integer memberId = (Integer) session.getAttribute("loginedMemberId");
        if (memberId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            // 1. Payload에서 데이터 추출
            // Jackson ObjectMapper 등을 써서 UserWebPage로 변환하는 게 정석이지만, 
            // 여기선 간단히 Map에서 꺼내서 세팅한다고 가정합니다.
            // (실제로는 ObjectMapper로 변환하거나 프론트에서 구조를 맞춰 보내야 함)
            
            // 편의상 DTO 변환 로직 (간략화)
            ObjectMapper mapper = new ObjectMapper();
            UserWebPage pageData = mapper.convertValue(payload.get("pageData"), UserWebPage.class);
            String previewHtml = (String) payload.get("previewHtml"); // 🔥 HTML 받기

            pageData.setWebId(webId);
            
            // 2. 서비스 호출 (인자 4개)
            projectService.updateProjectData(webId, oldPageName, pageData, previewHtml);
            
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
// 1. [조회] 페이지 목록 가져오기 API (F12의 GET .../pages 500 에러 해결)
@GetMapping("/{webId}/pages") 
public ResponseEntity<?> getPageList(@PathVariable Integer webId) {
    try {
        java.util.List<com.example.web_crafter_java.dto.UserWebPage> pages = projectService.getPageList(webId);
        return ResponseEntity.ok(pages);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("목록 조회 실패: " + e.getMessage());
    }
}

// 2. [생성] 새 페이지 만들기 API (원래 이름에 맞는 기능)
@PostMapping("/{webId}/pages")
public ResponseEntity<?> createNewPage(
        @PathVariable Integer webId,
        @RequestBody com.example.web_crafter_java.dto.UserWebPage pageData,
        HttpSession session) {
    
    Integer memberId = (Integer) session.getAttribute("loginedMemberId");
    if (memberId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

    try {
        // 실제 저장 로직 수행 (Service에 구현되어 있어야 함)
        projectService.insertNewPage(pageData); 
        return ResponseEntity.ok("페이지 생성 성공");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("페이지 생성 실패: " + e.getMessage());
    }
}

// 3. [초대 발송] 친구에게 초대장 보내기
    @PostMapping("/invite")
    public ResponseEntity<?> inviteUser(@RequestBody java.util.Map<String, Integer> body, HttpSession session) {
        Integer myId = (Integer) session.getAttribute("loginedMemberId");
        if (myId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        Integer targetId = body.get("targetId"); // 친구 ID
        Integer webId = body.get("webId");       // 프로젝트 ID

        try {
            projectService.inviteUser(myId, targetId, webId);
            return ResponseEntity.ok("초대장을 보냈습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. [초대 수락] 알림을 보고 수락 버튼 누름
    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvite(@RequestBody java.util.Map<String, Integer> body, HttpSession session) {
        Integer myId = (Integer) session.getAttribute("loginedMemberId");
        if (myId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        Integer notiId = body.get("notiId"); // 알림 ID (삭제용)
        Integer webId = body.get("webId");   // 들어갈 프로젝트 ID

        try {
            projectService.acceptInvite(myId, notiId, webId);
            return ResponseEntity.ok("수락되었습니다. 프로젝트 멤버가 되었습니다!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("수락 실패: " + e.getMessage());
        }
    }

    // 5. [초대 거절] 알림만 삭제 (멤버 추가 X)
    @PostMapping("/reject")
    public ResponseEntity<?> rejectInvite(@RequestBody java.util.Map<String, Integer> body, HttpSession session) {
        Integer myId = (Integer) session.getAttribute("loginedMemberId");
        if (myId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        Integer notiId = body.get("notiId"); // 알림 ID
        
        try {
            projectService.rejectInvite(notiId);
            return ResponseEntity.ok("초대를 거절했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("거절 실패");
        }
    }

    @GetMapping("/{webId}/members")
    public ResponseEntity<?> getProjectMemberIds(@PathVariable Integer webId) {
        try {
            java.util.List<Integer> memberIds = projectService.getProjectMemberIds(webId);
            return ResponseEntity.ok(memberIds);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("멤버 조회 실패");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getMyProjects(HttpSession session) {
        Integer myId = (Integer) session.getAttribute("loginedMemberId");
        if (myId == null) return ResponseEntity.status(401).body("로그인 필요");

        return ResponseEntity.ok(projectService.getMyAllProjects(myId));
    }
    // ProjectController.java 내부

    // (초대 대기 멤버 조회)
    @GetMapping("/{webId}/pending-invites")
    public ResponseEntity<?> getPendingInviteIds(@PathVariable Integer webId) {
        try {
            java.util.List<Integer> pendingIds = projectService.getPendingInviteIds(webId);
            return ResponseEntity.ok(pendingIds);
        } catch (Exception e) {
            // 에러가 나도 빈 리스트를 줘서 프론트가 멈추지 않게 함
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

        // ✅ [탐색] 모든 프로젝트 조회 API
        @GetMapping("/explore")
        public ResponseEntity<List<com.example.web_crafter_java.dto.ProjectExploreDto>> getExploreProjects(
                @RequestParam(required = false) String keyword, 
                @RequestParam(defaultValue = "0") int page,     
                @RequestParam(defaultValue = "20") int size     
        ) {
            try {
                // Service 호출
                List<com.example.web_crafter_java.dto.ProjectExploreDto> projects = 
                    projectService.getExploreProjects(keyword, page, size);
                
                return ResponseEntity.ok(projects);
            } catch (Exception e) {
                e.printStackTrace(); // 서버 콘솔에 에러 로그 출력 (디버깅용)
                return ResponseEntity.status(500).build();
            }
        }

    // 🔥 [추방 API] 방장이 멤버 내보내기
    @DeleteMapping("/{webId}/members/{memberId}")
    public ResponseEntity<?> kickMember(
            @PathVariable Integer webId,
            @PathVariable Integer memberId,
            HttpSession session) {
        
        Integer myId = (Integer) session.getAttribute("loginedMemberId");
        if (myId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        try {
            projectService.kickMember(myId, webId, memberId);
            return ResponseEntity.ok("멤버를 추방했습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // ProjectController.java 내부에 추가

    // 🔥 [신규] 프로젝트 리메이크 (복제) API
    @PostMapping("/{webId}/remake")
    public ResponseEntity<?> remakeProject(
            @PathVariable Integer webId, // 원본 프로젝트 ID
            HttpSession session
    ) {
        Integer memberId = (Integer) session.getAttribute("loginedMemberId");
        if (memberId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            // 서비스 호출 -> 복제된 새 프로젝트 ID 반환
            Integer newWebId = projectService.remakeProject(webId, memberId);
            return ResponseEntity.ok(newWebId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("프로젝트 복제 실패: " + e.getMessage());
        }
    }
    // ProjectController.java

    @PatchMapping("/hit/{webId}")
    public ResponseEntity<Void> updateHit(@PathVariable("webId") Integer webId) { // 👈 ("webId") 추가!!
        projectService.updateHit(webId);
        return ResponseEntity.ok().build();
    }
}