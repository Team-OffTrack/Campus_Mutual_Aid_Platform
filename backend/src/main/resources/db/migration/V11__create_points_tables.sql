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
