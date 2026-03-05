package org.reco.reco_sys.module.file.controller;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.result.Result;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.file.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final JwtUtil jwtUtil;

    @PostMapping("/api/files/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.uploadImage(file));
    }

    @GetMapping("/api/files/mobile-session")
    public Result<String> createMobileSession(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(extractToken(token));
        return Result.success(fileService.createMobileUploadSession(userId));
    }

    @PostMapping("/mobile-upload/upload")
    public Result<Void> mobileUpload(@RequestParam("token") String token,
                                      @RequestParam("file") MultipartFile file) {
        fileService.handleMobileUpload(token, file);
        return Result.success(null);
    }

    private String extractToken(String bearer) {
        return bearer != null && bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    }
}
