package org.reco.reco_sys.module.classroom.repository;

import org.reco.reco_sys.module.classroom.entity.ClassroomCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassroomCourseRepository extends JpaRepository<ClassroomCourse, Long> {
    List<ClassroomCourse> findByClassroomId(Long classroomId);
    Optional<ClassroomCourse> findByClassroomIdAndCourseId(Long classroomId, Long courseId);
    boolean existsByClassroomIdAndCourseId(Long classroomId, Long courseId);
    void deleteByClassroomIdAndCourseId(Long classroomId, Long courseId);

    /** 查某课程被哪些班级关联 */
    @Query("SELECT cc.classroomId FROM ClassroomCourse cc WHERE cc.courseId = :courseId")
    List<Long> findClassroomIdsByCourseId(Long courseId);
}
