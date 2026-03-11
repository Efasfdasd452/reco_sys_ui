# 数据库表设计文档

> 数据库：MySQL 8.0+，字符集：utf8mb4，排序规则：utf8mb4_unicode_ci  
> **本文档与项目 JPA 实体一致**，表结构由 Hibernate（`ddl-auto=update`）根据实体自动生成，以下 SQL 仅作参考。

---

## 总览

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表 |
| `course` | 课程表 |
| `classroom` | 班级表 |
| `classroom_course` | 班级-课程关联表 |
| `classroom_student` | 班级-学生关联表 |
| `knowledge_point` | 知识点表 |
| `exercise` | 习题表 |
| `exercise_kp_rel` | 习题-知识点关联表 |
| `answer_record` | 答题记录表 |
| `recommendation_record` | 推荐记录表 |
| `rec_exercise_item` | 推荐习题明细表 |
| `user_kc_state` | 用户知识点掌握状态缓存表 |
| `user_exercise_forget` | 用户习题遗忘率缓存表 |
| `user_course` | 学生选课关联表 |
| `email_verification` | 邮箱验证码表 |
| `notification` | 站内通知表 |
| `login_record` | 登录记录表 |
| `mobile_upload_session` | 跨设备拍照上传会话表 |

---

## 1. sys_user — 用户表

对应实体：`org.reco.reco_sys.module.user.entity.SysUser`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 登录用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt 加密） |
| email | VARCHAR(100) | UNIQUE | 邮箱 |
| nickname | VARCHAR(50) | | 昵称/显示名 |
| role | VARCHAR(50) | NOT NULL, DEFAULT 'STUDENT' | 角色：STUDENT / TEACHER / ADMIN |
| is_enabled | BIT/TINYINT | | 是否启用（默认 1） |
| totp_secret | VARCHAR(128) | | TOTP 密钥（AES-256-GCM 加密后的 Base64 密文），注册时自动生成 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

> **安全说明**：`totp_secret` 字段存储的是加密密文（非明文），通过 `app.totp.encryption-key`（AES-256-GCM）加密。即使数据库泄露，攻击者也无法直接使用该字段生成 TOTP 验证码。

```sql
CREATE TABLE sys_user (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    email        VARCHAR(100) UNIQUE,
    nickname     VARCHAR(50),
    role         VARCHAR(50)  NOT NULL DEFAULT 'STUDENT',
    is_enabled   TINYINT(1)   DEFAULT 1,
    totp_secret  VARCHAR(128),
    created_at   DATETIME(6),
    updated_at   DATETIME(6)
);
```

---

## 2. course — 课程表

对应实体：`org.reco.reco_sys.module.course.entity.Course`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 课程名称 |
| description | TEXT | | 课程简介 |
| teacher_id | BIGINT | | 创建教师 ID |
| invite_code | VARCHAR(10) | UNIQUE | 课程邀请码 |
| is_active | BIT/TINYINT | | 是否启用（默认 1） |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

```sql
CREATE TABLE course (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(100)  NOT NULL,
    description  TEXT,
    teacher_id   BIGINT,
    invite_code  VARCHAR(10)   UNIQUE,
    is_active    TINYINT(1)    DEFAULT 1,
    created_at   DATETIME(6),
    updated_at   DATETIME(6)
);
```

---

## 3. classroom — 班级表

对应实体：`org.reco.reco_sys.module.classroom.entity.Classroom`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 班级名称 |
| description | TEXT | | 班级描述 |
| teacher_id | BIGINT | NOT NULL | 负责教师 |
| invite_code | VARCHAR(20) | UNIQUE | 加入班级邀请码 |
| is_active | BIT/TINYINT | | 是否启用 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

---

## 4. classroom_course — 班级-课程关联表

