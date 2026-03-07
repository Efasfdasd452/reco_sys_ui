package org.reco.reco_sys.module.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourseStudentDto {
    private Long userId;
    private String username;
    private String nickname;
}
