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