对应实体：`org.reco.reco_sys.module.classroom.entity.ClassroomCourse`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| classroom_id | BIGINT | NOT NULL | 班级 ID |
| course_id | BIGINT | NOT NULL | 课程 ID |
| added_at | DATETIME | | 添加时间 |
| UNIQUE(classroom_id, course_id) | | | 班级-课程唯一 |

---

## 5. classroom_student — 班级-学生关联表

对应实体：`org.reco.reco_sys.module.classroom.entity.ClassroomStudent`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| classroom_id | BIGINT | NOT NULL | 班级 ID |
| user_id | BIGINT | NOT NULL | 学生 ID |
| joined_at | DATETIME | | 加入时间 |
| UNIQUE(classroom_id, user_id) | | | 班级-学生唯一 |

---

## 6. knowledge_point — 知识点表

对应实体：`org.reco.reco_sys.module.knowledge.entity.KnowledgePoint`  
MySQL 存知识点基础信息，Neo4j 存知识点图关系。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| course_id | BIGINT | NOT NULL | 所属课程 |
| name | VARCHAR(100) | NOT NULL | 显示名称（如 kc0、一元一次方程） |
| description | TEXT | | 知识点描述 |
| parent_id | BIGINT | | 父知识点 ID |
| py_kc_index | INT | | Python 模型索引（kc0~kc111），null 表示非导入 |
| created_at | DATETIME | | 创建时间 |

```sql
CREATE TABLE knowledge_point (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    course_id     BIGINT        NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    description   TEXT,
    parent_id     BIGINT,
    py_kc_index   INT,
    created_at    DATETIME(6)
);
```

---

## 7. exercise — 习题表

对应实体：`org.reco.reco_sys.module.exercise.entity.Exercise`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| course_id | BIGINT | NOT NULL | 所属课程 |
| type | VARCHAR(50) | NOT NULL | 题型：SINGLE_CHOICE / MULTIPLE_CHOICE / TRUE_FALSE / FILL_BLANK / SHORT_ANSWER |
| content | LONGTEXT | NOT NULL | 题目正文（Markdown） |
| answer_key | LONGTEXT | | 标准答案/答案键 |
| difficulty | VARCHAR(50) | NOT NULL | EASY / MEDIUM / HARD |
| creator_id | BIGINT | | 创建教师 ID |
| py_ex_index | INT | | Python 模型索引（ex0~），null 表示教师自建 |
| created_at | DATETIME | | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

```sql
CREATE TABLE exercise (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    course_id     BIGINT        NOT NULL,
    type          VARCHAR(50)   NOT NULL,
    content       LONGTEXT      NOT NULL,
    answer_key    LONGTEXT,
    difficulty    VARCHAR(50)   NOT NULL DEFAULT 'MEDIUM',
    creator_id    BIGINT,
    py_ex_index   INT,
    created_at    DATETIME(6),
    updated_at    DATETIME(6)
);
```

---

## 8. exercise_kp_rel — 习题-知识点关联表

对应实体：`org.reco.reco_sys.module.exercise.entity.ExerciseKpRel`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| exercise_id | BIGINT | NOT NULL | 习题 ID |
| kp_id | BIGINT | NOT NULL | 知识点 ID（knowledge_point.id） |
| weight | DOUBLE | | 权重（默认 1.0） |
| UNIQUE(exercise_id, kp_id) | | | 习题-知识点唯一 |

---

## 9. answer_record — 答题记录表

对应实体：`org.reco.reco_sys.module.learning.entity.AnswerRecord`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 答题学生 |
| exercise_id | BIGINT | NOT NULL | 习题 ID |
| answer | LONGTEXT | | 学生作答内容 |
| status | VARCHAR(50) | NOT NULL | SUBMITTED / GRADING / GRADED / AUTO_GRADED |
| score | INT | | 得分 |
| teacher_comment | TEXT | | 教师评语 |
| graded_by | BIGINT | | 批改教师 ID |
| graded_at | DATETIME | | 批改时间 |
| time_spent | INT | | 答题用时（秒） |
| submitted_at | DATETIME | | 提交时间 |
| updated_at | DATETIME | | 更新时间 |

