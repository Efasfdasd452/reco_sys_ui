package org.reco.reco_sys.module.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.common.util.IpUtil;
import org.reco.reco_sys.module.auth.captcha.CaptchaService;
import org.reco.reco_sys.module.auth.dto.*;
import org.reco.reco_sys.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final IpUtil ipUtil;
    private final CaptchaService captchaService;

    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailRequest request) {
        authService.sendEmailCode(request);
        return Result.success(null);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        if (!captchaService.verifyPassToken(request.getCaptchaPassToken())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "滑块验证已过期，请重新验证");
        }
        authService.register(request);
        return Result.success(null);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        if (!captchaService.verifyPassToken(request.getCaptchaPassToken())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "滑块验证已过期，请重新验证");
        }
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
