package org.reco.reco_sys.module.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.reco.reco_sys.common.exception.BusinessException;
import org.reco.reco_sys.common.result.ResultCode;
import org.reco.reco_sys.common.util.IpUtil;
import org.reco.reco_sys.common.util.JwtUtil;
import org.reco.reco_sys.module.auth.dto.*;
import org.reco.reco_sys.module.auth.service.AuthService;
import org.reco.reco_sys.module.notification.entity.Notification;
import org.reco.reco_sys.module.notification.service.NotificationService;
import org.reco.reco_sys.module.user.entity.EmailVerification;
import org.reco.reco_sys.module.user.entity.LoginRecord;
import org.reco.reco_sys.module.user.entity.SysUser;
import org.reco.reco_sys.module.user.repository.EmailVerificationRepository;
import org.reco.reco_sys.module.user.repository.LoginRecordRepository;
import org.reco.reco_sys.module.user.repository.SysUserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final LoginRecordRepository loginRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final NotificationService notificationService;
    private final IpUtil ipUtil;

    @Override
    @Transactional
    public void sendEmailCode(SendEmailRequest request) {
        EmailVerification.Type type = EmailVerification.Type.valueOf(request.getType());
        if (type == EmailVerification.Type.REGISTER && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被注册");
        }
        String code = generateCode();
        EmailVerification ev = new EmailVerification();
        ev.setEmail(request.getEmail());
        ev.setCode(code);
        ev.setType(type);
        ev.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        emailVerificationRepository.save(ev);
        sendMail(request.getEmail(), "验证码", "您的验证码是：" + code + "，5分钟内有效。");
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        verifyEmailCode(request.getEmail(), request.getEmailCode(), EmailVerification.Type.REGISTER);
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(SysUser.Role.STUDENT);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!user.getIsEnabled()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 记录登录
        LoginRecord record = new LoginRecord();
        record.setUserId(user.getId());
        record.setIpAddress(ip);
        record.setUserAgent(userAgent);
        String location = ipUtil.getLocation(ip);
        record.setLocation(location);

        // 检查异常IP（简单策略：与上次登录地点不同且间隔小于1小时）
        List<LoginRecord> recent = loginRecordRepository
                .findByUserIdOrderByLoginAtDesc(user.getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent();
        if (!recent.isEmpty()) {
            LoginRecord last = recent.get(0);
            boolean locationChanged = last.getLocation() != null && !last.getLocation().equals(location);
            boolean quickSwitch = last.getLoginAt().isAfter(LocalDateTime.now().minusHours(1));
            if (locationChanged && quickSwitch) {
                record.setIsAnomaly(true);
                notificationService.send(user.getId(), Notification.Type.SECURITY_ALERT,
                        "异常登录提醒", "检测到异地登录，IP: " + ip + "，地点: " + location, null);
            }
        }
        loginRecordRepository.save(record);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getUsername(),
                user.getRole().name(), user.getNickname());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        verifyEmailCode(request.getEmail(), request.getEmailCode(), EmailVerification.Type.RESET_PASSWORD);
        SysUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredEmailCodes() {
        emailVerificationRepository.deleteExpired(LocalDateTime.now());
    }

    private void verifyEmailCode(String email, String code, EmailVerification.Type type) {
        EmailVerification ev = emailVerificationRepository
                .findTopByEmailAndTypeAndIsUsedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "验证码不存在"));
        if (ev.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码已过期");
        }
        if (!ev.getCode().equals(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误");
        }
        ev.setIsUsed(true);
        emailVerificationRepository.save(ev);
    }

    private void sendMail(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1000000));
    }
}
