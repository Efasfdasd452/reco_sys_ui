package org.reco.reco_sys.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;
}
