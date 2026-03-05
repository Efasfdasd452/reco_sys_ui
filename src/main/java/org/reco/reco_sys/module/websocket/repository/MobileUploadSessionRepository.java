package org.reco.reco_sys.module.websocket.repository;

import org.reco.reco_sys.module.websocket.entity.MobileUploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MobileUploadSessionRepository extends JpaRepository<MobileUploadSession, Long> {
    Optional<MobileUploadSession> findByToken(String token);

    @Modifying
    @Query("UPDATE MobileUploadSession m SET m.status = 'EXPIRED' WHERE m.expiresAt < :now AND m.status = 'PENDING'")
    void expireSessions(LocalDateTime now);
}