```sql
CREATE TABLE answer_record (
    id               BIGINT         PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT         NOT NULL,
    exercise_id      BIGINT         NOT NULL,
    answer            LONGTEXT,
    status           VARCHAR(50)    NOT NULL DEFAULT 'SUBMITTED',
    score            INT,
    teacher_comment  TEXT,
    graded_by        BIGINT,
    graded_at        DATETIME(6),
    time_spent       INT,
    submitted_at     DATETIME(6),
    updated_at       DATETIME(6)
);
```

---

## 10. recommendation_record — 推荐记录表

对应实体：`org.reco.reco_sys.module.recommendation.entity.RecommendationRecord`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 被推荐学生 |
| course_id | BIGINT | NOT NULL | 课程 ID |
| triggered_by | VARCHAR(50) | | 触发方式描述 |
| reason | TEXT | | 推荐原因说明 |
| created_at | DATETIME | | 推荐时间 |

---

## 11. rec_exercise_item — 推荐习题明细表

对应实体：`org.reco.reco_sys.module.recommendation.entity.RecExerciseItem`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| rec_id | BIGINT | NOT NULL | 所属推荐记录 ID（recommendation_record.id） |
| exercise_id | BIGINT | NOT NULL | 推荐的习题 ID |
| rank_order | INT | NOT NULL | 排名序号 |
| score | DOUBLE | | 推荐评分 |
| reason | TEXT | | 推荐理由 |
| kc_indices_json | TEXT | | 关联知识点 py 索引 JSON（如 "[0,1,2]"） |

---

## 12. user_kc_state — 用户知识点掌握状态缓存表

对应实体：`org.reco.reco_sys.module.learning.entity.UserKcState`  
掌握度 mlkc = correct_count / total_count，pkc 在业务侧由 total_count/总答题数 等计算。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 学生 ID |
| kp_id | BIGINT | NOT NULL | 知识点 ID（knowledge_point.id） |
| mastery_level | DOUBLE | NOT NULL | 掌握度 mlkc，0~1 |
| correct_count | INT | NOT NULL | 涉及该 KC 的答对数 |
| total_count | INT | NOT NULL | 涉及该 KC 的总答题数 |
| updated_at | DATETIME | | 最后更新时间 |
| UNIQUE(user_id, kp_id) | | | 用户-知识点唯一 |

```sql
CREATE TABLE user_kc_state (
    id                  BIGINT         PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT         NOT NULL,
    kp_id               BIGINT         NOT NULL,
    mastery_level        DOUBLE         NOT NULL DEFAULT 0,
    correct_count       INT            NOT NULL DEFAULT 0,
    total_count         INT            NOT NULL DEFAULT 0,
    updated_at          DATETIME(6),
    UNIQUE KEY uk_user_kp (user_id, kp_id)
);
```

---

## 13. user_exercise_forget — 用户习题遗忘率缓存表

对应实体：`org.reco.reco_sys.module.learning.entity.UserExerciseForget`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 学生 ID |
| exercise_id | BIGINT | NOT NULL | 习题 ID |
| forget_prob | DOUBLE | NOT NULL | 遗忘概率 exfr，0~1 |
| last_reviewed_at | DATETIME | | 最后复习时间 |
| updated_at | DATETIME | | 最后更新时间 |
| UNIQUE(user_id, exercise_id) | | | 用户-习题唯一 |

---

## 14. user_course — 学生选课关联表

对应实体：`org.reco.reco_sys.module.course.entity.UserCourse`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 学生 ID |
| course_id | BIGINT | NOT NULL | 课程 ID |
| enrolled_at | DATETIME | | 加入时间 |
| UNIQUE(user_id, course_id) | | | 用户-课程唯一 |

---

## 15. email_verification — 邮箱验证码表

