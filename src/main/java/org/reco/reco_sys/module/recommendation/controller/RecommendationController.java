package org.reco.reco_sys.module.recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.recommendation.dto.RecommendResponse;
import org.reco.reco_sys.module.recommendation.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;

    @GetMapping("/course/{courseId}")
    public Result<RecommendResponse> getLatest(@PathVariable Long courseId,
                                                @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(recommendationService.getLatest(userId, courseId));
    }

    @PostMapping("/course/{courseId}/refresh")
    public Result<RecommendResponse> refresh(@PathVariable Long courseId,
                                              @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(recommendationService.recommend(userId, courseId));
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
