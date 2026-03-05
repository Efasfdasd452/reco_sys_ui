# 数据库表设计文档

> 数据库：MySQL 8.0+，字符集：utf8mb4，排序规则：utf8mb4_unicode_ci

---

## 总览

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表 |
| `course` | 课程表 |
| `knowledge_point` | 知识点表 |
| `exercise` | 习题表 |
| `exercise_kp_rel` | 习题-知识点关联表 |
| `answer_record` | 答题记录表 |
| `recommendation_record` | 推荐记录表 |
| `rec_exercise_item` | 推荐习题明细表 |
| `user_kc_state` | 用户知识点掌握状态缓存表 |
| `user_exercise_forget` | 用户习题遗忘率缓存表 |

---

## 1. sys_user — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 登录用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt 加密） |
| real_name | VARCHAR(50) | | 真实姓名 |
| email | VARCHAR(100) | UNIQUE | 邮箱 |
| phone | VARCHAR(20) | | 手机号 |
| role | ENUM | NOT NULL, DEFAULT 'STUDENT' | 角色：STUDENT / TEACHER / ADMIN |
| avatar_url | VARCHAR(500) | | 头像地址 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 正常 / 0 禁用 |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | NOT NULL, ON UPDATE | 更新时间 |

```sql
CREATE TABLE sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    real_name   VARCHAR(50),
    email       VARCHAR(100) UNIQUE,
    phone       VARCHAR(20),
    role        ENUM('STUDENT','TEACHER','ADMIN') NOT NULL DEFAULT 'STUDENT',
    avatar_url  VARCHAR(500),
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 2. course — 课程表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(100) | NOT NULL | 课程名称 |
| description | TEXT | | 课程简介 |
| cover_url | VARCHAR(500) | | 封面图 |
| teacher_id | BIGINT | FK → sys_user.id | 创建教师 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 上线 / 0 下线 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

```sql
CREATE TABLE course (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    description TEXT,
    cover_url   VARCHAR(500),
    teacher_id  BIGINT        NOT NULL,
    status      TINYINT       NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
);
```

---

## 3. knowledge_point — 知识点表

> MySQL 存知识点基础信息，Neo4j 存知识点之间的图结构关系。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| kc_name | VARCHAR(50) | NOT NULL, UNIQUE | Python 模型内部名称（如 kc0、kc1） |
| name | VARCHAR(100) | NOT NULL | 可读显示名称（如"一元一次方程"） |
| description | TEXT | | 知识点描述 |
| course_id | BIGINT | FK → course.id | 所属课程 |
| difficulty | TINYINT | DEFAULT 1 | 难度：1~5 |
| neo4j_node_id | VARCHAR(100) | | 对应 Neo4j 节点 ID |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

```sql
CREATE TABLE knowledge_point (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    kc_name       VARCHAR(50)   NOT NULL UNIQUE,
    name          VARCHAR(100)  NOT NULL,
    description   TEXT,
    course_id     BIGINT        NOT NULL,
    difficulty    TINYINT       DEFAULT 1,
    neo4j_node_id VARCHAR(100),
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES course(id)
);
```

---

## 4. exercise — 习题表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| ex_name | VARCHAR(50) | NOT NULL, UNIQUE | Python 模型内部名称（如 ex0、ex1） |
| title | VARCHAR(255) | | 题目标题（可选） |
| content | TEXT | NOT NULL | 题目正文（Markdown 格式，支持图文混排） |
| type | ENUM | NOT NULL | 题型：SINGLE_CHOICE / MULTI_CHOICE / TRUE_FALSE / FILL_BLANK / ESSAY |
| options | JSON | | 选择题选项，格式：`[{"key":"A","value":"..."}]` |
| answer | VARCHAR(500) | | 标准答案（问答题为空，由教师批改） |
| analysis | TEXT | | 解题分析 |
| difficulty | TINYINT | NOT NULL, DEFAULT 1 | 难度：1~5 |
| course_id | BIGINT | FK → course.id | 所属课程 |
| created_by | BIGINT | FK → sys_user.id | 创建教师 |
| neo4j_node_id | VARCHAR(100) | | 对应 Neo4j 节点 ID |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：1 启用 / 0 禁用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

```sql
CREATE TABLE exercise (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    ex_name       VARCHAR(50)   NOT NULL UNIQUE,
    title         VARCHAR(255),
    content       TEXT          NOT NULL,
    type          ENUM('SINGLE_CHOICE','MULTI_CHOICE','TRUE_FALSE','FILL_BLANK','ESSAY') NOT NULL,
    options       JSON,
    answer        VARCHAR(500),
    analysis      TEXT,
    difficulty    TINYINT       NOT NULL DEFAULT 1,
    course_id     BIGINT        NOT NULL,
    created_by    BIGINT        NOT NULL,
    neo4j_node_id VARCHAR(100),
    status        TINYINT       NOT NULL DEFAULT 1,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id)   REFERENCES course(id),
    FOREIGN KEY (created_by)  REFERENCES sys_user(id)
);
```

---

## 5. exercise_kp_rel — 习题-知识点关联表

> MySQL 冗余存储一份，便于快速查询；Neo4j 中同样维护此关系用于图计算。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| exercise_id | BIGINT | FK → exercise.id | 习题 ID |
| knowledge_point_id | BIGINT | FK → knowledge_point.id | 知识点 ID |

```sql
CREATE TABLE exercise_kp_rel (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    exercise_id         BIGINT NOT NULL,
    knowledge_point_id  BIGINT NOT NULL,
    UNIQUE KEY uk_ex_kp (exercise_id, knowledge_point_id),
    FOREIGN KEY (exercise_id)        REFERENCES exercise(id),
    FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
);
```

---

## 6. answer_record — 答题记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | FK → sys_user.id | 答题学生 |
| exercise_id | BIGINT | FK → exercise.id | 习题 ID |
| user_answer | TEXT | | 学生作答内容 |
| is_correct | TINYINT | | 1 正确 / 0 错误 / NULL 待批改 |
| score | DECIMAL(5,2) | | 得分（问答题由教师填写） |
| time_spent | INT | | 答题用时（秒） |
| grade_type | ENUM | NOT NULL, DEFAULT 'AUTO' | 批改方式：AUTO / MANUAL |
| grade_status | ENUM | NOT NULL, DEFAULT 'GRADED' | 批改状态：GRADED / PENDING |
| teacher_comment | TEXT | | 教师评语（问答题） |
| graded_by | BIGINT | FK → sys_user.id | 批改教师 |
| graded_at | DATETIME | | 批改时间 |
| submitted_at | DATETIME | NOT NULL | 提交时间 |

```sql
CREATE TABLE answer_record (
    id               BIGINT         PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT         NOT NULL,
    exercise_id      BIGINT         NOT NULL,
    user_answer      TEXT,
    is_correct       TINYINT,
    score            DECIMAL(5,2),
    time_spent       INT,
    grade_type       ENUM('AUTO','MANUAL')    NOT NULL DEFAULT 'AUTO',
    grade_status     ENUM('GRADED','PENDING') NOT NULL DEFAULT 'GRADED',
    teacher_comment  TEXT,
    graded_by        BIGINT,
    graded_at        DATETIME,
    submitted_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)     REFERENCES sys_user(id),
    FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    FOREIGN KEY (graded_by)   REFERENCES sys_user(id)
);
```

---

## 7. recommendation_record — 推荐记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | FK → sys_user.id | 被推荐学生 |
| trigger_type | ENUM | NOT NULL | 触发方式：AUTO（自动）/ MANUAL（手动刷新） |
| top_n | INT | NOT NULL, DEFAULT 10 | 推荐数量 |
| created_at | DATETIME | NOT NULL | 推荐时间 |

```sql
CREATE TABLE recommendation_record (
    id           BIGINT   PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL,
    trigger_type ENUM('AUTO','MANUAL') NOT NULL,
    top_n        INT      NOT NULL DEFAULT 10,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
```

---

## 8. rec_exercise_item — 推荐习题明细表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| record_id | BIGINT | FK → recommendation_record.id | 所属推荐记录 |
| exercise_id | BIGINT | FK → exercise.id | 推荐的习题 |
| score | DECIMAL(10,4) | | Python 模型推荐评分 |
| rank_order | INT | NOT NULL | 排名序号 |

```sql
CREATE TABLE rec_exercise_item (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT,
    record_id   BIGINT          NOT NULL,
    exercise_id BIGINT          NOT NULL,
    score       DECIMAL(10,4),
    rank_order  INT             NOT NULL,
    FOREIGN KEY (record_id)   REFERENCES recommendation_record(id),
    FOREIGN KEY (exercise_id) REFERENCES exercise(id)
);
```

---

## 9. user_kc_state — 用户知识点掌握状态缓存表

> 缓存 Python 模型所需的 `mlkc`（掌握度）与 `pkc`（出现概率），每次答题后异步更新，避免每次推荐时重新计算。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | FK → sys_user.id | 学生 ID |
| knowledge_point_id | BIGINT | FK → knowledge_point.id | 知识点 ID |
| mastery_level | DECIMAL(5,4) | NOT NULL, DEFAULT 0 | 掌握度 mlkc，范围 0~1 |
| appear_prob | DECIMAL(5,4) | NOT NULL, DEFAULT 0 | 出现概率 pkc，范围 0~1 |
| updated_at | DATETIME | NOT NULL | 最后更新时间 |

```sql
CREATE TABLE user_kc_state (
    id                  BIGINT         PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT         NOT NULL,
    knowledge_point_id  BIGINT         NOT NULL,
    mastery_level       DECIMAL(5,4)   NOT NULL DEFAULT 0.0000,
    appear_prob         DECIMAL(5,4)   NOT NULL DEFAULT 0.0000,
    updated_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_kp (user_id, knowledge_point_id),
    FOREIGN KEY (user_id)            REFERENCES sys_user(id),
    FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
);
```

---

## 10. user_exercise_forget — 用户习题遗忘率缓存表

> 缓存 Python 模型所需的 `exfr`（习题遗忘率），根据学生答题时间间隔计算并缓存。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | FK → sys_user.id | 学生 ID |
| exercise_id | BIGINT | FK → exercise.id | 习题 ID |
| forget_rate | DECIMAL(5,4) | NOT NULL, DEFAULT 0 | 遗忘率 exfr，范围 0~1 |
| updated_at | DATETIME | NOT NULL | 最后更新时间 |

```sql
CREATE TABLE user_exercise_forget (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    exercise_id BIGINT       NOT NULL,
    forget_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_ex (user_id, exercise_id),
    FOREIGN KEY (user_id)     REFERENCES sys_user(id),
    FOREIGN KEY (exercise_id) REFERENCES exercise(id)
);
```

---

## 表关系总览

```
sys_user ──────────────────────────────────────────────────────┐
  │ (teacher_id)                                               │
  ├──→ course                                                  │
  │      │ (course_id)                                         │
  │      ├──→ knowledge_point ←── exercise_kp_rel ──→ exercise │
  │      │                                           │(created_by)
  │      └──→ exercise ────────────────────────────←─┘
  │             │
  │             ↓
  ├──→ answer_record ←── (user_id / exercise_id / graded_by)
  │
  ├──→ recommendation_record
  │         │
  │         └──→ rec_exercise_item ──→ exercise
  │
  ├──→ user_kc_state ──→ knowledge_point
  │
  └──→ user_exercise_forget ──→ exercise
```

---

## Neo4j 节点与关系（知识图谱部分）

| 节点/关系 | 说明 |
|-----------|------|
| `(:KnowledgePoint {id, kcName, name})` | 知识点节点 |
| `(:Exercise {id, exName})` | 习题节点 |
| `(:Course {id, name})` | 课程节点 |
| `-[:PREREQUISITE]->` | 知识点前置关系 |
| `-[:RELATED_TO]->` | 知识点相关关系 |
| `-[:BELONGS_TO]->` | 习题属于某知识点 |
| `-[:CONTAINS]->` | 课程包含某知识点 |
