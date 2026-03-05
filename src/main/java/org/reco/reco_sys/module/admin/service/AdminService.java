package org.reco.reco_sys.module.admin.service;

import org.reco.reco_sys.module.user.dto.UserProfileDto;

import java.util.List;

public interface AdminService {
    List<UserProfileDto> listUsers();
    void setUserRole(Long userId, String role);
    void triggerRetrain();
}
