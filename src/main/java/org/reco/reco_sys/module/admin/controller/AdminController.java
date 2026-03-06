package org.reco.reco_sys.module.admin.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.admin.service.AdminService;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    @GetMapping("/users")
    public Result<List<UserProfileDto>> listUsers() {
        return Result.success(adminService.listUsers());
    }

    @PutMapping("/users/{userId}/role")
    public Result<Void> setRole(@PathVariable Long userId, @RequestParam String role) {
        adminService.setUserRole(userId, role);
        return Result.success(null);
    }

    /**
     * 从 Python 推荐服务初始化 KG4Ex 数据（112知识点 + 1084习题）。
     * 幂等：重复调用安全，已初始化时直接返回。
     */
    @PostMapping("/init-python-data")
    public Result<Map<String, Object>> initPythonData(
            @RequestHeader("Authorization") String token) {
        Long adminUserId = jwtUtil.getUserId(extractToken(token));
        return Result.success(adminService.initPythonData(adminUserId));
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
