package org.reco.reco_sys.module.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {

    @NotNull
    private Long exerciseId;

    @NotBlank
    private String answer;

    private Integer timeSpent;
}
