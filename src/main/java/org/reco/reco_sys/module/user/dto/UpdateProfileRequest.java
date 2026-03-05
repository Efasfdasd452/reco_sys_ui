package org.reco.reco_sys.module.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50)
    private String nickname;

    @Size(min = 6, max = 100)
    private String newPassword;

    private String currentPassword;
}
