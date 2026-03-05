package org.reco.reco_sys.module.user.repository;

import org.reco.reco_sys.module.user.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndTypeAndIsUsedFalseOrderByCreatedAtDesc(
            String email, EmailVerification.Type type);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}
