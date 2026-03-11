package org.reco.reco_sys.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetByTotpRequest {

    @NotBlank
    private String username;

    /** 验证器App 当前显示的 6 位数字 */
    @NotBlank
    @Size(min = 6, max = 6)
    private String totpCode;

    @NotBlank
    @Size(min = 6)
    private String newPassword;
}
