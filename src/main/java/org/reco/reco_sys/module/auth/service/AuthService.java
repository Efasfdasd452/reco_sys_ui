package org.reco.reco_sys.module.auth.service;

import org.reco.reco_sys.module.auth.dto.*;

public interface AuthService {
    void sendEmailCode(SendEmailRequest request);
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request, String ip, String userAgent);
    void resetPassword(ResetPasswordRequest request);
    void resetPasswordByTotp(ResetByTotpRequest request);
    /** 返回当前用户的 TOTP 绑定信息（不重新生成） */
    RegisterResponse getTotpSetup(Long userId);

    /** 强制重新生成 TOTP 密钥，返回新的绑定信息 */
    RegisterResponse resetTotp(Long userId);
}
