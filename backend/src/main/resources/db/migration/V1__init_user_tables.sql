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
