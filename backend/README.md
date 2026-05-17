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
│   └── UserAccount.java                # 积分账户表
├── dto/
│   ├── RegisterRequest.java            # 注册请求体
│   ├── LoginRequest.java               # 登录请求体
│   ├── LoginResponse.java              # 登录响应体
│   ├── UpdateProfileRequest.java       # 更新资料请求体
│   └── UserInfoResponse.java           # 用户信息响应体
├── mapper/
│   ├── UserMapper.java
│   ├── PrivacyProfileMapper.java
│   └── UserAccountMapper.java
├── service/
│   ├── UserService.java                # 接口
│   └── impl/UserServiceImpl.java       # 实现（注册/登录/资料管理）
└── controller/
    ├── UserController.java             # /api/v1/user/*
    └── AdminController.java            # /api/v1/admin/*
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

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/user/register` | 无 | 注册 |
| POST | `/user/login` | 无 | 登录，返回 JWT |
| GET | `/user/profile` | JWT | 查看个人资料 |
| PUT | `/user/profile` | JWT | 修改资料（含隐私设置） |
| GET | `/admin/users` | ADMIN | 分页用户列表 |
| PUT | `/admin/users/{id}/status` | ADMIN | 封禁/解封用户 |

### 统一响应格式

```json
{"code": 200, "msg": "操作成功", "data": {}}
```

---

## 数据库迁移（Flyway）

迁移文件位于 `src/main/resources/db/migration/`：

```
V1__init_user_tables.sql   # 当前唯一迁移：user + privacy_profile + user_account
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
