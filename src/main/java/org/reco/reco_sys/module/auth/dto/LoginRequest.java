package org.reco.reco_sys.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    /** 滑块验证码通行证（/api/auth/captcha/verify 成功后返回的 passToken） */
    @NotBlank(message = "请先完成滑块验证")
    private String captchaPassToken;
}
