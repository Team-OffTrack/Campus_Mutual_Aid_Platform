# 校园互助服务平台 (Campus Mutual Help Platform)

面向在校大学生的互助服务系统，支持跑腿代取、二手交易、组队匹配、失物招领等功能。平台采用统一积分货币体系——取消现金交易，积分通过签到、注册赠送、完成互助任务获得，支持发布冻结、取消解冻、完成转账的完整流转，每笔积分变动可追溯。

---

## 项目结构

```
/
├── backend/                    # 后端 Spring Boot 项目（Java 17）
│   ├── src/main/java/cn/seecoder/campushelp/
│   │   ├── common/             # ApiResult、ResultCode、BusinessException、全局异常处理
│   │   ├── config/             # Security、CORS、MyBatis-Plus、数据初始化
│   │   ├── security/           # JWT 生成与认证过滤器
│   │   ├── entity/             # 数据实体（User、Demand、UserAccount、PointsTransaction 等）
│   │   │   └── enums/          # 常量类（DemandStatus、NotificationType、RewardType 等）
│   │   ├── dto/                # 请求/响应 DTO
│   │   ├── mapper/             # MyBatis-Plus Mapper 接口（12 个）
│   │   ├── service/            # 业务服务接口 + 实现
│   │   └── controller/         # REST 控制器（8 个）
│   └── src/main/resources/
│       ├── application.yml           # 全局配置
│       ├── application-dev.yml       # 开发环境配置
│       └── db/migration/             # Flyway 数据库迁移脚本（V1-V11）
├── frontend/                   # 前端 Vue 3 项目（Vite + Vant UI）
│   └── src/
│       ├── router/             # 路由配置（12 个页面 + 导航守卫）
│       ├── stores/             # Pinia 状态管理（auth store）
│       ├── api/                # Axios 封装 + 7 个 API 模块
│       ├── views/              # 页面组件（12 个）
│       │   └── admin/          # 管理后台页面（UserList）
│       ├── components/         # 可复用组件（NavActions、ImageViewer、EmojiPicker）
│       ├── constants/          # 常量（需求类型、rewardText 共享函数）
│       └── styles/             # 全局样式（M3 设计系统 200+ 行 token）
└── docs/                       # 阶段文档 + HCI 交互设计文档
```

## 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.2.5 | Java 17 |
| 安全认证 | Spring Security + JWT (jjwt 0.12.5) | 无状态 Token 认证 |
| 持久层 | MyBatis-Plus 3.5.6 | Lambda 查询封装、分页插件 |
| 数据库 | MySQL / MariaDB | 生产数据库 |
| 数据库迁移 | Flyway | 版本化 SQL 迁移 |
| 测试数据库 | H2 | 内存库，测试用 |
| 前端框架 | Vue 3 + Vite | Composition API |
| UI 组件库 | Vant 4 | 移动端优先 |
| 状态管理 | Pinia | 用户认证状态 |
| HTTP 客户端 | Axios | 拦截器自动注入 JWT |

## 信用分计算

用户信用分（`user_account.reputation_score`）用于衡量用户在平台上的可靠程度，取值范围约 [0.6, 5.0]，精确到小数点后一位。

### 计算公式

```
信用分 = 0.6 × 评价均分 + 0.4 × (完成率 × 5)
```

| 参数 | 说明 | 取值范围 | 默认值 |
|------|------|----------|--------|
| **评价均分** | 所有收到评价的 rating 算术平均 | [1, 5] | 5.0（无评价时） |
| **完成率** | 作为接单者：`COMPLETED / (COMPLETED + CANCELLED)` | [0, 1] | 1.0（无已完结需求时） |

完成率乘以 5 将其归一化到与评价均分相同的 [0, 5] 区间，使得两个维度可加权求和。

### 设计意图

- **w₁ = 0.6（评价）**：他人对你的直接评价是最重要的信用信号
- **w₂ = 0.4（完成率）**：即使评分高，频繁被取消接单也会拉低信用分
- **新用户冷启动**：无评价且无接单记录时默认满分 5.0，随交易积累逐渐收敛到真实水平

