package org.reco.reco_sys.module.admin.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.admin.service.AdminService;
import org.reco.reco_sys.module.admin.service.DataInitService;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final DataInitService dataInitService;
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
     * 将 algebra2005 Q矩阵导入指定课程（112知识点 + 1084习题）。
     * 幂等：已存在的数据会跳过。
     * @param courseId 目标课程 ID
     */
    @PostMapping("/init-dataset")
    public Result<Map<String, Object>> initDataset(
            @RequestParam Long courseId,
            @RequestHeader("Authorization") String token) {
        Long adminUserId = jwtUtil.getUserId(extractToken(token));
        try {
            return Result.success(dataInitService.importAlgebra2005(courseId, adminUserId));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "读取数据集失败：" + e.getMessage());
        }
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
