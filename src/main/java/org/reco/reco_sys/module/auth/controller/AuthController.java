package org.reco.reco_sys.module.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.IpUtil;
import org.reco.reco_sys.module.auth.dto.*;
import org.reco.reco_sys.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final IpUtil ipUtil;

    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailRequest request) {
        authService.sendEmailCode(request);
        return Result.success(null);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success(null);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        String ip = ipUtil.getRealIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return Result.success(authService.login(request, ip, userAgent));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success(null);
    }
}
