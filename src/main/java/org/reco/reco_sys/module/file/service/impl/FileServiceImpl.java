package org.reco.reco_sys.module.file.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.common.util.NetworkUtil;
import org.reco.reco_sys.config.AppProperties;
import org.reco.reco_sys.module.file.service.FileService;
import org.reco.reco_sys.module.websocket.entity.MobileUploadSession;
import org.reco.reco_sys.module.websocket.handler.MobileUploadWebSocketHandler;
import org.reco.reco_sys.module.websocket.repository.MobileUploadSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    private final AppProperties appProperties;
    private final MobileUploadSessionRepository sessionRepository;
    private final MobileUploadWebSocketHandler webSocketHandler;
    private final NetworkUtil networkUtil;

    @Override
    public String uploadImage(MultipartFile file) {
        validateImage(file);
        return saveFile(file);
    }

    @Override
    @Transactional
    public String createMobileUploadSession(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        MobileUploadSession session = new MobileUploadSession();
        session.setToken(token);
        session.setUserId(userId);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        sessionRepository.save(session);

        String lanIp = networkUtil.getLanIp();
        String baseUrl = appProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://" + lanIp + ":8888";
        }
        return baseUrl + "/mobile-upload/index.html?token=" + token;
    }

    @Override
    @Transactional
    public void handleMobileUpload(String token, MultipartFile file) {
        MobileUploadSession session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ResultCode.UPLOAD_SESSION_NOT_FOUND));
        if (session.getStatus() != MobileUploadSession.Status.PENDING) {
            throw new BusinessException(ResultCode.UPLOAD_SESSION_EXPIRED);
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(MobileUploadSession.Status.EXPIRED);
            sessionRepository.save(session);
            throw new BusinessException(ResultCode.UPLOAD_SESSION_EXPIRED);
        }
        validateImage(file);
        String url = saveFile(file);
        session.setFileUrl(url);
        session.setStatus(MobileUploadSession.Status.DONE);
        sessionRepository.save(session);
        webSocketHandler.notifyUploadDone(token, url);
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException(ResultCode.UPLOAD_FAILED, "文件为空");
        if (!ALLOWED_TYPES.contains(file.getContentType())) throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        if (file.getSize() > MAX_SIZE) throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
    }

    private String saveFile(MultipartFile file) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + ext;
            Path dir = Paths.get(appProperties.getUploadDir(), "images");
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename));
            return "/api/files/view/images/" + filename;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(ResultCode.UPLOAD_FAILED);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
