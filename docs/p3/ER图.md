# 校园互助服务平台 — ER 图

> **版本**：2.0 | **最后更新**：2026-06-14
> 基于实际数据库 schema（Flyway V1–V11）

---

## 1. 实体关系图

```mermaid
erDiagram
    %% ============ 关系定义 ============
    user ||--o{ demand : "发布 (publisher_id)"
    user ||--o{ demand : "接单 (acceptor_id)"
    user ||--|| privacy_profile : "拥有"
    user ||--|| user_account : "拥有"
    user ||--o{ notification : "接收"
    user ||--o{ points_transaction : "产生"
    user ||--o{ daily_checkin : "签到"
    user ||--o{ evaluation : "评价他人 (evaluator_id)"
    user ||--o{ evaluation : "被评价 (target_user_id)"
    user ||--o{ team_member : "参与组队"
    user ||--o{ conversation : "参与会话 (user1_id/user2_id)"
    user ||--o{ message : "发送消息"
    demand ||--o{ team_member : "包含成员"
    demand ||--o{ conversation : "关联会话"
    demand ||--o{ evaluation : "被评价"
    demand ||--o{ notification : "触发通知"
    conversation ||--o{ message : "包含消息"

    %% ============ 实体定义 ============
    user {
        bigint user_id PK "用户ID，自增主键"
        varchar student_id UK "学号，唯一索引"
        varchar password "BCrypt 加密密码"
        varchar name "姓名"
        varchar avatar "头像 URL"
        varchar role "角色：USER / ADMIN"
        int status "状态：1正常 0封禁"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }

    privacy_profile {
        bigint privacy_id PK "隐私配置ID"
        bigint user_id UK "用户ID，级联删除"
        tinyint is_anonymous "匿名模式：0关闭 1开启"
        varchar mask_name "虚拟昵称"
    }

    user_account {
        bigint account_id PK "账户ID"
        bigint user_id UK "用户ID，级联删除"
        int available_points "可用积分"
        int frozen_points "冻结积分"
        double reputation_score "信誉评分，默认5.0"
    }

    demand {
        bigint demand_id PK "需求ID，自增主键"
        bigint publisher_id FK "发布者ID"
        bigint acceptor_id FK "接单者ID，可为空"
        varchar type "类型：errand/trade/team/lost_found/study/other"
        varchar title "标题"
        text description "详细描述"
        text images "图片URL列表，逗号分隔"
        text attributes "类型特有字段，JSON格式"
        varchar location "地点信息"
        datetime deadline "截止时间"
        varchar reward_type "报酬类型：point/donation"
        int reward_amount "报酬数额"
        tinyint is_anonymous "是否匿名发布"
        varchar status "状态：OPEN/IN_PROGRESS/COMPLETED/CANCELLED"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }

    team_member {
        bigint id PK "记录ID"
        bigint demand_id FK "需求ID，级联删除"
        bigint user_id FK "用户ID，级联删除"
        varchar role "角色：LEADER/MEMBER"
        varchar status "状态：PENDING/JOINED/REJECTED"
        varchar message "申请留言"
        datetime create_time "创建时间"
    }

    conversation {
        bigint conversation_id PK "会话ID"
        bigint demand_id FK "需求ID，级联删除"
        bigint user1_id FK "较小用户ID（确定性排序）"
        bigint user2_id FK "较大用户ID"
        varchar last_message "最后一条消息快照"
        timestamp last_message_at "最后消息时间"
        timestamp create_time "创建时间"
    }

    message {
        bigint message_id PK "消息ID"
        bigint conversation_id FK "会话ID，级联删除"
        bigint sender_id FK "发送者ID"
        text content "消息内容"
        varchar message_type "消息类型：text/image"
        varchar image_url "图片URL"
        tinyint is_read "已读：0未读 1已读"
        timestamp create_time "发送时间"
    }

    notification {
        bigint notification_id PK "通知ID"
        bigint user_id FK "接收者ID，级联删除"
        varchar type "类型：ACCEPT/COMPLETE/CANCEL/EVALUATION等"
        varchar title "通知标题"
        text content "通知正文"
        tinyint is_read "已读：0未读 1已读"
        bigint related_demand_id FK "关联需求ID"
        datetime create_time "创建时间"
    }

    evaluation {
        bigint evaluation_id PK "评价ID"
        bigint demand_id FK "需求ID，级联删除"
        bigint evaluator_id FK "评价人ID"
        bigint target_user_id FK "被评价人ID"
        int rating "评分：1-5"
        varchar comment "评语，最长500字"
        timestamp create_time "创建时间"
        timestamp update_time "更新时间"
    }

    points_transaction {
        bigint transaction_id PK "流水ID"
        bigint user_id FK "用户ID，级联删除"
        int amount "变动额：正=收入 负=支出"
        int balance_after "变动后可用余额"
        varchar type "类型：SIGNUP_BONUS/DAILY_CHECKIN/PUBLISH等"
        bigint reference_id "关联需求ID"
        varchar description "可读说明"
        datetime create_time "创建时间"
    }

    daily_checkin {
        bigint checkin_id PK "签到ID"
        bigint user_id FK "用户ID，级联删除"
        date checkin_date UK "签到日期"
        int points_awarded "本次签到获得积分"
        int streak "当日连续签到天数"
        datetime create_time "创建时间"
    }
```

---

## 2. 索引设计说明

