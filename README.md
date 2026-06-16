# 校园互助服务平台 (Campus Mutual Help Platform)

> 面向在校大学生的全功能互助服务系统，以积分经济为核心，连接"求助者"与"助人者"。
> 支持跑腿代取、二手交易、组队匹配、失物招领、学习互助、其他共六大需求类型，
> 覆盖需求发布→接单→完成→互评的完整生命周期，配备实时私信沟通、团队协作管理、
> 积分流水追溯、信用评价体系与匿名隐私保护等全套基础设施。

---

## 目录

- [功能全景](#功能全景)
- [技术架构](#技术架构)
- [安全体系](#安全体系)
- [积分经济系统](#积分经济系统)
- [信用评价体系](#信用评价体系)
- [UI/UX 设计](#uiux-设计)
- [数据库设计](#数据库设计)
- [API 设计规范](#api-设计规范)
- [前端架构](#前端架构)
- [工程质量](#工程质量)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [数据库维护](#数据库维护)
- [演示数据](#演示数据)
- [团队](#团队)

---

## 功能全景

平台围绕校园互助场景设计了六大需求类型，每种类型拥有专属的属性结构和交互流程：

### 六大需求类型

| 类型 | 标识 | 图标 | 典型场景 | 专属属性 | 报酬模式 |
|------|------|------|----------|----------|----------|
| **跑腿代取** | `errand` | logistics | 代取快递、代买早餐、代还图书 | 取件地点、物品类别、紧急程度 | 积分悬赏 / 公益 |
| **二手交易** | `trade` | shop | 出售教材、闲置电子设备、日用品 | 商品成色、交易品类 | 积分定价 |
| **组队匹配** | `team` | friends | 竞赛组队、运动搭子、自习伙伴 | 队伍人数、技能标签、队伍类型 | 公益（默认） |
| **失物招领** | `lost_found` | search | 丢失校园卡、捡到钥匙/U盘 | 丢失/捡到类型、物品类别、日期 | 公益（默认） |
| **学习互助** | `study` | bookmark-o | 辅导高数、C语言答疑、四级辅导 | 学科、模式（线上/线下）、难度 | 积分悬赏 / 公益 |
| **其他** | `other` | ellipsis | 帮忙搬家、装系统、修车 | — | 积分悬赏 / 公益 |

### 需求生命周期

每条需求经历严格的状态机流转，全程可追溯：

```
OPEN ──→ IN_PROGRESS ──→ COMPLETED
  │           │
  └──→ CANCELLED        └──→ CANCELLED
```

| 状态 | 含义 | 触发操作 | 积分行为 |
|------|------|----------|----------|
| **OPEN** | 待接单，展示在需求广场 | 发布需求 | 积分悬赏型：冻结发布者可用积分 |
| **IN_PROGRESS** | 已有人接单，进行中 | 他人接单 | 无变化（积分仍冻结） |
| **COMPLETED** | 双方确认完成 | 发布者确认完成 | 冻结积分转移至接单人，双方记录流水 |
| **CANCELLED** | 已取消 | 发布者取消 | 冻结积分解冻退还，记录退款流水 |

**组队类型**的生命周期独立于积分流转——队长即发布者，队员通过"申请→审批"机制加入，满员或队长解散时结束。

### 用户系统

- **注册** — 学号（唯一凭证）+ 姓名 + 密码（BCrypt 加密，最低 6 位），注册即送 100 积分
- **登录** — 学号 + 密码，返回 JWT Token（含 userId、studentId、role），支持 Token 持久化
- **个人资料** — 头像上传（Multipart，服务端校验 MIME）、姓名修改、密码修改（需验证旧密码）
- **角色体系** — USER（普通用户）与 ADMIN（管理员），管理员拥有独立的用户管理后台

### 签到系统

- **每日签到** — 每天限签 1 次，数据库 UNIQUE(user_id, checkin_date) 约束防重复
- **连续签到加成** — 基础 5 分，根据连续天数阶梯递增：
  - 连续 1-2 天：5 分
  - 连续 3-4 天：8 分（+3 加成）
  - 连续 5-6 天：10 分（+5 加成）
  - 连续 7 天及以上：15 分（+10 加成，封顶）
- **断签重置** — 昨天未签则连续天数重置为 1
- **签到状态查询** — 无需签到即可查看今日签到状态与当前连续天数

### 积分系统（详见[积分经济系统](#积分经济系统)）

- **积分流水** — 六种交易类型，每笔变动记录 amount、balance_after、reference_id，形成不可篡改的账本
- **积分明细页** — 可用/冻结余额总览 + 按类型筛选的分页流水列表
- **SELECT ... FOR UPDATE 行级锁** — 防止并发签到/发布/完成导致的积分不一致

### 信用评价体系（详见[信用评价体系](#信用评价体系)）

- **双向互评** — COMPLETED 后双方各给对方打分（1-5 星 + 可选文字评价）
- **信用分公式** — `0.6 × 评价均分 + 0.4 × (完成率 × 5)`，新用户冷启动默认满分 5.0
- **信用分展示** — 需求详情页显示发布者/接单人的信用分，作为信任参考

### 组队协作

- **多人队伍** — 发布者自动成为队长（LEADER），其他用户可申请加入（MEMBER）
- **申请审批流** — 申请（PENDING）→ 队长审批（JOINED / REJECTED），发送通知
- **队伍容量** — 依据发布时设定的 team_size 自动判断"已满员"状态
- **退出与踢出** — 队员可主动退出，队长可移除队员（队长不可被移除）
- **队伍解散** — 队长取消需求即解散队伍，通知所有已加入成员

### 实时私信

- **基于需求的对话** — 每条对话绑定一条需求 + 两个用户，确保沟通上下文清晰
- **文本 + 图片消息** — 支持文字消息和图片消息两种类型
- **WebSocket 实时推送** — 采用 STOMP over WebSocket 协议，消息和通知实时送达，无需轮询
- **未读计数** — 全局未读消息数角标，对话列表按最近消息时间排序
- **会话创建去重** — 同一需求 + 同一对用户仅创建一个会话（deterministic user_id 排序 + UNIQUE 约束 + DuplicateKeyException 容错）
- **Emoji 选择器** — 前端集成 Emoji 面板，支持快速插入表情符号

### 通知中心

- **七种通知类型** — 接单(ACCEPT)、完成(COMPLETE)、取消(CANCEL)、评价(EVALUATION)、组队申请(JOIN_REQUEST)、申请通过(REQUEST_APPROVED)、申请拒绝(REQUEST_REJECTED)、举报处理(REPORT_RESOLVED)
- **未读标记** — 未读通知以紫色左边框 + 脉动圆点标识，全局未读数角标
- **已读管理** — 单条标记已读 / 一键全部已读
- **导航联动** — 每条通知携带 related_demand_id，点击即跳转到对应需求详情

### 成就徽章系统

- **9 种成就徽章** — 首次发布(🎉)、首次接单(🤝)、十全十美(🏆)、五星好评(⭐)、百星好评(💯)、签到达人(🔥)、乐于助人(💝)、正义使者(🛡️)、彩蛋猎人(🐱)
- **自动检测颁发** — 发布需求/接单/完成/签到/举报处理时自动检测条件，达成即颁发
- **徽章佩戴** — 从已获得徽章中选择一枚佩戴，显示在头像角标上（全局可见）
- **彩蛋隐藏条件** — EASTER_EGG 徽章达成条件对用户隐藏，触发后全屏动效展示

### 举报系统

- **多态举报目标** — 支持举报需求（DEMAND）、用户（USER）、消息（MESSAGE）
- **五种举报原因** — 虚假信息(MISLEADING)、骚扰/不当言论(HARASSMENT)、违禁品/违规(ILLEGAL)、垃圾广告(SPAM)、其他(OTHER)
- **管理处理流程** — 待处理→已处理/驳回，支持关联操作（下架需求、封禁用户）
- **举报成就联动** — 首次举报被确认处理后获得"正义使者"徽章

### 需求收藏

- **书签功能** — 用户可收藏感兴趣的需求，在"我的收藏"中统一查看
- **幂等设计** — UNIQUE(demand_id, user_id) 约束保证不重复收藏，重复操作不报错

### 需求编辑

- **发布后修改** — 发布者可在 OPEN 状态下编辑需求的标题、描述、地点、截止时间等信息

### 匿名与隐私

- **匿名发布** — 发布需求时可选择匿名，其他用户看到的是遮罩名称（如"热心市民小王"）而非真实姓名
- **隐私设置** — 独立隐私配置页，可随时开关匿名模式、自定义虚拟昵称
- **隐私展示一致性** — 需求广场卡片、需求详情、订单列表、聊天页面等处统一应用匿名规则

### 管理后台

- **仪表盘概览** — 用户/需求/积分/举报四大维度统计（总量、今日新增、活跃指标、类型分布、签到率）
- **用户管理** — 分页列表 + 学号/姓名关键词搜索，展示头像、角色、账户状态、佩戴徽章
- **需求管理** — 按类型/状态/关键词筛选，支持管理员直接硬删除需求
- **举报管理** — 待处理/已处理/已驳回三栏筛选，一键处理（确认/驳回）+ 关联操作（下架需求/封禁用户）
- **封禁/解封** — 一键操作，被封禁用户在下次请求时被 JWT 过滤器拦截（状态码 403）
- **自我保护** — 管理员不可封禁自己

---

## 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────┐
│                    Frontend                      │
│        Vue 3 + Vite + Vant 4 + Pinia            │
│              https://localhost:5173              │
│          (Vite proxy → localhost:8080)           │
└────────────────────┬────────────────────────────┘
                     │ HTTPS (self-signed cert)
                     │ REST API + JWT Bearer Token
┌────────────────────┴────────────────────────────┐
│                    Backend                       │
│        Spring Boot 3.2.5 + Spring Security       │
│              https://localhost:8080              │
│  ┌──────────────────────────────────────────┐   │
│  │          Controller Layer (10 个)          │   │
│  │  User / Demand / Points / TeamMember      │   │
│  │  Evaluation / Chat / Notification / Admin │   │
│  │  Badge / Report                          │   │
│  ├──────────────────────────────────────────┤   │
│  │          Service Layer (14 个)             │   │
│  │  业务逻辑 + @Transactional 事务边界        │   │
│  ├──────────────────────────────────────────┤   │
│  │          Mapper Layer (17 个)              │   │
│  │  MyBatis-Plus LambdaQueryWrapper 封装     │   │
│  ├──────────────────────────────────────────┤   │
│  │          Security Layer                    │   │
│  │  JwtTokenProvider + JwtAuthFilter         │   │
│  │  + SecurityConfig (stateless)             │   │
│  └──────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────┘
                     │ JDBC
┌────────────────────┴────────────────────────────┐
│              MySQL / MariaDB                      │
│          database: campus_help                    │
│   15 tables + Flyway versioned migrations        │
└─────────────────────────────────────────────────┘
```

### 技术栈详表

| 层次 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| **后端框架** | Spring Boot | 3.2.5 | 企业级生态，自动配置，内嵌 Tomcat |
| **语言** | Java | 17 | LTS 版本，records/sealed classes 等现代特性 |
| **安全框架** | Spring Security | 6.x | 过滤器链 + 方法级授权 |
| **JWT 库** | jjwt | 0.12.5 | 现代 API 设计（Builder 模式 + Key 类型安全） |
| **ORM** | MyBatis-Plus | 3.5.6 | Lambda 查询避免字符串字段名，分页插件，自动填充 |
| **数据库迁移** | Flyway | — | 版本化 SQL 迁移（V1–V14），checksum 校验防篡改 |
| **实时通信** | WebSocket + STOMP | (Spring) | 消息和通知实时推送，替代轮询 |
| **密码加密** | BCrypt | (Spring) | 自适应哈希，strength=10（2^10 轮） |
| **JSON 处理** | Jackson | (Spring) | 需求 attributes JSON 列序列化/反序列化 |
| **测试数据库** | H2 | — | 内存模式，测试隔离，无需 MySQL |
| **前端框架** | Vue | 3.x | Composition API，响应式系统，TypeScript-ready |
| **构建工具** | Vite | 5.x | 极速 HMR，ESBuild 预构建，Tree Shaking |
| **UI 库** | Vant | 4.x | 移动端优先，70+ 组件，主题定制 |
| **状态管理** | Pinia | 2.x | Vue 3 官方推荐，DevTools 支持，模块化 |
| **路由** | Vue Router | 4.x | 懒加载、导航守卫、路由元信息 |
| **HTTP 客户端** | Axios | 1.x | 拦截器链、自动 JSON 转换、超时控制 |

### 关键设计决策

#### 1. 单表多类型需求设计

六种需求类型共享一张 `demand` 表，类型专属属性以 **JSON 字符串**存储在 `attributes` 列中。

**优势**：
- 避免六张表的笛卡尔积式 JOIN
- 需求广场统一查询天然支持跨类型搜索和排序
- 新增需求类型只需扩展 JSON schema，无需 DDL 变更
- MyBatis-Plus + Jackson 自动完成 JSON ↔ Java Map 的序列化

**类型安全**：后端 Service 层对每种类型的 attributes 做结构化校验（如 errand 必须含 pickup_location，lost_found 必须含 lf_type 为 LOST/FOUND），前端通过 `constants/demandTypes.js` 中的 TYPE_CONFIG 驱动动态表单。

#### 2. 无状态 JWT 认证

- 服务端不存储任何会话状态，每个请求自包含身份信息
- Token 负载：`{ sub: userId, studentId, role }`，HMAC-SHA256 签名
- 封禁用户在 JWT 过滤器中直接拦截，无需等到业务层
- 前端 Axios 拦截器自动注入 Bearer Token，401 时自动清除登录态并跳转

#### 3. 不可变积分账本

`points_transaction` 表中的记录一旦写入永不修改。每笔交易的 `balance_after` 字段快照了交易后的可用余额，使得任何时刻的账户余额都可以被完整审计。配合 `SELECT ... FOR UPDATE` 悲观锁，保证并发场景下的积分一致性。

#### 4. 批加载防 N+1

需求列表、我的订单、用户管理等涉及多实体关联查询的接口，均采用"先查主表，再批量加载关联实体"的模式，而非逐条 JOIN 或逐条子查询。例如需求广场接口：查需求列表 → 收集 publisher_id 集合 → 一次 IN 查询加载所有发布者信息。

#### 5. 确定性会话 ID

私信会话表的 `UNIQUE(demand_id, user1_id, user2_id)` 约束依赖确定性排序：`user1_id = MIN(user_a, user_b), user2_id = MAX(user_a, user_b)`。这样无论 A 向 B 还是 B 向 A 发起会话，都映射到同一行，避免双向重复会话。

---

## 安全体系

### 认证流程

```
用户登录 → BCrypt 密码验证 → 生成 JWT（含 userId/studentId/role）
→ 返回 Token → 前端 localStorage 持久化
→ 后续请求：Authorization: Bearer <token>
→ JwtAuthenticationFilter 解析 Token → 查库验证用户未被封禁
→ 构建 Authentication → 注入 SecurityContext
→ Controller 通过 @AuthenticationPrincipal 获取当前用户
```

### 安全措施清单

| 措施 | 实现 |
|------|------|
| **密码存储** | BCrypt 自适应哈希，strength=10，每密码独立随机盐 |
| **传输加密** | 全站 HTTPS（前后端均启用 TLS，自签名证书用于本地开发） |
| **无状态认证** | JWT + HMAC-SHA256，服务端零会话存储，天然水平扩展友好 |
| **CSRF 防护** | REST API 架构，禁用 CSRF（不依赖 Cookie 传递凭据） |
| **封禁即生效** | JWT 过滤器每次请求查库验证用户 status，封禁用户下次请求即被拦截 |
| **密码修改验证** | 修改密码需提供旧密码，防止 Token 泄露后被恶意改密 |
| **文件上传校验** | 头像和聊天图片上传校验 MIME 类型，限制为常见图片格式 |
| **访问控制** | 管理员接口 `ROLE_ADMIN` 守卫；用户资料/订单等接口校验数据归属 |
| **会话安全** | 私信会话创建时校验请求者必须是需求发布者、接单人或队员之一 |
| **操作权限** | 接单不能接自己的需求；确认完成只能发布者操作；取消只能发布者操作 |

### 安全配置 (SecurityConfig.java)

```java
// 公开端点（无需认证）
"/api/v1/user/register", "/api/v1/user/login", "/uploads/**"

// 管理员端点（需 ROLE_ADMIN）
"/api/v1/admin/**"

// WebSocket 端点（公开，STOMP 层自行认证）
"/ws/**"

// 其他所有端点（需认证）
"/api/v1/**"
```

---

## 积分经济系统

平台使用统一积分作为内部流通货币，完全取消了现金交易，形成"帮助他人→获取积分→发布需求→消耗积分"的正向循环。

### 积分流转全图

```
注册赠送 (+100)
    │
    ├──→ 可用积分 ──→ 发布需求(冻结) ──→ 冻结积分
    │                                        │
    │                   ┌─────────────────────┤
    │                   │ 取消(解冻)          │ 完成(转移)
    │                   ▼                     ▼
    │              可用积分 (+N)        接单人可用积分 (+N)
    │                                        │
    └──→ 每日签到 (+5~15)                    │
                                             │
    双方各记一笔 COMPLETE_EARN 流水 ←────────┘
```

### 六种流水类型

| 类型 | 触发时机 | 金额符号 | 含义 |
|------|----------|----------|------|
| `SIGNUP_BONUS` | 用户注册 | +100 | 注册奖励 |
| `DAILY_CHECKIN` | 每日签到 | +5~+15 | 签到奖励（含连续加成） |
| `PUBLISH` | 发布积分悬赏需求 | -N | 冻结可用积分 |
| `CANCEL_REFUND` | 取消需求 | +N | 解冻退还 |
| `COMPLETE_EARN` | 需求完成 | -N(发布者) / +N(接单人) | 积分转移 |
| `ADMIN_ADJUST` | 管理员操作 | ±N | 管理调节（预留） |

### 并发安全

签到和积分操作的关键路径使用 **`SELECT ... FOR UPDATE` 悲观行锁**：

```java
// PointsServiceImpl 中的锁模式
UserAccount account = userAccountMapper.selectForUpdate(userId);
// 后续的积分计算和更新在同一事务中完成
```

这确保了同一用户在高并发场景下（如同时签到、同时发布需求、同时被确认完成）不会出现积分不一致。

### 积分 API

| 方法 | 路径 | 说明 | 幂等性 |
|------|------|------|--------|
| POST | `/api/v1/points/checkin` | 每日签到 | 重复签到返回 409 |
| GET | `/api/v1/points/checkin/status` | 签到状态查询 | 只读 |
| GET | `/api/v1/points/transactions` | 积分流水（分页+类型筛选） | 只读 |

---

## 信用评价体系

用户信用分是平台上衡量可靠程度的核心指标，影响其他用户决定是否与你交易。

### 计算公式

```
信用分 = 0.6 × 评价均分 + 0.4 × (完成率 × 5)

其中：
  评价均分 = 所有收到评价的 rating 算术平均（无评价时默认 5.0）
  完成率 = COMPLETED / (COMPLETED + CANCELLED)（作为接单人维度，无记录时默认 1.0）
  信用分结果四舍五入保留一位小数，取值范围约 [0.6, 5.0]
```

### 设计原理

| 维度 | 权重 | 理由 |
|------|------|------|
| **评价均分** | 0.6 | 他人直接评价是最强信用信号——好就是好，差就是差 |
| **完成率** | 0.4 | 平衡"评分高但频繁取消"的投机行为——守信履约同样重要 |
| **冷启动默认满分** | — | 新用户无历史记录时不应被歧视，随交易积累收敛至真实水平 |
| **完成率归一化** | ×5 | 将 [0,1] 的完成率映射到 [0,5]，与评价均分同区间，方可加权求和 |

### 典型场景示例

| 用户画像 | 评价均分 | 完成率 | 计算 | 信用分 |
|----------|----------|--------|------|--------|
| 完美用户 | 5.0 | 100% (10/10) | 0.6×5.0 + 0.4×5.0 | **5.0** |
| 靠谱但偶有取消 | 4.5 | 80% (8/10) | 0.6×4.5 + 0.4×4.0 | **4.3** |
| 高评分但常取消 | 5.0 | 50% (5/10) | 0.6×5.0 + 0.4×2.5 | **4.0** |
| 中等评分高完成率 | 3.0 | 100% (1/1) | 0.6×3.0 + 0.4×5.0 | **3.8** |
| 差评用户 | 1.0 | 100% (1/1) | 0.6×1.0 + 0.4×5.0 | **2.6** |
| 新用户 | 无评价(默认5.0) | 无记录(默认1.0) | 0.6×5.0 + 0.4×5.0 | **5.0** |

### 双向互评机制

- 需求完成后，发布者和接单人 **各自独立**给对方打分
- 评价包含 1-5 星评分和可选文字评价
- 每对 (demand_id, evaluator_id) 仅允许一条评价（UNIQUE 约束）
- 评价可更新（仅评价者本人），更新后重新计算对方信用分
- 组队类型的需求不参与评价体系（多人协作难以归因到单一个体）

---

## UI/UX 设计

### 设计系统：Material 3 Expressive (M3E)

前端采用完整的 Material 3 Expressive 设计系统，通过 **200+ 行 CSS 自定义属性**实现统一的设计语言：

- **色彩体系** — 紫色主色调（#6750A4），完整的 primary/secondary/tertiary/error 色阶，支持 light 主题
- **圆角系统** — xs(4px) / sm(8px) / md(12px) / lg(16px) / xl(20px) / full(50%)
- **阴影层级** — 6 级阴影（elevation-0 到 elevation-5），模拟 Material 高度系统
- **间距系统** — 基于 4px 网格的 spacing 体系（xs=4px 到 xxl=64px）
- **字体系统** — display/headline/title/label/body 五级排版层级，含字号、字重、行高
- **动效系统** — M3 标准 duration token + spring 缓动曲线（cubic-bezier 含轻微 overshoot）
- **Vant 主题覆盖** — 深度覆写 Vant 4 默认样式变量，使其融入 M3 设计语言

### 响应式设计

所有页面采用**移动端优先（Mobile-First）**策略，两档断点：

| 断点 | 宽度 | 布局策略 |
|------|------|----------|
| **Mobile（默认）** | < 768px | 全宽卡片流、底部导航、FAB 浮动按钮 |
| **Desktop** | ≥ 768px | 最大宽度约束（1200px）、双列/三列网格、数据表格替代卡片、悬停效果 |

**响应式适配示例**：
- 登录/注册页：移动端上下堆叠（品牌区 + 表单各占全宽），桌面端左右分栏（品牌区 50% + 表单卡片 50%）
- 需求广场：移动端无限滚动卡片流 + 顶部搜索/筛选栏，桌面端数据表格 + 固定侧边筛选
- 首页功能网格：移动端 2 列，桌面端 3 列
- 个人资料页：移动端单列，桌面端基本信息/隐私设置双列

### 视觉细节

- **玻璃拟态（Glass Morphism）** — 首页积分统计栏和签到卡片采用 `backdrop-filter: blur()` 半透明玻璃质感
- **品牌氛围动画** — 登录/注册页装饰性浮动色块（M3 Spring 缓动，无限循环交错动画）
- **时间感知问候** — 首页根据当前时段显示"早上好/下午好/晚上好"
- **类型色彩系统** — 六种需求类型各有独立配色（跑腿=橙、交易=绿、组队=蓝、失物=紫、学习=红、其他=灰），在类型标签、发布表单、需求卡片中统一应用
- **状态语义色** — OPEN=蓝、IN_PROGRESS=琥珀、COMPLETED=绿、CANCELLED=红
- **空状态与错误状态** — 所有列表/搜索结果均有空状态占位图和提示文字，网络错误提供重试按钮

### 可访问性

- **键盘导航** — 所有可交互元素均自定义 `focus-visible` 样式（紫色光环 + 缩放反馈）
- **语义化 HTML** — 合理的 heading 层级、button/label 标签、form 语义元素
- **颜色对比度** — 文本与背景色符合 WCAG AA 标准
- **触控优化** — 移动端按钮最小 44×44px 触控区域，卡片间距充足防误触

---

## 数据库设计

### 核心 ER 关系

```
user (1) ──── (1) privacy_profile    1:1 隐私配置
user (1) ──── (1) user_account       1:1 积分账户
user (1) ──── (N) demand              作为发布者
user (1) ──── (N) demand              作为接单人 (acceptor)
user (1) ──── (N) points_transaction  积分流水
user (1) ──── (N) daily_checkin       签到记录
user (1) ──── (N) notification        通知
user (1) ──── (N) evaluation          评价（评价者/被评价者）
user (N) ──── (N) conversation        私信会话（user1/user2）
demand (1) ──── (N) conversation      会话绑定需求
demand (1) ──── (N) team_member       队伍成员
demand (1) ──── (N) evaluation        评价绑定需求
conversation (1) ──── (N) message     会话消息
```

### 数据表清单（15 张）

| 表名 | 行数规模 | 核心索引 |
|------|----------|----------|
| `user` | ~N 用户 | UNIQUE(student_id), INDEX(role) |
| `privacy_profile` | = N | UNIQUE(user_id) |
| `user_account` | = N | UNIQUE(user_id) |
| `demand` | ~M 需求 | INDEX(publisher_id, status, type), FULLTEXT(title, description) |
| `team_member` | ~M×k 队伍 | UNIQUE(demand_id, user_id), INDEX(status) |
| `evaluation` | ~M_completed×2 | UNIQUE(demand_id, evaluator_id), INDEX(target_user_id) |
| `conversation` | ~M_conversations | UNIQUE(demand_id, user1_id, user2_id) |
| `message` | ~消息数 | INDEX(conversation_id, create_time) |
| `notification` | ~通知数 | INDEX(user_id, is_read, create_time) |
| `points_transaction` | ~流水数 | INDEX(user_id, create_time), INDEX(type) |
| `daily_checkin` | ~签到数 | UNIQUE(user_id, checkin_date) |
| `user_favorite` | ~M_fav | UNIQUE(demand_id, user_id) |
| `report` | ~举报数 | INDEX(target_type, target_id), INDEX(status) |
| `user_badge` | ~N×k_badge | UNIQUE(user_id, badge_key) |
| `worn_badge` | ≤ N | UNIQUE(user_id) |

### 数据库迁移策略

采用 **Flyway 版本化迁移**，共 14 版迁移脚本（V1–V14），存储在 `backend/src/main/resources/db/migration/`：

| 版本 | 内容 | 关键 DDL |
|------|------|----------|
| V1 | 用户基础表 | user + privacy_profile + user_account |
| V2 | 需求表 | demand（含 reward_type, reward_amount） |
| V3 | 接单人 | demand 增加 acceptor_id 字段 |
| V4 | 通知表 | notification（7 种类型） |
| V5 | 评价表 | evaluation（含 UNIQUE 约束） |
| V6 | 私信表 | conversation + message |
| V7 | 需求图片 | demand 增加 images 列（逗号分隔 URL） |
| V8 | 消息图片 | message 增加 image_url 列 |
| V9 | 需求属性 JSON | demand 增加 attributes 列（TEXT，JSON 格式） |
| V10 | 组队表 | team_member（role + status + 申请消息） |
| V11 | 积分表 | points_transaction + daily_checkin |
| V12 | 收藏表 | user_favorite（UNIQUE constraint） |
| V13 | 举报表 | report（多态 target + 5 种原因 + 处理流程） |
| V14 | 徽章表 | user_badge + worn_badge（成就系统） |

**原则**：已应用的迁移文件永不修改（Flyway checksum 校验）。

### 数据完整性

- **外键约束** — 核心关联使用 FOREIGN KEY（user→demand, user→evaluation 等）
- **级联删除** — 用户拥有的数据（privacy_profile, user_account, points_transaction, daily_checkin）采用 ON DELETE CASCADE
- **引用保护** — demand 的 acceptor_id 使用 ON DELETE SET NULL（删除用户不影响已有需求接单记录）
- **UNIQUE 约束** — 关键业务唯一性全部通过数据库约束保证（学号、签到日、评价人、会话对、队伍成员）

---

## API 设计规范

### 统一响应格式

所有 API 响应遵循统一结构：

```json
{
  "code": 200,
  "message": "success",
  "body": {
    "data": { ... },
    "total": 100,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

- 成功：`code: 200`，数据在 `body.data` 中
- 业务错误：`code: 4xx`（如 409 重复签到、400 参数校验失败）
- 认证错误：`code: 401`（Token 缺失/无效/过期）
- 授权错误：`code: 403`（非管理员访问管理接口、账号被封禁）
- 全局异常处理：`@RestControllerAdvice` 统一捕获并格式化异常响应

### 完整 API 清单

#### 用户模块 (UserController)

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/v1/user/register` | 否 | 注册（送 100 积分） |
| POST | `/api/v1/user/login` | 否 | 登录（返回 JWT） |
| GET | `/api/v1/user/profile` | 是 | 获取个人资料（含积分/信用分） |
| PUT | `/api/v1/user/profile` | 是 | 更新个人资料（姓名/头像/隐私） |
| POST | `/api/v1/user/avatar` | 是 | 上传头像（Multipart） |
| PUT | `/api/v1/user/password` | 是 | 修改密码（需旧密码验证） |

#### 需求模块 (DemandController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/demands` | 发布需求 |
| GET | `/api/v1/demands` | 需求广场（分页+类型/关键词筛选+排序） |
| GET | `/api/v1/demands/{id}` | 需求详情（含发布者/接单人/队员） |
| PUT | `/api/v1/demands/{id}/accept` | 接单 |
| PUT | `/api/v1/demands/{id}/complete` | 确认完成 |
| PUT | `/api/v1/demands/{id}/cancel` | 取消需求 |
| GET | `/api/v1/demands/my` | 我的订单（publisher/acceptor 角色筛选） |
| GET | `/api/v1/demands/my/team` | 我的队伍需求 |

#### 组队模块 (TeamMemberController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/demands/{id}/team/apply` | 申请加入队伍 |
| PUT | `/api/v1/demands/{id}/team/applicants/{uid}/approve` | 批准申请 |
| PUT | `/api/v1/demands/{id}/team/applicants/{uid}/reject` | 拒绝申请 |
| POST | `/api/v1/demands/{id}/team/leave` | 退出队伍 |
| DELETE | `/api/v1/demands/{id}/team/members/{uid}` | 移除队员 |
| GET | `/api/v1/demands/{id}/team/members` | 队伍成员列表 |
| GET | `/api/v1/demands/{id}/team/applicants` | 待审批申请人列表 |

#### 积分模块 (PointsController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/points/checkin` | 每日签到 |
| GET | `/api/v1/points/checkin/status` | 签到状态 |
| GET | `/api/v1/points/transactions` | 积分流水（分页+类型筛选） |

#### 评价模块 (EvaluationController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/evaluations` | 创建评价 |
| PUT | `/api/v1/evaluations/{id}` | 更新评价 |
| GET | `/api/v1/evaluations/demand/{id}` | 查看需求的所有评价 |
| GET | `/api/v1/evaluations/mine` | 查看我对某需求的评价 |
| GET | `/api/v1/evaluations/user/{id}` | 查看某用户收到的评价 |

#### 私信模块 (ChatController)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/chat/conversations` | 会话列表（按最近消息排序） |
| POST | `/api/v1/chat/conversations` | 创建/获取会话（幂等） |
| GET | `/api/v1/chat/conversations/{id}/messages` | 消息列表（自动标记已读） |
| POST | `/api/v1/chat/conversations/{id}/messages` | 发送消息 |
| POST | `/api/v1/chat/upload-image` | 上传聊天图片 |
| GET | `/api/v1/chat/unread-count` | 全局未读消息数 |

#### 通知模块 (NotificationController)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/notifications` | 通知列表（最新优先） |
| GET | `/api/v1/notifications/unread-count` | 未读通知数 |
| PUT | `/api/v1/notifications/{id}/read` | 标记单条已读 |
| PUT | `/api/v1/notifications/read-all` | 全部已读 |

#### 管理模块 (AdminController)

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/admin/dashboard` | ADMIN | 仪表盘统计概览 |
| GET | `/api/v1/admin/users` | ADMIN | 用户列表（分页+搜索+徽章） |
| PUT | `/api/v1/admin/users/{id}/status` | ADMIN | 封禁/解封用户 |
| GET | `/api/v1/admin/demands` | ADMIN | 需求列表（类型/状态/关键词筛选） |
| DELETE | `/api/v1/admin/demands/{id}` | ADMIN | 删除需求（硬删除） |
| GET | `/api/v1/admin/reports` | ADMIN | 举报列表（按状态筛选） |
| PUT | `/api/v1/admin/reports/{id}/resolve` | ADMIN | 处理举报（已处理/驳回） |

#### 举报模块 (ReportController)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/reports` | 提交举报（需求/用户/消息） |

#### 成就徽章模块 (BadgeController)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/badges` | 9 种徽章及用户进度 |
| POST | `/api/v1/badges/wear/{key}` | 佩戴徽章 |
| DELETE | `/api/v1/badges/wear` | 取下徽章 |
| POST | `/api/v1/badges/easter-egg` | 触发彩蛋徽章 |

#### 收藏模块 (DemandController 内)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/demands/my/favorites` | 我的收藏列表 |
| POST | `/api/v1/demands/{id}/favorite` | 收藏需求 |
| DELETE | `/api/v1/demands/{id}/favorite` | 取消收藏 |

---

## 前端架构

### 页面路由（18 条）

| 路径 | 组件 | 权限 | 说明 |
|------|------|------|------|
| `/login` | Login.vue | 游客 | 分栏式登录页，装饰动画 |
| `/register` | Register.vue | 游客 | 分栏式注册页 |
| `/` | Home.vue | 登录 | 首页：问候/积分统计/签到/功能入口 |
| `/profile` | Profile.vue | 登录 | 个人资料 + 隐私设置 + 密码修改 |
| `/demands` | DemandList.vue | 登录 | 需求广场：搜索/筛选/排序/分页/收藏 |
| `/demands/publish` | DemandPublish.vue | 登录 | 发布需求：动态表单（6 种类型） |
| `/demands/:id` | DemandDetail.vue | 登录 | 需求详情：状态流/组队审批/评价/管理删除 |
| `/orders` | MyOrders.vue | 登录 | 我的订单：三标签页（发布/接取/队伍） |
| `/points/history` | PointsHistory.vue | 登录 | 积分明细：余额总览 + 分类流水 |
| `/notifications` | Notifications.vue | 登录 | 通知中心：系统消息 + 私信会话 |
| `/chat/:id` | ChatDetail.vue | 登录 | 私信聊天：文本/图片/Emoji |
| `/badges` | BadgeList.vue | 登录 | 成就徽章：全部 9 种徽章展示 + 佩戴/取下 |
| `/admin` | AdminDashboard.vue | ADMIN | 管理仪表盘：统计概览 + 快速入口 |
| `/admin/users` | UserList.vue | ADMIN | 用户管理：搜索/封禁/解封/徽章展示 |
| `/admin/demands` | DemandList.vue | ADMIN | 需求管理：表格+列表双模式/筛选/删除 |
| `/admin/reports` | ReportList.vue | ADMIN | 举报管理：状态栏筛选/处理操作面板 |
| `/demands/my/favorites` | FavoriteList.vue | 登录 | 我的收藏：分页浏览已收藏需求 |
| `/settings` | Settings.vue | 登录 | 设置：关于/彩蛋/退出登录 |

### 导航守卫

```javascript
// 全局前置守卫逻辑
if (需要认证 && 未登录)          → redirect('/login')
if (游客路由 && 已登录)           → redirect('/')
if (管理员路由 && 角色≠ADMIN)     → redirect('/')
```

### 状态管理 (Pinia)

**AuthStore** (`stores/auth.js`)：
- 状态：`token`, `userId`, `name`, `role`, `avatar`
- 计算属性：`isLoggedIn`, `isAdmin`
- 持久化：登录时写入 localStorage，页面刷新时自动恢复
- 退出：清除 localStorage + Pinia state，路由重定向

**BadgeToastStore** (`stores/badgeToast.js`)：
- 持久化：`earnedBadgeKeys` 存入 localStorage，防止重复弹窗
- `checkNewBadges()`：异步检测新获得徽章并加入弹窗队列
- 全屏动画：`<teleport to="body">` 实现跨路由覆盖层动效

### API 层设计

**Axios 实例** (`api/client.js`)：
- `baseURL`：从环境变量 `VITE_API_BASE` 读取，默认代理到后端 8080
- **请求拦截器**：自动从 localStorage 读取 Token 并注入 Authorization 头
- **响应拦截器**：自动解包 `response.data.body.data`；非 200 状态码提取错误信息并 Toast 提示；401 自动清除登录态跳转登录页；网络错误统一 Toast "网络错误"

**API 模块**（10 个）：`user.js`, `demand.js`, `points.js`, `chat.js`, `notification.js`, `evaluation.js`, `admin.js`, `badge.js`, `report.js`, `favorite.js` — 每个模块导出纯函数，返回 Promise。

### 共享常量

`constants/demandTypes.js` 作为**单一数据源**，定义了：
- 六种类型的 label、icon、color、bg
- 每种类型的表单配置（是否显示报酬/地点、报酬类型选项、默认报酬类型）
- `rewardText()` 工具函数（统一需求卡片、详情、订单列表等处的报酬展示逻辑）

### 性能优化

| 优化项 | 策略 |
|--------|------|
| **路由懒加载** | 所有 12 个页面组件均为动态 import，按需加载 |
| **聊天轮询节流** | 仅当消息数量变化时才更新 DOM（`watch(() => messages.value.length)`） |
| **响应式图片** | 图片上传前前端压缩（Vant Uploader max-size），缩略图与全屏预览分离 |
| **列表虚拟化准备** | Vant List 组件的 `offset` 属性预配置，为大量数据场景做准备 |
| **CSS 变量** | 全局设计 token 使用 CSS 自定义属性，避免运行时样式计算 |

---

## 工程质量

### 后端测试

- **测试框架**：JUnit 5 + Spring Boot Test
- **测试数据库**：H2 内存数据库（`application-test.yml`），无需 MySQL
- **测试范围**：Service 层业务逻辑、Mapper 层数据访问
- **运行**：`cd backend && mvn test`

### 代码规范

- **后端**：Java 常量类替代字符串枚举（`DemandStatus`, `PointsTransactionType`, `NotificationType` 等），消除魔法字符串
- **前端**：共享常量文件 `constants/demandTypes.js` 作为需求类型的单一数据源，`TYPE_CONFIG` + `TYPE_LABELS` + `TYPE_STYLES` 派生自同一份定义
- **DTO 分离**：Request/Response DTO 与 Entity 分离，避免 API 契约泄露内部数据结构
- **全局异常处理**：`@RestControllerAdvice` + `BusinessException` 体系，统一错误码和错误消息

### 开发工具链

| 工具 | 用途 |
|------|------|
| **Flyway** | 数据库版本迁移，14 版 SQL 脚本（V1–V14），支持从零建库到最新 Schema |
| **`prepare_for_demo.sh`** | 演示数据填充脚本（471 行），通过 REST API 创建 11 个用户 + ~28 条需求 + 签到/组队/评价/隐私数据 |
| **SSL 证书生成命令** | README 内提供完整的 keytool + openssl 命令，一键生成前后端开发证书 |
| **环境变量覆盖** | `JWT_SECRET` 和 `DB_PASSWORD` 支持环境变量注入，避免敏感信息硬编码 |

---

## 项目结构

```
/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/cn/seecoder/campushelp/
│   │   ├── common/                   # ApiResult、ResultCode、BusinessException、全局异常处理
│   │   ├── config/                   # SecurityConfig、CorsConfig、MyBatisPlusConfig、DataInitializer
│   │   ├── security/                 # JwtTokenProvider、JwtAuthenticationFilter、JwtProperties
│   │   ├── entity/                   # 数据实体：User、Demand、UserAccount、Favorite、Report 等 15 个
│   │   │   └── enums/                # 常量类：DemandStatus、PointsTransactionType、BadgeDefinition 等 7 个
│   │   ├── dto/                      # 请求/响应 DTO（request/response 子包）
│   │   ├── mapper/                   # MyBatis-Plus Mapper 接口（17 个）
│   │   ├── service/                  # 业务接口 + impl 实现（14 个 Service）
│   │   └── controller/               # REST 控制器（10 个 Controller）
│   └── src/main/resources/
│       ├── application.yml           # 全局配置
│       ├── application-dev.yml       # 开发环境（MySQL 连接、Flyway、JWT）
│       ├── application-test.yml      # 测试环境（H2 内存库）
│       └── db/migration/             # Flyway 迁移脚本 V1-V14
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── router/index.js           # 路由配置（12 页 + 导航守卫）
│       ├── stores/auth.js            # Pinia 认证状态
│       ├── api/                      # 7 个 API 模块 + Axios 实例
│       │   ├── client.js             # Axios 拦截器
│       │   ├── user.js / demand.js / points.js
│       │   ├── chat.js / notification.js
│       │   └── evaluation.js / admin.js
│       ├── views/                    # 18 个页面组件
│       │   ├── Login.vue / Register.vue
│       │   ├── Home.vue / Profile.vue / Settings.vue
│       │   ├── DemandList.vue / DemandPublish.vue / DemandDetail.vue
│       │   ├── MyOrders.vue / PointsHistory.vue / FavoriteList.vue
│       │   ├── Notifications.vue / ChatDetail.vue / BadgeList.vue
│       │   └── admin/
│       │       ├── AdminDashboard.vue / UserList.vue
│       │       ├── DemandList.vue / ReportList.vue
│       ├── components/               # 可复用组件
│       │   ├── NavActions.vue        # 导航栏右侧操作区
│       │   ├── ImageViewer.vue       # 全屏图片预览
│       │   ├── EmojiPicker.vue       # Emoji 选择面板
│       │   ├── BadgeOverlay.vue      # 徽章角标（头像叠加）
│       │   └── BadgeToast.vue        # 徽章获得全屏动效
│       ├── constants/
│       │   └── demandTypes.js        # 需求类型共享常量（单一数据源）
│       └── styles/
│           └── main.css              # M3E 设计系统（200+ 行 CSS 自定义属性）
├── docs/                             # 阶段文档 + HCI 交互设计文档
└── prepare_for_demo.sh               # 演示数据填充脚本（471 行，通过 API 调用）
```

---

## 快速开始

### 环境要求

| 依赖 | 版本 | 安装（Arch） | 安装（Debian/Ubuntu） |
|------|------|-------------|----------------------|
| JDK | 17+ | `sudo pacman -S jdk17-openjdk` | `sudo apt install openjdk-17-jdk` |
| Maven | 3.8+ | `sudo pacman -S maven` | `sudo apt install maven` |
| MariaDB | 10.x+ | `sudo pacman -S mariadb` | `sudo apt install mariadb-server` |
| Node.js | 18+ | `sudo pacman -S nodejs npm` | `sudo apt install nodejs npm` |

### 1. 初始化数据库

```bash
sudo systemctl start mariadb
sudo mariadb -u root -e "CREATE DATABASE IF NOT EXISTS campus_help DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Debian/Ubuntu 用户需先切换 MariaDB 认证方式：
```bash
sudo mariadb -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root'; FLUSH PRIVILEGES;"
```

### 2. 生成 SSL 证书（仅首次）

```bash
# 后端 PKCS12 证书
cd backend/src/main/resources
keytool -genkeypair -alias campus-help -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit -keypass changeit -dname "CN=campus-help" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"

# 前端 PEM 证书
cd ../../frontend
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout key.pem -out cert.pem -days 3650 \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 运行于 https://localhost:8080
# 首次启动 Flyway 自动建表 + 创建管理员 admin/admin123
```

### 4. （可选）填充演示数据

```bash
# 另开终端，确保后端已启动
bash prepare_for_demo.sh
# 创建 11 个用户（密码 123456）+ ~28 条需求 + 签到/组队/评价数据
```

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
# 浏览器打开 http://localhost:5173
# Vite 自动代理 /api 到后端 localhost:8080
```

### 6. 运行测试

```bash
cd backend
mvn test
# 使用 H2 内存数据库，无需 MySQL
```

---

## 配置说明

### 后端数据库连接

编辑 `backend/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_help?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: root        # ← 修改为你的数据库密码
```

### 环境变量覆盖

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `JWT_SECRET` | 内置 Base64 密钥 | JWT HMAC-SHA256 签名密钥 |
| `DB_PASSWORD` | `root` | MariaDB 密码 |

### 前端 API 地址

默认通过 Vite proxy 转发。如需自定义：

```bash
# frontend/.env.development
VITE_API_BASE=http://localhost:5173/api/v1
```

---

## 数据库维护

### 新增迁移

在 `backend/src/main/resources/db/migration/` 下创建 `V{next}__description.sql`，下次启动 Flyway 自动执行。

**重要**：已应用的迁移文件永远不要修改（Flyway checksum 校验会导致启动失败）。

### 重置数据库

```bash
sudo mariadb -u root -e "DROP DATABASE campus_help; CREATE DATABASE campus_help DEFAULT CHARACTER SET utf8mb4;"
# 重启后端，Flyway 自动重建所有表
```

### 数据备份与迁移

```bash
# 导出
mariadb-dump -u root campus_help > dump.sql

# 导入
mariadb -u root -e "CREATE DATABASE campus_help DEFAULT CHARACTER SET utf8mb4;"
mariadb -u root campus_help < dump.sql
```

---

## 演示数据

`prepare_for_demo.sh` 脚本通过 REST API 填充逼真的校园互助场景数据：

| 数据项 | 数量 | 说明 |
|--------|------|------|
| 用户 | 11 人 | 学号 2024001001-2024001011，密码均为 123456 |
| 需求 | ~28 条 | 覆盖全部 6 种类型 × 4 种状态（含进行中和已完成） |
| 签到记录 | 7 人已签到 | 不同连续天数（1/2/4/5/6/7/7 天），4 人未签到 |
| 完成需求 | 5 条 | 经 accept→complete 完整流程，产生积分转移 |
| 组队 | 5 个队伍 | 含 7 条申请和 5 条审批（有 pending 状态） |
| 互评 | 10 条 | 5 对完成需求的双向评价 |
| 举报数据 | 3 条 | 含 pending/resolved/dismissed 各状态 |
| 徽章数据 | 部分用户 | 自动检测颁发的各项成就徽章 |
| 匿名用户 | 2 人 | "热心市民小王""匿名雷锋"，测试匿名发布效果 |

脚本特性：
- **幂等** — 通过 API 调用（非直接 SQL），处理 409 CONFLICT 优雅跳过已存在数据
- **真实** — 积分流转、连续签到计算、评价信用分更新均由后端 Service 层处理
- **兼容脏库** — 可安全重复执行，不会因数据已存在而崩溃

---

## 团队

| 角色 | 成员 | 职责 |
|------|------|------|
| 需求负责人 | 侯乔岳 | 功能规划、需求文档 |
| 架构负责人 | 杨佳兴 | 系统设计、数据库 Schema、API 设计 |
| 开发负责人 | 胡皓轩 | 前后端开发、代码实现 |
| 测试负责人 | 沈诺 | 测试用例、质量保证 |
