# 校园互助服务平台-ER图


## ER图 

```mermaid
erDiagram
    %% 核心实体关系定义
    USER ||--o{ BASE_ORDER : "发布/承接 (1:N)"
    USER ||--|| USER_ACCOUNT : "拥有 (1:1)"
    USER ||--|| PRIVACY_PROFILE : "配置 (1:1)"
    BASE_ORDER ||--o{ EVALUATION : "产生评价 (1:N)"
    BASE_ORDER ||--|| CHAT_SESSION : "关联会话 (1:1)"
    CHAT_SESSION ||--o{ CHAT_MESSAGE : "包含消息 (1:N)"
    BASE_ORDER }|--|| PAID_ORDER : "继承 (1:1)"
    BASE_ORDER }|--|| LOST_FOUND_ORDER : "继承 (1:1)"
    PAID_ORDER }|--|| ERRAND_ORDER : "继承 (1:1)"
    PAID_ORDER }|--|| TRADE_ORDER : "继承 (1:1)"
    PAID_ORDER }|--|| TEAM_ORDER : "继承 (1:1)"

    %% 实体及属性定义
    USER {
        bigint user_id PK "用户ID，主键"
        varchar student_id "学号，校内唯一认证"
        varchar password "加密后的密码"
        varchar name "姓名"
        varchar avatar "头像URL"
        int status "状态：正常/封禁"
    }

    USER_ACCOUNT {
        bigint account_id PK "账户ID，主键"
        bigint user_id FK "关联用户ID"
        int available_points "可用积分"
        int frozen_points "冻结积分"
        double reputation_score "信誉评分"
    }

    PRIVACY_PROFILE {
        bigint privacy_id PK "隐私配置ID，主键"
        bigint user_id FK "关联用户ID"
        boolean is_anonymous "是否开启匿名"
        varchar mask_name "虚拟昵称"
    }

    BASE_ORDER {
        bigint order_id PK "订单ID，主键"
        bigint publisher_id FK "发布者ID"
        bigint acceptor_id FK "接单者ID，可为空"
        varchar title "订单标题"
        varchar description "详细描述"
        varchar status "订单状态"
        datetime create_time "创建时间"
    }

    PAID_ORDER {
        bigint order_id PK,FK "关联BASE_ORDER主键"
        int points_reward "积分报酬"
    }

    ERRAND_ORDER {
        bigint order_id PK,FK "关联PAID_ORDER主键"
        varchar errand_type "跑腿类型"
        varchar pickup_location "取件地点"
        varchar delivery_location "送达地点"
        datetime deadline "截止时间"
    }

    TRADE_ORDER {
        bigint order_id PK,FK "关联PAID_ORDER主键"
        varchar category "商品分类"
        varchar images "商品图片URL列表，JSON格式"
        double cash_price "现金价格（可选）"
    }

    TEAM_ORDER {
        bigint order_id PK,FK "关联PAID_ORDER主键"
        varchar team_type "组队类型"
        int max_members "人数上限"
        varchar current_members "当前成员ID列表，JSON格式"
    }

    LOST_FOUND_ORDER {
        bigint order_id PK,FK "关联BASE_ORDER主键"
        int type "类型：寻物/招领"
        varchar item_location "丢失/拾获地点"
        varchar images "物品图片URL列表，JSON格式"
    }

    CHAT_SESSION {
        bigint session_id PK "会话ID，主键"
        bigint order_id FK "关联订单ID"
        bigint user_a_id "发布者ID"
        bigint user_b_id "接单者ID"
    }

    CHAT_MESSAGE {
        bigint message_id PK "消息ID，主键"
        bigint session_id FK "所属会话ID"
        bigint sender_id "发送者ID"
        varchar content "文字内容"
        varchar file_url "文件URL"
        datetime send_time "发送时间"
    }

    EVALUATION {
        bigint evaluation_id PK "评价ID，主键"
        bigint order_id FK "关联订单ID"
        bigint evaluator_id "评价人ID"
        bigint target_user_id "被评价人ID"
        int rating "评分"
        varchar comment "评语"
        datetime create_time "评价时间"
    }