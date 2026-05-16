-- 校园互助平台 P3 数据库设计

-- 自动创建并使用数据库
CREATE DATABASE IF NOT EXISTS campus_help DEFAULT CHARACTER SET utf8mb4;
USE campus_help;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `student_id` varchar(32) NOT NULL COMMENT '学号',
  `password` varchar(128) NOT NULL COMMENT '加密密码',
  `name` varchar(64) NOT NULL COMMENT '姓名',
  `avatar` varchar(255) NULL DEFAULT NULL COMMENT '头像',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态 1正常 0封禁',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_student_id`(`student_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '用户表';

-- ----------------------------
-- 2. 隐私配置表
-- ----------------------------
DROP TABLE IF EXISTS `privacy_profile`;
CREATE TABLE `privacy_profile`  (
  `privacy_id` bigint NOT NULL AUTO_INCREMENT COMMENT '隐私配置ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `is_anonymous` tinyint NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `mask_name` varchar(64) NULL DEFAULT '' COMMENT '虚拟昵称',
  PRIMARY KEY (`privacy_id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id`) USING BTREE,
  CONSTRAINT `fk_privacy_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '隐私配置表';

-- ----------------------------
-- 3. 用户账户表（积分+信誉）
-- ----------------------------
DROP TABLE IF EXISTS `user_account`;
CREATE TABLE `user_account`  (
  `account_id` bigint NOT NULL AUTO_INCREMENT COMMENT '账户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `available_points` int NOT NULL DEFAULT 0 COMMENT '可用积分',
  `frozen_points` int NOT NULL DEFAULT 0 COMMENT '冻结积分',
  `reputation_score` double NOT NULL DEFAULT 5.0 COMMENT '信誉分',
  PRIMARY KEY (`account_id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id`) USING BTREE,
  CONSTRAINT `fk_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '用户账户表';

-- ----------------------------
-- 4. 基础订单表
-- ----------------------------
DROP TABLE IF EXISTS `base_order`;
CREATE TABLE `base_order`  (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `acceptor_id` bigint NULL DEFAULT NULL COMMENT '接单者ID',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `description` text NULL COMMENT '描述',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`order_id`) USING BTREE,
  INDEX `idx_publisher`(`publisher_id`) USING BTREE,
  INDEX `idx_acceptor`(`acceptor_id`) USING BTREE,
  INDEX `idx_status_create`(`status`, `create_time`) USING BTREE,
  CONSTRAINT `fk_order_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_order_acceptor` FOREIGN KEY (`acceptor_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '基础订单表';

-- ----------------------------
-- 5. 积分订单表
-- ----------------------------
DROP TABLE IF EXISTS `paid_order`;
CREATE TABLE `paid_order`  (
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `points_reward` int NOT NULL COMMENT '积分报酬',
  PRIMARY KEY (`order_id`) USING BTREE,
  CONSTRAINT `fk_paid_base` FOREIGN KEY (`order_id`) REFERENCES `base_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '积分订单表';

-- ----------------------------
-- 6. 跑腿订单
-- ----------------------------
DROP TABLE IF EXISTS `errand_order`;
CREATE TABLE `errand_order`  (
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `errand_type` varchar(64) NOT NULL COMMENT '跑腿类型',
  `pickup_location` varchar(255) NOT NULL COMMENT '取件地点',
  `delivery_location` varchar(255) NOT NULL COMMENT '送达地点',
  `deadline` datetime NOT NULL COMMENT '截止时间',
  PRIMARY KEY (`order_id`) USING BTREE,
  CONSTRAINT `fk_errand_paid` FOREIGN KEY (`order_id`) REFERENCES `paid_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '跑腿订单表';

-- ----------------------------
-- 7. 二手交易订单
-- ----------------------------
DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order`  (
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `category` varchar(64) NOT NULL COMMENT '商品分类',
  `cash_price` double NOT NULL DEFAULT 0 COMMENT '现金价格',
  PRIMARY KEY (`order_id`) USING BTREE,
  CONSTRAINT `fk_trade_paid` FOREIGN KEY (`order_id`) REFERENCES `paid_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '二手交易订单表';

-- ----------------------------
-- 8. 组队订单
-- ----------------------------
DROP TABLE IF EXISTS `team_order`;
CREATE TABLE `team_order`  (
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `team_type` varchar(64) NOT NULL COMMENT '组队类型',
  `max_members` int NOT NULL COMMENT '最大人数',
  PRIMARY KEY (`order_id`) USING BTREE,
  CONSTRAINT `fk_team_paid` FOREIGN KEY (`order_id`) REFERENCES `paid_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '组队订单表';

-- ----------------------------
-- 9. 失物招领订单
-- ----------------------------
DROP TABLE IF EXISTS `lost_found_order`;
CREATE TABLE `lost_found_order`  (
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `type` int NOT NULL COMMENT '1寻物 2招领',
  `item_location` varchar(255) NOT NULL COMMENT '地点',
  PRIMARY KEY (`order_id`) USING BTREE,
  CONSTRAINT `fk_lost_base` FOREIGN KEY (`order_id`) REFERENCES `base_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '失物招领表';

-- ----------------------------
-- 10. 聊天会话
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
  `session_id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `user_a_id` bigint NOT NULL COMMENT '用户A',
  `user_b_id` bigint NOT NULL COMMENT '用户B',
  PRIMARY KEY (`session_id`) USING BTREE,
  UNIQUE INDEX `uk_order_id`(`order_id`) USING BTREE,
  CONSTRAINT `fk_session_order` FOREIGN KEY (`order_id`) REFERENCES `base_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '聊天会话表';

-- ----------------------------
-- 11. 聊天消息
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `content` text NULL COMMENT '内容',
  `file_url` varchar(255) NULL DEFAULT '' COMMENT '文件地址',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`message_id`) USING BTREE,
  INDEX `idx_session`(`session_id`) USING BTREE,
  INDEX `idx_session_time`(`session_id`, `send_time`) USING BTREE,
  CONSTRAINT `fk_msg_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`session_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '聊天消息表';

-- ----------------------------
-- 12. 评价表
-- ----------------------------
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation`  (
  `evaluation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `evaluator_id` bigint NOT NULL COMMENT '评价人',
  `target_user_id` bigint NOT NULL COMMENT '被评价人',
  `rating` int NOT NULL COMMENT '1-5分',
  `comment` text NULL COMMENT '评价内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`evaluation_id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE,
  INDEX `idx_target_user`(`target_user_id`) USING BTREE,
  CONSTRAINT `fk_eval_order` FOREIGN KEY (`order_id`) REFERENCES `base_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '评价表';

-- ----------------------------
-- 13. 订单图片表
-- ----------------------------
DROP TABLE IF EXISTS `order_image`;
CREATE TABLE `order_image`  (
  `image_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `image_url` varchar(255) NOT NULL,
  PRIMARY KEY (`image_id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE,
  CONSTRAINT `fk_img_order` FOREIGN KEY (`order_id`) REFERENCES `base_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '订单图片表';

-- ----------------------------
-- 14. 组队成员表
-- ----------------------------
DROP TABLE IF EXISTS `team_member`;
CREATE TABLE `team_member`  (
  `member_id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`member_id`) USING BTREE,
  UNIQUE INDEX `uk_order_user`(`order_id`, `user_id`) USING BTREE,
  CONSTRAINT `fk_member_team` FOREIGN KEY (`order_id`) REFERENCES `team_order` (`order_id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '组队成员表';

SET FOREIGN_KEY_CHECKS = 1;