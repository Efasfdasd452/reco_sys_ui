package org.reco.reco_sys.module.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassroomCourseRequest {
    @NotNull
    private Long courseId;
}
