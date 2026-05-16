# 校园互助服务平台 (Campus Mutual Help Platform)

面向在校大学生的互助服务系统，支持跑腿代取、二手交易、组队匹配、失物招领等功能。平台采用积分激励体系，学生通过完成互助任务获得积分，同时支持在线聊天与双方互评。

---

## 项目结构

```
/
├── backend/                    # 后端 Spring Boot 项目（Java 17）
│   ├── src/main/java/cn/seecoder/campushelp/
│   │   ├── common/             # ApiResult、全局异常处理
│   │   ├── config/             # Security、CORS、MyBatis-Plus、数据初始化
│   │   ├── security/           # JWT 生成与认证过滤器
│   │   ├── entity/             # 数据实体（User、PrivacyProfile、UserAccount）
│   │   ├── dto/                # 请求/响应 DTO
│   │   ├── mapper/             # MyBatis-Plus Mapper 接口
│   │   ├── service/            # 业务服务接口 + 实现
│   │   └── controller/         # REST 控制器（UserController、AdminController）
│   └── src/main/resources/
│       ├── application.yml           # 全局配置
│       ├── application-dev.yml       # 开发环境配置
│       └── db/migration/             # Flyway 数据库迁移脚本
├── frontend/                   # 前端 Vue 3 项目（Vite + Vant UI）
│   └── src/
│       ├── router/             # 路由配置（含导航守卫）
│       ├── stores/             # Pinia 状态管理（auth store）
│       ├── api/                # Axios 封装 + API 函数
│       ├── views/              # 页面组件（Login、Register、Home、Profile）
│       │   └── admin/          # 管理后台页面（UserList）
│       └── styles/             # 全局样式
└── docs/                       # 阶段文档（P1 需求 / P2 架构 / P3 详细设计）
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
| 缓存 | Redis | Token 黑名单等（预留） |
| 前端框架 | Vue 3 + Vite | Composition API |
| UI 组件库 | Vant 4 | 移动端优先 |
| 状态管理 | Pinia | 用户认证状态 |
| HTTP 客户端 | Axios | 拦截器自动注入 JWT |

## 快速开始

### 环境要求

- **JDK 17+**
- **Maven 3.8+**
- **MariaDB**（Arch Linux: `sudo pacman -S mariadb`）
- **Redis**（Arch Linux: `sudo pacman -S redis`）
- **Node.js 18+**

### 1. 初始化数据库

```bash
# 初始化 MariaDB 数据目录（仅首次）
sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql

# 启动服务
sudo systemctl start mariadb redis

# 创建数据库
sudo mariadb -u root -e "CREATE DATABASE IF NOT EXISTS campus_help DEFAULT CHARACTER SET utf8mb4;"
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

首次启动 Flyway 会自动执行建表迁移，并创建一个默认管理员 `admin / admin123`。

### 3. 启动前端

```bash
cd frontend
npm install    # 仅首次
npm run dev
```

浏览器打开 `http://localhost:5173`。前端开发服务器会将 `/api` 请求自动代理到后端 `localhost:8080`。

### 4. 运行测试

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

### 前端 — `frontend/.env.development`

```
VITE_API_BASE=http://localhost:5173/api/v1   # 开发环境，走 Vite 代理
```

生产环境构建时自动切换为 `.env.production` 中的 `/api/v1`（由 Nginx 代理）。

---

## 数据库维护

### 新迁移（加表 / 改表）

在 `backend/src/main/resources/db/migration/` 下新建递增版本文件：

```
V1__init_user_tables.sql    # 已执行——绝不修改
V2__add_order_tables.sql    # 下次新增的迁移
V3__alter_user_add_phone.sql
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
| `/` | 首页 | 登录用户 |
| `/profile` | 个人资料 + 隐私设置 | 登录用户 |
| `/admin/users` | 用户管理（封禁/解封） | ADMIN |

---

## 团队

| 角色 | 成员 |
|------|------|
| 需求负责人 | 侯乔岳 |
| 架构负责人 | 杨佳兴 |
| 开发负责人 | 胡皓轩 |
| 测试负责人 | 沈诺 |
