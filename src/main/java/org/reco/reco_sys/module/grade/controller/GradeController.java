package org.reco.reco_sys.module.grade.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.grade.dto.GradeRequest;
import org.reco.reco_sys.module.grade.service.GradeService;
import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grade")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final JwtUtil jwtUtil;

    @GetMapping("/pending/course/{courseId}")
    public Result<List<AnswerRecordDto>> listPending(@PathVariable Long courseId) {
        return Result.success(gradeService.listPendingByCourse(courseId));
    }

    @PostMapping("/{recordId}")
    public Result<AnswerRecordDto> grade(@PathVariable Long recordId,
                                         @Valid @RequestBody GradeRequest request,
                                         @RequestHeader("Authorization") String token) {
        Long teacherId = jwtUtil.getUserId(extractToken(token));
        return Result.success(gradeService.grade(recordId, request, teacherId));
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
