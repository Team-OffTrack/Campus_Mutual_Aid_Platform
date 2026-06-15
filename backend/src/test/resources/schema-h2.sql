-- H2-compatible schema for tests (mirrors V1__init_user_tables.sql)

CREATE TABLE IF NOT EXISTS user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    student_id    VARCHAR(32)  NOT NULL,
    password      VARCHAR(128) NOT NULL,
    name          VARCHAR(64)  NOT NULL,
    avatar        VARCHAR(255),
    role          VARCHAR(32)  NOT NULL DEFAULT 'USER',
    status        INT          NOT NULL DEFAULT 1,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE (student_id)
);

CREATE TABLE IF NOT EXISTS privacy_profile (
    privacy_id    BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    is_anonymous  INT         NOT NULL DEFAULT 0,
    mask_name     VARCHAR(64) DEFAULT '',
    PRIMARY KEY (privacy_id),
    UNIQUE (user_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_account (
    account_id        BIGINT  NOT NULL AUTO_INCREMENT,
    user_id           BIGINT  NOT NULL,
    available_points  INT     NOT NULL DEFAULT 0,
    frozen_points     INT     NOT NULL DEFAULT 0,
    reputation_score  DOUBLE  NOT NULL DEFAULT 5.0,
    PRIMARY KEY (account_id),
    UNIQUE (user_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS demand (
    demand_id     BIGINT       NOT NULL AUTO_INCREMENT,
    publisher_id  BIGINT       NOT NULL,
    acceptor_id   BIGINT       DEFAULT NULL,
    type          VARCHAR(32)  NOT NULL,
    title         VARCHAR(128) NOT NULL,
    description   TEXT,
    images        TEXT,
    attributes    TEXT,
    location      VARCHAR(255),
    deadline      TIMESTAMP,
    reward_type   VARCHAR(32)  DEFAULT 'point',
    reward_amount INT          DEFAULT 0,
    is_anonymous  INT          NOT NULL DEFAULT 0,
    status        VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (demand_id),
    FOREIGN KEY (publisher_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (acceptor_id) REFERENCES user(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS notification (
    notification_id   BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL,
    type              VARCHAR(32) NOT NULL,
    title             VARCHAR(128) NOT NULL,
    content           TEXT,
    is_read           INT         NOT NULL DEFAULT 0,
    related_demand_id BIGINT      DEFAULT NULL,
    create_time       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (related_demand_id) REFERENCES demand(demand_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS evaluation (
    evaluation_id  BIGINT       NOT NULL AUTO_INCREMENT,
    demand_id      BIGINT       NOT NULL,
    evaluator_id   BIGINT       NOT NULL,
    target_user_id BIGINT       NOT NULL,
    rating         INT          NOT NULL,
    comment        VARCHAR(500),
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (evaluation_id),
    UNIQUE (demand_id, evaluator_id),
    FOREIGN KEY (demand_id)     REFERENCES demand(demand_id) ON DELETE CASCADE,
    FOREIGN KEY (evaluator_id)  REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (target_user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS conversation (
    conversation_id  BIGINT       NOT NULL AUTO_INCREMENT,
    demand_id        BIGINT       NOT NULL,
    user1_id         BIGINT       NOT NULL,
    user2_id         BIGINT       NOT NULL,
    last_message     VARCHAR(500),
    last_message_at  TIMESTAMP,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id),
    UNIQUE (demand_id, user1_id, user2_id),
    FOREIGN KEY (demand_id) REFERENCES demand(demand_id) ON DELETE CASCADE,
    FOREIGN KEY (user1_id)  REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id)  REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS message (
    message_id       BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id  BIGINT       NOT NULL,
    sender_id        BIGINT       NOT NULL,
    content          TEXT         NOT NULL,
    message_type     VARCHAR(32)  NOT NULL DEFAULT 'text',
    image_url        VARCHAR(500),
    is_read          INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id)       REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS team_member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    role        VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    message     VARCHAR(256),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (demand_id, user_id),
    FOREIGN KEY (demand_id) REFERENCES demand(demand_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS points_transaction (
    transaction_id  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    amount          INT          NOT NULL,
    balance_after   INT          NOT NULL,
    type            VARCHAR(32)  NOT NULL,
    reference_id    BIGINT       DEFAULT NULL,
    description     VARCHAR(255) DEFAULT NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS daily_checkin (
    checkin_id     BIGINT    NOT NULL AUTO_INCREMENT,
    user_id        BIGINT    NOT NULL,
    checkin_date   DATE      NOT NULL,
    points_awarded INT       NOT NULL DEFAULT 0,
    streak         INT       NOT NULL DEFAULT 0,
    create_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (checkin_id),
    UNIQUE (user_id, checkin_date),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_id   BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (demand_id, user_id),
    FOREIGN KEY (demand_id) REFERENCES demand(demand_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS report (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id   BIGINT NOT NULL,
    reason      VARCHAR(32) NOT NULL,
    description VARCHAR(512),
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    admin_note  VARCHAR(512),
    admin_id    BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolve_time TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES user(user_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_report_target ON report(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_report_reporter ON report(reporter_id);
CREATE INDEX IF NOT EXISTS idx_report_status ON report(status);

-- V14: Badge tables (H2-compatible)
CREATE TABLE user_badge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    badge_key   VARCHAR(32) NOT NULL,
    earned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, badge_key),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ub_user ON user_badge(user_id);

CREATE TABLE worn_badge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE,
    badge_key   VARCHAR(32) NOT NULL,
    worn_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wb_user ON worn_badge(user_id);