对应实体：`org.reco.reco_sys.module.user.entity.EmailVerification`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| email | VARCHAR(100) | NOT NULL | 目标邮箱 |
| code | VARCHAR(10) | NOT NULL | 验证码 |
| type | VARCHAR(50) | NOT NULL | REGISTER / RESET_PASSWORD |
| expires_at | DATETIME | NOT NULL | 过期时间 |
| is_used | BIT/TINYINT | | 是否已使用 |
| created_at | DATETIME | | 发送时间 |

---

## 16. notification — 站内通知表

对应实体：`org.reco.reco_sys.module.notification.entity.Notification`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 接收人 |
| type | VARCHAR(50) | NOT NULL | GRADE_DONE / SECURITY_ALERT / SYSTEM |
| title | VARCHAR(200) | NOT NULL | 通知标题 |
| content | TEXT | | 通知内容 |
| related_id | BIGINT | | 关联记录 ID（如 answer_record.id） |
| is_read | BIT/TINYINT | | 是否已读 |
| created_at | DATETIME | | 创建时间 |

---

## 17. login_record — 登录记录表

对应实体：`org.reco.reco_sys.module.user.entity.LoginRecord`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 用户 ID |
| ip_address | VARCHAR(45) | | 登录 IP |
| location | VARCHAR(100) | | IP 解析地理位置 |
| user_agent | VARCHAR(500) | | 浏览器/设备信息 |
| is_anomaly | BIT/TINYINT | | 是否异常登录 |
| login_at | DATETIME | | 登录时间 |

---

## 18. mobile_upload_session — 跨设备拍照上传会话表

对应实体：`org.reco.reco_sys.module.websocket.entity.MobileUploadSession`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| token | VARCHAR(64) | NOT NULL, UNIQUE | 一次性 Token |
| user_id | BIGINT | NOT NULL | 发起上传的用户 |
| file_url | VARCHAR(500) | | 上传完成后的文件访问地址 |
| status | VARCHAR(50) | NOT NULL | PENDING / DONE / EXPIRED |
| expires_at | DATETIME | NOT NULL | 过期时间 |
| created_at | DATETIME | | 创建时间 |

---

## 表关系总览

```
sys_user ──────────────────────────────────────────────────────────────┐
  │ (teacher_id)                                                       │
  ├──→ course ←──────────── user_course（学生选课，enrolled_at）       │
  │      │ (course_id)                                                 │
  │      ├──→ knowledge_point ←── exercise_kp_rel(kp_id) ──→ exercise   │
  │      │                                           │(creator_id)     │
  │      └──→ exercise ────────────────────────────←─┘
  │             │
  │             ↓
  ├──→ answer_record ←── (user_id / exercise_id / graded_by，status)
  │
  ├──→ recommendation_record(course_id, triggered_by, reason)
  │         └──→ rec_exercise_item(rec_id) ──→ exercise
  │
  ├──→ user_kc_state(kp_id) ──→ knowledge_point
  ├──→ user_exercise_forget ──→ exercise
  │
  ├──→ classroom(teacher_id) ── classroom_course / classroom_student
  ├──→ notification（批改完成 / 安全警告）
  ├──→ login_record（登录历史 + is_anomaly）
  └──→ mobile_upload_session（file_url，扫码跨设备上传）

email_verification（独立，与邮箱关联）
```

---

## Neo4j 节点与关系（知识图谱）

与 Java 中 `KnowledgePointNode`、图查询一致：

| 节点/关系 | 说明 |
|-----------|------|
| `(:KnowledgePoint {id, mysqlId, name, courseId})` | 知识点节点（name 如 kc0、kc1） |
| `-[:RELATED_TO {cooccurrence}]->` | 知识点共现关系（init_neo4j.cypher 创建） |

图数据由管理后台「导入推荐数据（KG4Ex）」写入 MySQL 并同步创建 Neo4j 节点；`sql/init_neo4j.cypher` 在此基础上创建 RELATED_TO 边。
