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
