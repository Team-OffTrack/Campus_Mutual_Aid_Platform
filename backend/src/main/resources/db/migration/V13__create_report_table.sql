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
