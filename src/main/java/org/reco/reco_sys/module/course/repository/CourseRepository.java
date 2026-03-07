package org.reco.reco_sys.module.course.repository;

import org.reco.reco_sys.module.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByIsActiveTrue();
    List<Course> findByTeacherId(Long teacherId);
    Optional<Course> findByInviteCode(String inviteCode);
}
