package org.reco.reco_sys.module.admin.service;

import org.reco.reco_sys.module.user.dto.UserProfileDto;

import java.util.List;
import java.util.Map;

public interface AdminService {
    List<UserProfileDto> listUsers();
    void setUserRole(Long userId, String role);
    /** 从 Python 推荐服务导入知识点和习题数据（方向A数据初始化） */
    Map<String, Object> initPythonData(Long adminUserId);
}