### 示例

| 评价均分 | 完成率 | 计算过程 | 信用分 |
|----------|--------|----------|--------|
| 5.0 | 100% (10/10) | 0.6×5.0 + 0.4×5.0 | **5.0** |
| 4.0 | 80% (8/10) | 0.6×4.0 + 0.4×4.0 | **4.0** |
| 5.0 | 50% (5/10) | 0.6×5.0 + 0.4×2.5 | **4.0** |
| 3.0 | 100% (1/1) | 0.6×3.0 + 0.4×5.0 | **3.8** |
| 1.0 | 100% (1/1) | 0.6×1.0 + 0.4×5.0 | **2.6** |

## 积分系统

平台使用积分作为统一的流通货币，取消现金交易。积分贯穿需求全生命周期。

### 积分获取

| 途径 | 积分 | 说明 |
|------|------|------|
| 注册赠送 | 100 | 新用户注册即送 |
| 每日签到 | 5-15 | 基础 5 分，连续签到加成（3-4 天 +3、5-6 天 +5、7 天+ +10） |
| 完成任务 | 需求悬赏额 | 接单并完成后获得发布者冻结的积分 |

### 积分流转

```
发布需求（rewardType=point, amount=N）
  → 可用积分 -= N，冻结积分 += N
  → 记录 PUBLISH 流水

取消需求
  → 冻结积分 -= N，可用积分 += N
  → 记录 CANCEL_REFUND 流水

确认完成
  → 发布者冻结积分 -= N
  → 接单人可用积分 += N
  → 双方各记一笔 COMPLETE_EARN 流水
```

**team 类型**默认 `donation`（不涉及积分），**donation 类型**纯志愿互助。

### 签到规则

- 每日限签 1 次（数据库 UNIQUE 约束防重复）
- 连续天数按自然日计算，中断（昨天未签）则重置为 1
- 积分加成：1-2 天→5 分，3-4 天→8 分，5-6 天→10 分，7 天+→15 分

### 相关 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/points/checkin` | 每日签到 |
| GET | `/api/v1/points/checkin/status` | 签到状态（今日是否已签、连续天数） |
| GET | `/api/v1/points/transactions` | 积分流水（分页，按类型筛选） |

## 快速开始

### 环境要求

- **JDK 17+**
- **Maven 3.8+**
- **MariaDB**（Arch: `sudo pacman -S mariadb`；Debian: `sudo apt install mariadb-server`）
- **Node.js 18+**

### 1. 初始化数据库

```bash
# 启动服务
sudo systemctl start mariadb

# 创建数据库
sudo mariadb -u root -e "CREATE DATABASE IF NOT EXISTS campus_help DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

> **Debian / Ubuntu 注意**：MariaDB 默认使用 unix_socket 认证，不允许密码登录。需要先改为密码认证：
> ```bash
> sudo mariadb -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root'; FLUSH PRIVILEGES;"
> ```

### 2. 生成 SSL 证书（仅首次）

项目前后端均启用 HTTPS。每个开发机需要生成两份自签名证书：

```bash
# 后端 — PKCS12 证书（Java keystore）
cd backend/src/main/resources
keytool -genkeypair -alias campus-help -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit -keypass changeit -dname "CN=campus-help" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"

# 前端 — PEM 证书（Vite dev server）
cd ../../frontend
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout key.pem -out cert.pem -days 3650 \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

证书文件（`*.p12`, `*.jks`, `key.pem`, `cert.pem`）已在 `.gitignore` 排除，不会提交到仓库。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `https://localhost:8080`（HTTPS，自签名证书）。

首次启动 Flyway 会自动执行建表迁移，并创建一个默认管理员 `admin / admin123`。

### 4. 启动前端

```bash
cd frontend
npm install    # 仅首次
npm run dev
```

浏览器打开 `http://localhost:5173`。前端开发服务器会将 `/api` 请求自动代理到后端 `localhost:8080`。

### 5. 运行测试

```bash
cd backend
mvn test
```

测试使用 H2 内存数据库，无需 MySQL。

---

## 配置文件说明

