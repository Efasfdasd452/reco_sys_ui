package org.reco.reco_sys.module.admin.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.module.admin.service.AdminService;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<List<UserProfileDto>> listUsers() {
        return Result.success(adminService.listUsers());
    }

    @PutMapping("/users/{userId}/role")
    public Result<Void> setRole(@PathVariable Long userId, @RequestParam String role) {
        adminService.setUserRole(userId, role);
        return Result.success(null);
    }

    @PostMapping("/retrain")
    public Result<Void> retrain() {
        adminService.triggerRetrain();
        return Result.success(null);
    }
}
