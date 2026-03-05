package org.reco.reco_sys.module.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;
}
