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
