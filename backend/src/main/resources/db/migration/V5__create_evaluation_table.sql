CREATE TABLE evaluation (
    evaluation_id  BIGINT       NOT NULL AUTO_INCREMENT,
    demand_id      BIGINT       NOT NULL,
    evaluator_id   BIGINT       NOT NULL,
    target_user_id BIGINT       NOT NULL,
    rating         INT          NOT NULL,
    comment        VARCHAR(500),
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (evaluation_id),
    UNIQUE uk_demand_evaluator (demand_id, evaluator_id),
    FOREIGN KEY (demand_id)     REFERENCES demand(demand_id) ON DELETE CASCADE,
    FOREIGN KEY (evaluator_id)  REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (target_user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
