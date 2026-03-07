package org.reco.reco_sys.module.classroom.repository;

import org.reco.reco_sys.module.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findByTeacherIdAndIsActiveTrue(Long teacherId);
    Optional<Classroom> findByInviteCode(String inviteCode);
}