| 表 | 索引名 | 索引列 | 类型 | 用途 |
|----|--------|--------|------|------|
| `user` | `uk_student_id` | `student_id` | UNIQUE | 学号登录高频查询，保证学号唯一 |
| `privacy_profile` | `uk_privacy_user_id` | `user_id` | UNIQUE | 用户与隐私配置一对一关联 |
| `user_account` | `uk_account_user_id` | `user_id` | UNIQUE | 用户与账户一对一关联 |
| `demand` | `idx_publisher` | `publisher_id` | INDEX | "我的发布"查询；与批量加载配合防 N+1 |
| `demand` | `idx_acceptor` | `acceptor_id` | INDEX | "我的接单"查询 |
| `demand` | `idx_type_status` | `type, status` | 复合 INDEX | 需求广场按类型+状态筛选（最常用查询） |
| `demand` | `idx_create_time` | `create_time` | INDEX | 按时间排序（首页最新需求） |
| `notification` | `idx_user_read` | `user_id, is_read` | 复合 INDEX | "我的未读通知"查询 |
| `notification` | `idx_create_time` | `create_time` | INDEX | 通知列表按时间排序 |
| `conversation` | `uk_demand_users` | `demand_id, user1_id, user2_id` | UNIQUE | 保证每对用户在同一需求下仅有一个会话 |
| `message` | `idx_conv_time` | `conversation_id, create_time` | 复合 INDEX | 会话消息按时间分页加载 |
| `evaluation` | `uk_demand_evaluator` | `demand_id, evaluator_id` | UNIQUE | 每人在每个需求下只能评价一次 |
| `team_member` | `uk_demand_user` | `demand_id, user_id` | UNIQUE | 同一用户在同一需求下只有一条组队记录 |
| `points_transaction` | `idx_tx_user` | `user_id` | INDEX | "我的积分明细"分页查询 |
| `points_transaction` | `idx_tx_time` | `create_time` | INDEX | 按时间排序积分流水 |
| `daily_checkin` | `uk_user_date` | `user_id, checkin_date` | UNIQUE | 每日签到唯一性约束 + 签到状态查询 |

---

## 3. 关键设计决策

### 3.1 单表继承（Single-Table Inheritance）

所有需求类型（跑腿、交易、组队、失物招领、学习互助、其他）共用一张 `demand` 表，通过 `type` 列（VARCHAR(32)）区分类型。类型特有字段存储在 `attributes`（TEXT，JSON 格式）列中。

**选择理由：**
- **查询简单**：需求列表无需跨表 JOIN，一条 `SELECT ... WHERE type=?` 即可
- **易于扩展**：新增需求类型只需在应用层添加验证逻辑，无需 ALTER TABLE
- **MyBatis-Plus 友好**：单一实体类映射，无需处理复杂的继承映射（MyBatis-Plus 不支持 JPA 继承注解）
- **避免 JOIN 开销**：相比类表继承（每种子类一张表），单表方案不存在多表 UNION 或 LEFT JOIN 的性能损耗

**权衡：**
- `attributes` 列中的 JSON 数据无法在数据库层做类型安全检查（由应用层 `DemandService.validateAttributes()` 保证）
- 所有类型共享同一表空间，极端情况下单表数据量大（校园场景可控）

### 3.2 JSON 属性列

`demand.attributes` 存储不同类型需求的特有字段，格式为 JSON 字符串：

| 需求类型 | attributes 示例 |
|----------|----------------|
| `errand` | `{"pickup_location":"韵达快递点"}` |
| `lost_found` | `{"lf_type":"LOST"}` 或 `{"lf_type":"FOUND"}` |
| `team` | `{"team_size":4,"team_type":"competition"}` |
| `trade` | `{"category":"教材","cash_price":0}` |
| `study` / `other` | `{}`（当前无特有字段） |

验证逻辑位于 `DemandServiceImpl.validateAttributes()`，按 `type` 分支检查必填字段和值域。

### 3.3 确定性会话 ID 排序

`conversation` 表始终保证 `user1_id < user2_id`（较小 ID 在前）。这一约束使得 `(demand_id, user1_id, user2_id)` 唯一索引可以正确防止同一对用户在同一个需求下创建重复会话，同时避免了 `(demand_id, LEAST(a,b), GREATEST(a,b))` 这类数据库函数索引的开销。

### 3.4 不可变积分账本

`points_transaction` 表作为只追加（append-only）的积分账本，每条记录包含变动后的余额快照（`balance_after`）。所有写操作使用 `SELECT ... FOR UPDATE` 悲观行锁，防止并发修改导致的余额不一致。积分流水不可删除、不可修改，保证审计可追溯。

---

## 4. 表清单速览

| # | 表名 | 用途 | 迁移版本 |
|---|------|------|----------|
| 1 | `user` | 用户认证与基本信息 | V1 |
| 2 | `privacy_profile` | 匿名模式与虚拟昵称 | V1 |
| 3 | `user_account` | 积分余额与信誉评分 | V1 |
| 4 | `demand` | 需求（全部6种类型，单表） | V2/V3/V7/V9 |
| 5 | `notification` | 系统通知 | V4 |
| 6 | `evaluation` | 交易评价（1-5星） | V5 |
| 7 | `conversation` | 聊天会话 | V6 |
| 8 | `message` | 聊天消息 | V6/V8 |
| 9 | `team_member` | 组队成员（含申请审批） | V10 |
| 10 | `points_transaction` | 积分流水账本（只追加） | V11 |
| 11 | `daily_checkin` | 每日签到记录 | V11 |
