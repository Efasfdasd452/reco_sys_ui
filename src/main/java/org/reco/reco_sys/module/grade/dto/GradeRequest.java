package org.reco.reco_sys.module.grade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeRequest {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    private String teacherComment;
}
