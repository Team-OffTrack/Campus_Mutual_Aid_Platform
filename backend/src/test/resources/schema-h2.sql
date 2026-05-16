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
    type          VARCHAR(32)  NOT NULL,
    title         VARCHAR(128) NOT NULL,
    description   TEXT,
    location      VARCHAR(255),
    deadline      TIMESTAMP,
    reward_type   VARCHAR(32)  DEFAULT 'point',
    reward_amount INT          DEFAULT 0,
    is_anonymous  INT          NOT NULL DEFAULT 0,
    status        VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (demand_id),
    FOREIGN KEY (publisher_id) REFERENCES user(user_id) ON DELETE CASCADE
);
