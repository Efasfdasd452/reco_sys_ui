package org.reco.reco_sys.module.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.auth.dto.RegisterResponse;
import org.reco.reco_sys.module.auth.service.AuthService;
import org.reco.reco_sys.module.user.dto.LoginRecordDto;
import org.reco.reco_sys.module.user.dto.UpdateProfileRequest;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.reco.reco_sys.module.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

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

    @GetMapping("/login-records")
    public Result<Page<LoginRecordDto>> loginRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(userService.getLoginRecords(userId, page, size));
    }

    /** 获取当前用户的 TOTP 绑定信息（查看，不重新生成） */
    @GetMapping("/totp-setup")
    public Result<RegisterResponse> getTotpSetup(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(authService.getTotpSetup(userId));
    }

    /** 重新生成 TOTP 密钥（旧密钥立即失效） */
    @PostMapping("/totp-reset")
    public Result<RegisterResponse> resetTotp(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(authService.resetTotp(userId));
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
