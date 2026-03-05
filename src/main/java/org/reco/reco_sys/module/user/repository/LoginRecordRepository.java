package org.reco.reco_sys.module.user.repository;

import org.reco.reco_sys.module.user.entity.LoginRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginRecordRepository extends JpaRepository<LoginRecord, Long> {
    Page<LoginRecord> findByUserIdOrderByLoginAtDesc(Long userId, Pageable pageable);
}
