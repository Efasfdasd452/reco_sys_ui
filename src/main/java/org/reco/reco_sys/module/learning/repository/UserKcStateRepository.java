package org.reco.reco_sys.module.learning.repository;

import org.reco.reco_sys.module.learning.entity.UserKcState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserKcStateRepository extends JpaRepository<UserKcState, Long> {
    List<UserKcState> findByUserId(Long userId);
    Optional<UserKcState> findByUserIdAndKpId(Long userId, Long kpId);
}
