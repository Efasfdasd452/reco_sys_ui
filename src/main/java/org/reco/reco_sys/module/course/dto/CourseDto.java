package org.reco.reco_sys.module.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDto {
    private Long id;
    private String name;
    private String description;
    private Long teacherId;
    private String teacherName;
    private Boolean isEnrolled;
    private LocalDateTime createdAt;
}
