package org.reco.reco_sys.module.auth.service;

import org.reco.reco_sys.module.auth.dto.*;

public interface AuthService {
    void sendEmailCode(SendEmailRequest request);
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request, String ip, String userAgent);
    void resetPassword(ResetPasswordRequest request);
}
