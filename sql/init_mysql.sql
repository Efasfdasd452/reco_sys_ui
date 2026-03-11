-- reco_sys MySQL 初始数据
-- 密码统一为 Abc@1234 (BCrypt)
--
-- 使用前请确认：
--   1. 已创建数据库: CREATE DATABASE reco_sys CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   2. 表结构由 Spring Boot (Hibernate) 根据 JPA 实体自动建表，请先至少启动一次后端再执行本脚本；
--      或先执行应用，再在空库上运行本 INSERT 插入初始用户。
--
-- 本脚本仅插入 sys_user 初始账号，列与实体 org.reco.reco_sys.module.user.entity.SysUser 一致：
--   username, password, email, nickname, role, is_enabled (created_at/updated_at 由 Hibernate 或 DB 默认值处理)

INSERT IGNORE INTO sys_user (username, password, email, nickname, role, is_enabled)
VALUES
  ('admin',   '$2b$10$honbehyGuK6Liq8LQr3gtuUrTSZtiiYb303AMXbRfeeP1/.NCFj5u',   'admin@reco.dev',   '管理员',   'ADMIN',   1),
  ('teacher1', '$2b$10$VwngEWBuUZupj3I41NIDOOaPw7SUnzS3xIAXglN9RrIDMrDmQKGbO', 'teacher@reco.dev', '赵老师',   'TEACHER', 1),
  ('student1', '$2b$10$fYo.GSfAxS.ujdO4Yqgl6etfe.AFLYt6cipB28HF5j7JS/v7FgyC6', 'student@reco.dev', '铭同学',   'STUDENT', 1);
