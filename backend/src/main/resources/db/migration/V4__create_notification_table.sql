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
