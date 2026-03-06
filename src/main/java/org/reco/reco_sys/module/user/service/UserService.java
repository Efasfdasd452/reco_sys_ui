package org.reco.reco_sys.module.user.service;

import org.reco.reco_sys.module.user.dto.LoginRecordDto;
import org.reco.reco_sys.module.user.dto.UpdateProfileRequest;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.springframework.data.domain.Page;

public interface UserService {
    UserProfileDto getProfile(Long userId);
    void updateProfile(Long userId, UpdateProfileRequest request);
    Page<LoginRecordDto> getLoginRecords(Long userId, int page, int size);
}
