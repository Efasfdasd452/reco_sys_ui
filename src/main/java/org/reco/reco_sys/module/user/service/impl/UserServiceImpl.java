package org.reco.reco_sys.module.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.module.user.dto.LoginRecordDto;
import org.reco.reco_sys.module.user.dto.UpdateProfileRequest;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.reco.reco_sys.module.user.entity.LoginRecord;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.LoginRecordRepository;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.reco.reco_sys.module.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRecordRepository loginRecordRepository;

    @Override
    public UserProfileDto getProfile(Long userId) {
        SysUser user = getUser(userId);
        return toDto(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = getUser(userId);
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getNewPassword() != null) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        userRepository.save(user);
    }

    private SysUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
    }

    @Override
    public Page<LoginRecordDto> getLoginRecords(Long userId, int page, int size) {
        return loginRecordRepository
                .findByUserIdOrderByLoginAtDesc(userId, PageRequest.of(page, size))
                .map(this::toLoginDto);
    }

    private LoginRecordDto toLoginDto(LoginRecord record) {
        LoginRecordDto dto = new LoginRecordDto();
        dto.setId(record.getId());
        dto.setIpAddress(record.getIpAddress());
        dto.setLocation(record.getLocation());
        dto.setUserAgent(record.getUserAgent());
        dto.setIsAnomaly(record.getIsAnomaly());
        dto.setLoginAt(record.getLoginAt());
        return dto;
    }

    public UserProfileDto toDto(SysUser user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
