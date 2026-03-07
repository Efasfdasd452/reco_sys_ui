package org.reco.reco_sys.module.classroom.repository;

import org.reco.reco_sys.module.classroom.entity.ClassroomStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassroomStudentRepository extends JpaRepository<ClassroomStudent, Long> {
    List<ClassroomStudent> findByClassroomId(Long classroomId);
    List<ClassroomStudent> findByUserId(Long userId);
    Optional<ClassroomStudent> findByClassroomIdAndUserId(Long classroomId, Long userId);
    boolean existsByClassroomIdAndUserId(Long classroomId, Long userId);

    @Query("SELECT cs.classroomId FROM ClassroomStudent cs WHERE cs.userId = :userId")
    List<Long> findClassroomIdsByUserId(Long userId);
}
