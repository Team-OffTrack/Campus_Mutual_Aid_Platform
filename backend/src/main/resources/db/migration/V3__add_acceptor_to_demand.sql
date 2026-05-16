-- V3: Add acceptor_id to demand table for order management

ALTER TABLE `demand`
    ADD COLUMN `acceptor_id` BIGINT NULL DEFAULT NULL COMMENT 'acceptor user ID' AFTER `publisher_id`,
    ADD INDEX `idx_acceptor` (`acceptor_id`) USING BTREE,
    ADD CONSTRAINT `fk_demand_acceptor` FOREIGN KEY (`acceptor_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL;
