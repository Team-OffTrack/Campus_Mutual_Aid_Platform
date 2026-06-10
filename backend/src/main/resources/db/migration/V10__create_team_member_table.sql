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
