package org.reco.reco_sys.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendEmailRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String type;
}
