package org.reco.reco_sys.module.course.repository;

import org.reco.reco_sys.module.course.entity.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    Optional<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    List<UserCourse> findByUserId(Long userId);

    @Query("SELECT uc.userId FROM UserCourse uc WHERE uc.courseId = :courseId")
    List<Long> findUserIdsByCourseId(Long courseId);
}
