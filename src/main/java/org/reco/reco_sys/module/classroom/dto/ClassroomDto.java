package org.reco.reco_sys.module.classroom.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClassroomDto {
    private Long id;
    private String name;
    private String description;
    private Long teacherId;
    private String teacherName;
    private String inviteCode;
    private Boolean isActive;
    private Integer studentCount;
    private LocalDateTime createdAt;
    private List<StudentItem> students;
    private List<CourseItem> courses;

    @Data
    public static class StudentItem {
        private Long userId;
        private String username;
        private String nickname;
        private LocalDateTime joinedAt;
    }

    @Data
    public static class CourseItem {
        private Long courseId;
        private String courseName;
        private String description;
        private String inviteCode;
        private LocalDateTime addedAt;
    }
}
