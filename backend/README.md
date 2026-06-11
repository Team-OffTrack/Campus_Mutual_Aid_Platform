# 校园互助后端 (Campus Help Backend)

Spring Boot 3 项目，提供 RESTful API，使用 JWT 无状态认证。

---

## 技术栈

| 组件 | 选型 |
|------|------|
| 框架 | Spring Boot 3.2.5 |
| 语言 | Java 17 |
| 安全 | Spring Security + JWT (jjwt) |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL / MariaDB |
| 迁移 | Flyway Community |
| 缓存 | Redis（预留） |
| 构建 | Maven |
| 测试 | JUnit 5 + H2 内存库 |

---

## 项目结构

```
src/main/java/cn/seecoder/campushelp/
├── CampusHelpApplication.java          # 入口
├── common/
│   ├── ApiResult.java                  # 统一响应格式 {code, msg, data}
│   ├── ResultCode.java                 # HTTP 状态码常量
│   ├── BusinessException.java          # 业务异常
│   └── GlobalExceptionHandler.java     # @RestControllerAdvice
├── config/
│   ├── SecurityConfig.java             # Spring Security 配置（JWT 过滤器链）
│   ├── CorsConfig.java                 # 跨域配置
│   ├── MyBatisPlusConfig.java          # 分页插件
│   └── DataInitializer.java            # 首次启动创建默认管理员
├── security/
│   ├── JwtTokenProvider.java           # JWT 生成与校验
│   └── JwtAuthenticationFilter.java    # 从请求头提取 JWT 注入 SecurityContext
├── entity/
│   ├── User.java                       # 用户表
│   ├── PrivacyProfile.java             # 隐私配置表
│   ├── UserAccount.java                # 积分账户表
│   ├── Demand.java                     # 需求表
│   ├── PointsTransaction.java          # 积分流水表（V11）
│   ├── DailyCheckin.java               # 每日签到表（V11）
│   ├── TeamMember.java                 # 队伍成员表（V10）
│   ├── Notification.java               # 通知表
│   ├── Evaluation.java                 # 评价表
│   ├── Conversation.java               # 会话表
│   ├── Message.java                    # 消息表
│   └── enums/
│       ├── DemandStatus.java           # OPEN / IN_PROGRESS / COMPLETED / CANCELLED
│       ├── NotificationType.java       # ACCEPT / COMPLETE / CANCEL / EVALUATION / JOIN_REQUEST / …
│       ├── PointsTransactionType.java  # SIGNUP_BONUS / DAILY_CHECKIN / PUBLISH / CANCEL_REFUND / …
│       ├── RewardType.java             # POINT / DONATION / @Deprecated CASH
│       ├── TeamMemberRole.java         # LEADER / MEMBER
│       └── TeamMemberStatus.java       # PENDING / JOINED / REJECTED
├── dto/
│   ├── RegisterRequest.java            # 注册请求体
│   ├── LoginRequest.java               # 登录请求体
│   ├── LoginResponse.java              # 登录响应体
│   ├── UpdateProfileRequest.java       # 更新资料请求体
│   ├── UserInfoResponse.java           # 用户信息响应体（含积分/信誉）
│   ├── CreateDemandRequest.java        # 发布需求请求体
│   ├── DemandResponse.java             # 需求响应体（含队员/人数）
│   ├── TeamMemberResponse.java         # 队员信息响应体
│   ├── DailyCheckinResponse.java       # 签到响应体（积分/连续天数）
│   ├── DailyCheckinStatus.java         # 签到状态（今日是否已签）
│   └── …（评价/聊天相关 DTO）
├── mapper/
│   ├── UserMapper.java
│   ├── UserAccountMapper.java
│   ├── DemandMapper.java
│   ├── PointsTransactionMapper.java    # 积分流水（V11）
│   ├── DailyCheckinMapper.java         # 签到记录（V11）
│   └── …（其他 6 个 mapper）
├── service/
│   ├── UserService.java                # 接口
│   ├── DemandService.java
│   ├── PointsService.java              # 积分服务接口（签到/冻结/解冻/转账/注册奖金）
│   └── impl/
│       ├── UserServiceImpl.java        # 注册/登录/资料管理（注册送 100 积分）
│       ├── DemandServiceImpl.java      # 需求生命周期（发布冻结/取消解冻/完成转账）
│       ├── PointsServiceImpl.java      # 积分核心实现（签到 streak / 悲观锁 / 记账）
│       └── …（其他 service 实现）
└── controller/
    ├── UserController.java             # /api/v1/user/*
    ├── DemandController.java           # /api/v1/demands/*
    ├── PointsController.java           # /api/v1/points/checkin, /checkin/status, /transactions
    ├── TeamMemberController.java       # /api/v1/demands/{id}/team/*
    └── …（Admin / Chat / Evaluation / Notification 控制器）
```

---

## 快速启动

### 1. 数据库准备

```bash
sudo systemctl start mariadb redis
sudo mariadb -u root -e "CREATE DATABASE IF NOT EXISTS campus_help DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

> **Debian / Ubuntu**：MariaDB 默认 unix_socket 认证，需先改密码认证：
> ```bash
> sudo mariadb -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root'; FLUSH PRIVILEGES;"
> ```

### 2. 生成 SSL 证书（仅首次）

```bash
cd src/main/resources
keytool -genkeypair -alias campus-help -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit -keypass changeit -dname "CN=campus-help" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"
```

### 3. 启动

```bash
cd backend
mvn spring-boot:run          # 开发模式（https://localhost:8080）
# 或
mvn package -DskipTests && java -jar target/campus-help-0.1.0.jar
```

应用启动时 Flyway 自动执行 `src/main/resources/db/migration/` 下的迁移脚本，同时创建默认管理员。

### 3. 运行测试

```bash
mvn test
```

测试使用 `application-test.yml` 配置，以 H2 内存数据库运行，**无需 MySQL**。测试失败不会影响开发库数据。

---

## 配置说明

### `application.yml`（全局）

```yaml
server:
  port: 8080                    # 服务端口
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: campus-help      # HTTPS 自签名证书

