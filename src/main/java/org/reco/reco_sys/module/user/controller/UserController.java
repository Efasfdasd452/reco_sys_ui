package org.reco.reco_sys.module.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.user.dto.UpdateProfileRequest;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.reco.reco_sys.module.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/profile")
    public Result<UserProfileDto> getProfile(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestHeader("Authorization") String token,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        userService.updateProfile(userId, request);
        return Result.success(null);
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
