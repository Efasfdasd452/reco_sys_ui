package org.reco.reco_sys.module.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String uploadImage(MultipartFile file);
    String createMobileUploadSession(Long userId);
    void handleMobileUpload(String token, MultipartFile file);
}