### 后端 — `backend/src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_help?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root          # 修改为你的数据库用户名
    password: root          # 修改为你的数据库密码
  flyway:
    enabled: true           # 应用启动自动执行数据库迁移
    baseline-on-migrate: true
```

如需修改数据库密码，只改 `username` 和 `password` 即可。注意 JDBC URL 中字符集写 `UTF-8`（Java 标准名），不是 `utf8mb4`（MySQL 内部名）。

### 前端环境变量

前端开发环境通过 Vite 内置代理将 `/api` 请求转发到后端 `localhost:8080`，无需额外配置 `VITE_API_BASE`。生产环境构建时使用 `/api/v1`（由 Nginx 代理）。

如需自定义 API 地址，可创建 `frontend/.env.development`：

```
VITE_API_BASE=http://localhost:5173/api/v1
```

### 后端环境变量

以下敏感配置支持通过环境变量覆盖（默认值适用于本地开发）：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `JWT_SECRET` | (内置 Base64 密钥) | JWT 签名密钥 |
| `DB_PASSWORD` | `root` | MariaDB 数据库密码 |

---

## 数据库维护

### 新迁移（加表 / 改表）

在 `backend/src/main/resources/db/migration/` 下新建递增版本文件：

```
V1__init_user_tables.sql            # user + privacy_profile + user_account
V2__create_demand_table.sql         # demand（reward_type / reward_amount）
V3__add_acceptor_to_demand.sql      # 接单人字段
V4__create_notification_table.sql   # 通知表
V5__create_evaluation_table.sql     # 评价表
V6__create_chat_tables.sql          # 会话 + 消息表
V7__add_images_to_demand.sql        # 需求图片列
V8__add_image_to_message.sql        # 消息图片列
V9__add_attributes_to_demand.sql    # 需求属性 JSON 列（类型个性化）
V10__create_team_member_table.sql   # 组队多人模型
V11__create_points_tables.sql       # 积分流水 + 每日签到（当前最新）
```

**规则**：已应用的迁移文件永远不要修改。Flyway 会对已执行文件做 checksum 校验，修改会直接报错。

### 重置数据库

```bash
# 删库重建（下次启动 Flyway 自动重新迁移）
sudo mariadb -u root -e "DROP DATABASE campus_help; CREATE DATABASE campus_help DEFAULT CHARACTER SET utf8mb4;"
```

### 数据迁移到另一台服务器

```bash
# 导出（包含 flyway_schema_history 表）
mariadb-dump -u root campus_help > dump.sql

# 目标服务器导入
mariadb -u root -e "CREATE DATABASE campus_help DEFAULT CHARACTER SET utf8mb4;"
mariadb -u root campus_help < dump.sql
```

---

## 默认账号

| 角色 | 学号 | 密码 |
|------|------|------|
| 管理员 | admin | admin123 |

管理员由 `DataInitializer` 在首次启动时自动创建（仅当数据库中无 ADMIN 用户时）。

---

## 前端页面路由

| 路径 | 说明 | 权限 |
|------|------|------|
| `/login` | 登录 | 游客 |
| `/register` | 注册 | 游客 |
| `/` | 首页（签到、功能入口、快捷入口） | 登录用户 |
| `/profile` | 个人资料 + 隐私设置 | 登录用户 |
| `/demands` | 需求广场（筛选/搜索/排序） | 登录用户 |
| `/demands/publish` | 发布需求 | 登录用户 |
| `/demands/:id` | 需求详情（含组队审批） | 登录用户 |
| `/orders` | 我的订单 | 登录用户 |
| `/points/history` | 积分明细（余额/流水） | 登录用户 |
| `/notifications` | 通知列表 | 登录用户 |
| `/chat/:id` | 私信聊天 | 登录用户 |
| `/admin/users` | 用户管理（封禁/解封） | ADMIN |

---

## 团队

| 角色 | 成员 |
|------|------|
| 需求负责人 | 侯乔岳 |
| 架构负责人 | 杨佳兴 |
| 开发负责人 | 胡皓轩 |
| 测试负责人 | 沈诺 |
