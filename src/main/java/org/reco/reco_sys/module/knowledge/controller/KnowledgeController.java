package org.reco.reco_sys.module.knowledge.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.knowledge.dto.GraphDto;
import org.reco.reco_sys.module.knowledge.dto.KnowledgePointDto;
import org.reco.reco_sys.module.knowledge.service.KnowledgeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final JwtUtil jwtUtil;

    @GetMapping("/course/{courseId}")
    public Result<List<KnowledgePointDto>> listByCourse(@PathVariable Long courseId) {
        return Result.success(knowledgeService.listByCourse(courseId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<KnowledgePointDto> create(@RequestBody KnowledgePointDto dto,
                                            @RequestHeader("Authorization") String token) {
        Long teacherId = jwtUtil.getUserId(extractToken(token));
        return Result.success(knowledgeService.create(dto, teacherId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/relation")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<Void> addRelation(@RequestParam Long fromId,
                                    @RequestParam Long toId,
                                    @RequestParam String type) {
        knowledgeService.addRelation(fromId, toId, type);
        return Result.success(null);
    }

    @GetMapping("/graph/{courseId}")
    public Result<GraphDto> getMyGraph(@PathVariable Long courseId,
                                       @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(knowledgeService.getGraphForStudent(courseId, userId));
    }

    @GetMapping("/graph/{courseId}/user/{userId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<GraphDto> getUserGraph(@PathVariable Long courseId,
                                         @PathVariable Long userId) {
        return Result.success(knowledgeService.getGraphForTeacher(courseId, userId));
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
