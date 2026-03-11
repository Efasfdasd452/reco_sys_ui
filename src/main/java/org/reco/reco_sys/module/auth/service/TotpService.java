package org.reco.reco_sys.module.auth.service;

public interface TotpService {
    /** 生成新的 Base32 密钥 */
    String generateSecret();

    /** 生成 otpauth:// URI，供前端渲染为二维码 */
    String buildQrUri(String secret, String username);

    /** 验证用户输入的 6 位 TOTP 码是否正确 */
    boolean verifyCode(String secret, String code);
}
