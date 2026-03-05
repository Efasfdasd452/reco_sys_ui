package org.reco.reco_sys.module.admin.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.config.AppProperties;
import org.reco.reco_sys.module.admin.service.AdminService;
import org.reco.reco_sys.module.user.dto.UserProfileDto;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.reco.reco_sys.module.user.service.impl.UserServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final SysUserRepository userRepository;
    private final UserServiceImpl userServiceImpl;
    private final AppProperties appProperties;

    @Override
    public List<UserProfileDto> listUsers() {
        return userRepository.findAll().stream()
                .map(userServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setUserRole(Long userId, String role) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        user.setRole(SysUser.Role.valueOf(role));
        userRepository.save(user);
    }

    @Override
    public void triggerRetrain() {
        try {
            RestClient client = RestClient.create();
            client.post()
                    .uri(appProperties.getRecommendServiceUrl() + "/api/v1/retrain")
                    .header("X-API-Key", appProperties.getRecommendServiceApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .toBodilessEntity();
            log.info("触发推荐模型重训练成功");
        } catch (Exception e) {
            log.error("触发重训练失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.RECOMMEND_SERVICE_ERROR);
        }
    }
}
