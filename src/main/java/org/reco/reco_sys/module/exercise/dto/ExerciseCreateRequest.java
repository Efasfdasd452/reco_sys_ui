package org.reco.reco_sys.module.exercise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExerciseCreateRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    private String type;

    @NotBlank
    private String content;

    private String answerKey;

    private String difficulty = "MEDIUM";

    private List<Long> knowledgePointIds;
}
