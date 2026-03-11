package org.reco.reco_sys.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    /** Base32 密钥，用于手动输入到验证器App */
    private String totpSecret;
    /** otpauth:// URI，前端渲染成二维码 */
    private String totpQrUri;
}