jwt:
  secret: <Base64 encoded>      # JWT 签名密钥
  expiration: 86400000          # Token 有效期（毫秒），默认 24h
```

### `application-dev.yml`（开发环境）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_help?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root              # 改为你的数据库用户名
    password: root              # 改为你的数据库密码
  flyway:
    enabled: true
    baseline-on-migrate: true   # 空库或非空库都能自动对齐
```

> **注意**：JDBC URL 中 `characterEncoding=UTF-8` 必须用 Java 标准字符集名，不能用 `utf8mb4`。

---

## API 概览

所有接口前缀：`/api/v1`

### 用户

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/user/register` | 无 | 注册（新用户赠送 100 积分） |
| POST | `/user/login` | 无 | 登录，返回 JWT |
| GET | `/user/profile` | JWT | 查看个人资料（含积分/信誉） |
| PUT | `/user/profile` | JWT | 修改资料（含隐私设置） |
| PUT | `/user/password` | JWT | 修改密码 |
| POST | `/user/avatar` | JWT | 上传头像 |
| GET | `/admin/users` | ADMIN | 分页用户列表 |
| PUT | `/admin/users/{id}/status` | ADMIN | 封禁/解封用户 |

### 需求（Demand）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/demands` | JWT | 发布需求（point 类型自动冻结积分） |
| GET | `/demands` | JWT | 分页列表（筛选/搜索/排序） |
| GET | `/demands/{id}` | JWT | 需求详情（team 含队员信息） |
| PUT | `/demands/{id}/accept` | JWT | 接取需求 |
| PUT | `/demands/{id}/complete` | JWT | 确认完成（自动转账积分） |
| PUT | `/demands/{id}/cancel` | JWT | 取消需求（自动解冻积分） |
| GET | `/demands/my/orders` | JWT | 我的订单（publisher/acceptor） |
| GET | `/demands/my/team` | JWT | 我的队伍 |
| POST | `/demands/image` | JWT | 上传需求图片 |

### 积分（V11 新增）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/points/checkin` | JWT | 每日签到（5-15 积分，连续天数加成） |
| GET | `/points/checkin/status` | JWT | 签到状态（今日是否已签、连续天数） |
| GET | `/points/transactions` | JWT | 积分流水（分页，按类型筛选） |

### 组队（V10 新增）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/demands/{id}/team/apply` | JWT | 申请加入队伍 |
| PUT | `/demands/{id}/team/applicants/{uid}/approve` | JWT | 队长审批通过 |
| PUT | `/demands/{id}/team/applicants/{uid}/reject` | JWT | 队长拒绝 |
| POST | `/demands/{id}/team/leave` | JWT | 退出队伍 |
| DELETE | `/demands/{id}/team/members/{mid}` | JWT | 队长踢人 |
| GET | `/demands/{id}/team/members` | JWT | 已加入成员列表 |
| GET | `/demands/{id}/team/applicants` | JWT | 待审核申请列表 |
| GET | `/demands/{id}/team/my-membership` | JWT | 我的成员状态 |

### 评价 / 聊天 / 通知

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/evaluations` | JWT | 创建评价（team 类型禁止） |
| GET | `/evaluations/user/{uid}` | JWT | 查看用户评价 |
| POST | `/chat/conversations` | JWT | 创建/获取会话 |
| GET | `/chat/conversations` | JWT | 会话列表 |
| POST | `/chat/messages` | JWT | 发送消息 |
| GET | `/chat/{id}/messages` | JWT | 消息列表 |
| GET | `/notifications` | JWT | 通知列表 |

### 统一响应格式

```json
{"code": 200, "msg": "操作成功", "data": {}}
```

---

## 数据库迁移（Flyway）

迁移文件位于 `src/main/resources/db/migration/`：

```
V1__init_user_tables.sql            # user + privacy_profile + user_account
V2__create_demand_table.sql         # demand 表（reward_type / reward_amount）
V3__add_acceptor_to_demand.sql      # 接单人字段
V4__create_notification_table.sql   # 通知表
V5__create_evaluation_table.sql     # 评价表
V6__create_chat_tables.sql          # 会话 + 消息表
V7__add_images_to_demand.sql        # 需求图片列
V8__add_image_to_message.sql        # 消息图片列
V9__add_attributes_to_demand.sql    # 需求属性 JSON 列
V10__create_team_member_table.sql   # 组队多人模型
V11__create_points_tables.sql       # 积分流水 + 每日签到（当前最新）
```

### 新增迁移

1. 创建 `V2__xxx.sql`，写入 DDL 或 DML
2. 重启应用，Flyway 自动执行
3. **不要修改**已应用的历史迁移文件

### 重置数据库

```bash
sudo mariadb -u root -e "DROP DATABASE campus_help; CREATE DATABASE campus_help DEFAULT CHARACTER SET utf8mb4;"
# 重启应用即可
```

---

## 默认管理员

- 学号：`admin`
- 密码：`admin123`
- 角色：`ADMIN`

由 `DataInitializer` 在首次启动时自动创建（仅在数据库中无 ADMIN 用户时触发）。
