package org.reco.reco_sys.module.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.learning.dto.AnswerRecordDto;
import org.reco.reco_sys.module.learning.dto.SubmitAnswerRequest;
import org.reco.reco_sys.module.learning.service.LearningService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;
    private final JwtUtil jwtUtil;

    @PostMapping("/submit")
    public Result<AnswerRecordDto> submit(@Valid @RequestBody SubmitAnswerRequest request,
                                          @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(learningService.submitAnswer(userId, request));
    }

    @GetMapping("/history")
    public Result<Page<AnswerRecordDto>> history(@RequestHeader("Authorization") String token,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(learningService.myHistory(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/answered-ids")
    public Result<List<Long>> answeredIds(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(learningService.getAnsweredExerciseIds(userId));
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
