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
