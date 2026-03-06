-- reco_sys MySQL init
-- 密码统一为 Abc@1234 (BCrypt)
-- 运行前请确认已创建数据库: CREATE DATABASE reco_sys CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Spring Boot 启动后 Hibernate 会自动建表，此脚本只插入初始用户数据

INSERT IGNORE INTO sys_user (username, password, email, nickname, role, is_enabled)
VALUES
  ('admin',   '$2b$10$honbehyGuK6Liq8LQr3gtuUrTSZtiiYb303AMXbRfeeP1/.NCFj5u',   'admin@reco.dev',   '管理员',   'ADMIN',   1),
  ('teacher1', '$2b$10$VwngEWBuUZupj3I41NIDOOaPw7SUnzS3xIAXglN9RrIDMrDmQKGbO', 'teacher@reco.dev', '赵老师',   'TEACHER', 1),
  ('student1', '$2b$10$fYo.GSfAxS.ujdO4Yqgl6etfe.AFLYt6cipB28HF5j7JS/v7FgyC6', 'student@reco.dev', '铭同学',   'STUDENT', 1);
