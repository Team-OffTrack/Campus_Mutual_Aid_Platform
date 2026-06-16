-- ============================================================
-- 校园互助平台 完整建表 Schema (Flyway V1–V14 合并)
-- 自动生成于 2026-06-16
-- ============================================================

-- ============================================================
-- V10__create_team_member_table.sql
-- ============================================================
-- V10: Create team_member table for multi-person team matching
CREATE TABLE team_member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    role        VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    message     VARCHAR(256),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_demand_user (demand_id, user_id),
    CONSTRAINT fk_tm_demand FOREIGN KEY (demand_id) REFERENCES demand(demand_id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- V11__create_points_tables.sql
-- ============================================================
-- V11: Points system tables
-- Creates points_transaction ledger and daily_checkin table

CREATE TABLE points_transaction (
    transaction_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'transaction ID',
    user_id         BIGINT       NOT NULL COMMENT 'user ID',
    amount          INT          NOT NULL COMMENT 'positive = earn, negative = spend',
    balance_after   INT          NOT NULL COMMENT 'available_points balance after this transaction',
    type            VARCHAR(32)  NOT NULL COMMENT 'SIGNUP_BONUS / DAILY_CHECKIN / PUBLISH / CANCEL_REFUND / COMPLETE_EARN / ADMIN_ADJUST',
    reference_id    BIGINT       DEFAULT NULL COMMENT 'related demand_id',
    description     VARCHAR(255) DEFAULT NULL COMMENT 'human-readable reason',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    PRIMARY KEY (transaction_id) USING BTREE,
    INDEX idx_tx_user (user_id) USING BTREE,
    INDEX idx_tx_time (create_time) USING BTREE,
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'points transaction ledger';

CREATE TABLE daily_checkin (
    checkin_id     BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'checkin ID',
    user_id        BIGINT   NOT NULL COMMENT 'user ID',
    checkin_date   DATE     NOT NULL COMMENT 'check-in date (no time component)',
    points_awarded INT      NOT NULL DEFAULT 0 COMMENT 'points awarded for this check-in',
    streak         INT      NOT NULL DEFAULT 0 COMMENT 'consecutive days streak at this check-in',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    PRIMARY KEY (checkin_id) USING BTREE,
    UNIQUE KEY uk_user_date (user_id, checkin_date) USING BTREE,
    CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'daily check-in records';


-- ============================================================
-- V12__create_favorite_table.sql
-- ============================================================
-- V12: Create user_favorite table for demand bookmarks
CREATE TABLE user_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fav_demand_user (demand_id, user_id),
    CONSTRAINT fk_fav_demand FOREIGN KEY (demand_id) REFERENCES demand(demand_id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- V13__create_report_table.sql
-- ============================================================
-- V13: Create report table for content reporting
CREATE TABLE report (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL COMMENT 'DEMAND/USER/MESSAGE',
    target_id   BIGINT NOT NULL,
    reason      VARCHAR(32) NOT NULL COMMENT 'MISLEADING/HARASSMENT/ILLEGAL/SPAM/OTHER',
    description VARCHAR(512) COMMENT 'Optional user-supplied detail',
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RESOLVED/DISMISSED',
    admin_note  VARCHAR(512) COMMENT 'Admin resolution note',
    admin_id    BIGINT COMMENT 'Admin who resolved this report',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolve_time DATETIME,
    INDEX idx_report_target (target_type, target_id),
    INDEX idx_report_reporter (reporter_id),
    INDEX idx_report_status (status),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_report_admin FOREIGN KEY (admin_id) REFERENCES user(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- V14__create_badge_tables.sql
-- ============================================================
-- Achievement badge system
-- user_badge: records which badges a user has earned
-- worn_badge: tracks which badge the user is currently wearing on their avatar
CREATE TABLE user_badge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    badge_key   VARCHAR(32) NOT NULL COMMENT 'Badge identifier, e.g. FIRST_PUBLISH',
    earned_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_badge (user_id, badge_key),
    INDEX idx_ub_user (user_id),
    CONSTRAINT fk_ub_user FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Badges earned by users';

CREATE TABLE worn_badge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE,
    badge_key   VARCHAR(32) NOT NULL COMMENT 'Currently worn badge key',
    worn_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wb_user (user_id),
    CONSTRAINT fk_wb_user FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Currently worn badge (one per user)';


-- ============================================================
-- V1__init_user_tables.sql
-- ============================================================
-- V1: User management module tables
-- Creates user, privacy_profile, and user_account tables

CREATE TABLE `user` (
    `user_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'user ID',
    `student_id`    VARCHAR(32)  NOT NULL COMMENT 'student number',
    `password`      VARCHAR(128) NOT NULL COMMENT 'encrypted password',
    `name`          VARCHAR(64)  NOT NULL COMMENT 'display name',
    `avatar`        VARCHAR(255) NULL     DEFAULT NULL COMMENT 'avatar URL',
    `role`          VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT 'role: USER / ADMIN',
    `status`        INT          NOT NULL DEFAULT 1 COMMENT 'status: 1 active, 0 banned',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    PRIMARY KEY (`user_id`) USING BTREE,
    UNIQUE INDEX `uk_student_id` (`student_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'user table';

CREATE TABLE `privacy_profile` (
    `privacy_id`    BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'privacy config ID',
    `user_id`       BIGINT      NOT NULL COMMENT 'user ID',
    `is_anonymous`  TINYINT     NOT NULL DEFAULT 0 COMMENT 'anonymous mode',
    `mask_name`     VARCHAR(64) NULL     DEFAULT '' COMMENT 'virtual nickname',
    PRIMARY KEY (`privacy_id`) USING BTREE,
    UNIQUE INDEX `uk_privacy_user_id` (`user_id`) USING BTREE,
    CONSTRAINT `fk_privacy_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'privacy profile table';

CREATE TABLE `user_account` (
    `account_id`        BIGINT  NOT NULL AUTO_INCREMENT COMMENT 'account ID',
    `user_id`           BIGINT  NOT NULL COMMENT 'user ID',
    `available_points`  INT     NOT NULL DEFAULT 0 COMMENT 'available points',
    `frozen_points`     INT     NOT NULL DEFAULT 0 COMMENT 'frozen points',
    `reputation_score`  DOUBLE  NOT NULL DEFAULT 5.0 COMMENT 'reputation score',
    PRIMARY KEY (`account_id`) USING BTREE,
    UNIQUE INDEX `uk_account_user_id` (`user_id`) USING BTREE,
    CONSTRAINT `fk_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'user account table';


-- ============================================================
-- V2__create_demand_table.sql
-- ============================================================
-- V2: Demand publishing module
-- Single-table design for all demand types (errand, trade, team, lost_found, study, other)

CREATE TABLE `demand` (
    `demand_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'demand ID',
    `publisher_id`  BIGINT       NOT NULL COMMENT 'publisher user ID',
    `type`          VARCHAR(32)  NOT NULL COMMENT 'demand type: errand / trade / team / lost_found / study / other',
    `title`         VARCHAR(128) NOT NULL COMMENT 'title',
    `description`   TEXT         NULL     COMMENT 'detailed description',
    `location`      VARCHAR(255) NULL     COMMENT 'location info',
    `deadline`      DATETIME     NULL     COMMENT 'deadline',
    `reward_type`   VARCHAR(32)  NULL     DEFAULT 'point' COMMENT 'reward type: point / cash / donation',
    `reward_amount` INT          NULL     DEFAULT 0 COMMENT 'reward amount',
    `is_anonymous`  TINYINT      NOT NULL DEFAULT 0 COMMENT 'publish anonymously',
    `status`        VARCHAR(32)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / IN_PROGRESS / COMPLETED / CANCELLED',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    PRIMARY KEY (`demand_id`) USING BTREE,
    INDEX `idx_publisher` (`publisher_id`) USING BTREE,
    INDEX `idx_type_status` (`type`, `status`) USING BTREE,
    INDEX `idx_create_time` (`create_time`) USING BTREE,
    CONSTRAINT `fk_demand_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'demand table';


-- ============================================================
-- V3__add_acceptor_to_demand.sql
-- ============================================================
-- V3: Add acceptor_id to demand table for order management

ALTER TABLE `demand`
    ADD COLUMN `acceptor_id` BIGINT NULL DEFAULT NULL COMMENT 'acceptor user ID' AFTER `publisher_id`,
    ADD INDEX `idx_acceptor` (`acceptor_id`) USING BTREE,
    ADD CONSTRAINT `fk_demand_acceptor` FOREIGN KEY (`acceptor_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL;


-- ============================================================
-- V4__create_notification_table.sql
-- ============================================================
-- V4: Notification system for order events (accept, complete, cancel)

CREATE TABLE `notification` (
    `notification_id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'notification ID',
    `user_id`           BIGINT      NOT NULL COMMENT 'recipient user ID',
    `type`              VARCHAR(32) NOT NULL COMMENT 'ACCEPT / COMPLETE / CANCEL',
    `title`             VARCHAR(128) NOT NULL COMMENT 'notification title',
    `content`           TEXT        NULL     COMMENT 'notification body',
    `is_read`           TINYINT     NOT NULL DEFAULT 0 COMMENT '0 unread, 1 read',
    `related_demand_id` BIGINT      NULL     COMMENT 'related demand ID for navigation',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    PRIMARY KEY (`notification_id`) USING BTREE,
    INDEX `idx_user_read` (`user_id`, `is_read`) USING BTREE,
    INDEX `idx_create_time` (`create_time`) USING BTREE,
    CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notif_demand` FOREIGN KEY (`related_demand_id`) REFERENCES `demand` (`demand_id`) ON DELETE SET NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'notification table';


-- ============================================================
-- V5__create_evaluation_table.sql
-- ============================================================
CREATE TABLE evaluation (
    evaluation_id  BIGINT       NOT NULL AUTO_INCREMENT,
    demand_id      BIGINT       NOT NULL,
    evaluator_id   BIGINT       NOT NULL,
    target_user_id BIGINT       NOT NULL,
    rating         INT          NOT NULL,
    comment        VARCHAR(500),
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (evaluation_id),
    UNIQUE uk_demand_evaluator (demand_id, evaluator_id),
    FOREIGN KEY (demand_id)     REFERENCES demand(demand_id) ON DELETE CASCADE,
    FOREIGN KEY (evaluator_id)  REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (target_user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- V6__create_chat_tables.sql
-- ============================================================
-- V6: Private chat system — conversations and messages

CREATE TABLE conversation (
    conversation_id  BIGINT       NOT NULL AUTO_INCREMENT,
    demand_id        BIGINT       NOT NULL,
    user1_id         BIGINT       NOT NULL COMMENT 'smaller user_id for deterministic pair ordering',
    user2_id         BIGINT       NOT NULL,
    last_message     VARCHAR(500),
    last_message_at  TIMESTAMP    NULL,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id),
    UNIQUE uk_demand_users (demand_id, user1_id, user2_id),
    FOREIGN KEY (demand_id) REFERENCES demand(demand_id) ON DELETE CASCADE,
    FOREIGN KEY (user1_id)  REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id)  REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE message (
    message_id       BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id  BIGINT       NOT NULL,
    sender_id        BIGINT       NOT NULL,
    content          TEXT         NOT NULL,
    is_read          TINYINT      NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    INDEX idx_conv_time (conversation_id, create_time),
    FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id)       REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- V7__add_images_to_demand.sql
-- ============================================================
ALTER TABLE demand ADD COLUMN images TEXT NULL AFTER description;


-- ============================================================
-- V8__add_image_to_message.sql
-- ============================================================
ALTER TABLE message ADD COLUMN message_type VARCHAR(32) NOT NULL DEFAULT 'text' AFTER content;
ALTER TABLE message ADD COLUMN image_url VARCHAR(500) NULL AFTER message_type;


-- ============================================================
-- V9__add_attributes_to_demand.sql
-- ============================================================
ALTER TABLE demand ADD COLUMN attributes TEXT NULL COMMENT '类型特有字段（JSON）' AFTER images;


