package org.reco.reco_sys.module.user.service;

import org.reco.reco_sys.module.user.dto.UpdateProfileRequest;
import org.reco.reco_sys.module.user.dto.UserProfileDto;

public interface UserService {
    UserProfileDto getProfile(Long userId);
    void updateProfile(Long userId, UpdateProfileRequest request);
}
