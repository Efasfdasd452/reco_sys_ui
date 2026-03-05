package org.reco.reco_sys.module.user.repository;

import org.reco.reco_sys.module.user.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    Optional<SysUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
