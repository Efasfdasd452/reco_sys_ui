package org.reco.reco_sys.module.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassroomCreateRequest {
    @NotBlank(message = "班级名称不能为空")
    private String name;
    private String description;
}
