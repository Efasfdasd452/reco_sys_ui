package org.reco.reco_sys.module.classroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 班级-课程关联：记录哪个班级开了哪些课
 */
@Data
@Entity
@Table(name = "classroom_course",
       uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id", "course_id"}))
public class ClassroomCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;
}
