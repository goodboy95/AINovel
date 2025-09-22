/*
 Navicat Premium Dump SQL

 Source Server         : seekerhut
 Source Server Type    : MySQL
 Source Server Version : 80038 (8.0.38)
 Source Host           : 43.136.95.117:3306
 Source Schema         : ainovel

 Target Server Type    : MySQL
 Target Server Version : 80038 (8.0.38)
 File Encoding         : 65001

 Date: 24/07/2025 23:00:15
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for character_cards
-- ----------------------------
DROP TABLE IF EXISTS `character_cards`;
CREATE TABLE `character_cards`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `relationships` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `synopsis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `story_card_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK2n3okhwtohsdneg24hoy71g8i`(`story_card_id` ASC) USING BTREE,
  INDEX `FK2vtouhugcyt6pyrvnd6p2yacc`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FK2n3okhwtohsdneg24hoy71g8i` FOREIGN KEY (`story_card_id`) REFERENCES `story_cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK2vtouhugcyt6pyrvnd6p2yacc` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for manuscript_sections
-- ----------------------------
DROP TABLE IF EXISTS `manuscript_sections`;
CREATE TABLE `manuscript_sections`
-- ----------------------------
-- Table structure for character_change_log
-- ----------------------------
DROP TABLE IF EXISTS `character_change_log`;
CREATE TABLE `character_change_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `character_id` bigint NOT NULL,
  `manuscript_id` bigint NOT NULL,
  `outline_id` bigint NULL DEFAULT NULL,
  `chapter_number` int NOT NULL,
  `section_number` int NOT NULL,
  `newly_known_info` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `character_changes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `character_details_after` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_auto_copied` bit(1) NOT NULL DEFAULT b'0',
  `relationship_changes` json NULL,
  `is_turning_point` bit(1) NOT NULL DEFAULT b'0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` datetime(6) NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_change_log_character`(`character_id`) USING BTREE,
  INDEX `idx_change_log_manuscript`(`manuscript_id`) USING BTREE,
  CONSTRAINT `fk_change_log_character` FOREIGN KEY (`character_id`) REFERENCES `character_cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_log_manuscript` FOREIGN KEY (`manuscript_id`) REFERENCES `manuscripts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_log_outline` FOREIGN KEY (`outline_id`) REFERENCES `outline_cards` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;
  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_active` bit(1) NULL DEFAULT NULL,
  `version` int NULL DEFAULT NULL,
  `scene_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK9u9w0yih8n376p3e37t6p2bj4`(`scene_id` ASC) USING BTREE,
  CONSTRAINT `FK9u9w0yih8n376p3e37t6p2bj4` FOREIGN KEY (`scene_id`) REFERENCES `outline_scenes` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for outline_cards
-- ----------------------------
DROP TABLE IF EXISTS `outline_cards`;
CREATE TABLE `outline_cards`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `point_of_view` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `story_card_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `world_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKppqtshxh48rvqalsyxjmaonjt`(`story_card_id` ASC) USING BTREE,
  INDEX `FKjurndtjfa6wkjg6iwyexqgy5n`(`user_id` ASC) USING BTREE,
  INDEX `idx_outline_cards_world`(`world_id` ASC) USING BTREE,
  CONSTRAINT `FKjurndtjfa6wkjg6iwyexqgy5n` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKppqtshxh48rvqalsyxjmaonjt` FOREIGN KEY (`story_card_id`) REFERENCES `story_cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for outline_chapters
-- ----------------------------
DROP TABLE IF EXISTS `outline_chapters`;
CREATE TABLE `outline_chapters`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chapter_number` int NOT NULL,
  `synopsis` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `outline_card_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK3arefuudf8xk63informf6lds`(`outline_card_id` ASC) USING BTREE,
  CONSTRAINT `FK3arefuudf8xk63informf6lds` FOREIGN KEY (`outline_card_id`) REFERENCES `outline_cards` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for outline_scenes
-- ----------------------------
DROP TABLE IF EXISTS `outline_scenes`;
CREATE TABLE `outline_scenes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expected_words` int NULL DEFAULT NULL,
  `scene_number` int NOT NULL,
  `synopsis` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `chapter_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKoewva0r1owesn6t3eqi20hx7p`(`chapter_id` ASC) USING BTREE,
  CONSTRAINT `FKoewva0r1owesn6t3eqi20hx7p` FOREIGN KEY (`chapter_id`) REFERENCES `outline_chapters` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for story_cards
-- ----------------------------
DROP TABLE IF EXISTS `story_cards`;
CREATE TABLE `story_cards`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `genre` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `story_arc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `synopsis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `world_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKbyhxq84adsppi4alnbuo7q52o`(`user_id` ASC) USING BTREE,
  INDEX `idx_story_cards_world`(`world_id` ASC) USING BTREE,
  CONSTRAINT `FKbyhxq84adsppi4alnbuo7q52o` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_settings
-- ----------------------------
DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `api_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `custom_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_4bos7satl9xeqd18frfeqg6tt`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FK8v82nj88rmai0nyck19f873dw` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_6dotkott2kjsp8vw4d0m25fb7`(`email` ASC) USING BTREE,
  UNIQUE INDEX `UK_r43af9ap4edm43mmtq01oddj6`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


-- ----------------------------
-- Table structure for world_generation_jobs
-- ----------------------------
DROP TABLE IF EXISTS `world_generation_jobs`;
CREATE TABLE `world_generation_jobs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `world_id` bigint NOT NULL,
  `module_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `job_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sequence` int NOT NULL,
  `attempts` int NOT NULL DEFAULT 0,
  `last_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `started_at` datetime NULL,
  `finished_at` datetime NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_generation_jobs_world` (`world_id`,`status`,`sequence`),
  KEY `idx_generation_jobs_status_seq` (`status`,`sequence`),
  CONSTRAINT `fk_generation_job_world` FOREIGN KEY (`world_id`) REFERENCES `worlds` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Version 2.2 Changes
-- ----------------------------

-- Add new fields to temporary_characters table and migrate description to summary
ALTER TABLE `temporary_characters`
CHANGE COLUMN `description` `summary` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '概要 (原description字段)',
ADD COLUMN `details` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详情',
ADD COLUMN `relationships` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '与核心人物的关系',
ADD COLUMN `status_in_scene` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在本节中的状态',
ADD COLUMN `mood_in_scene` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在本节中的心情',
ADD COLUMN `actions_in_scene` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在本节中的核心行动';

-- ----------------------------
-- Table structure for worlds
-- ----------------------------
DROP TABLE IF EXISTS `worlds`;
CREATE TABLE `worlds`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tagline` varchar(180) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `themes` json NULL,
  `creative_intent` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version` int NOT NULL DEFAULT 0,
  `published_at` datetime(6) NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` datetime(6) NULL,
  `last_edited_by` bigint NULL,
  `last_edited_at` datetime(6) NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_worlds_user_status`(`user_id`, `status`),
  KEY `idx_worlds_status_updated`(`status`, `updated_at`),
  CONSTRAINT `fk_worlds_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for world_modules
-- ----------------------------
DROP TABLE IF EXISTS `world_modules`;
CREATE TABLE `world_modules`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `world_id` bigint NOT NULL,
  `module_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fields` json NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `full_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `full_content_updated_at` datetime(6) NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `last_edited_by` bigint NULL,
  `last_edited_at` datetime(6) NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_world_module_key`(`world_id`, `module_key`),
  KEY `idx_world_modules_world`(`world_id`),
  KEY `idx_world_modules_status`(`status`),
  CONSTRAINT `fk_world_modules_world` FOREIGN KEY (`world_id`) REFERENCES `worlds` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for world_prompt_settings
-- ----------------------------
DROP TABLE IF EXISTS `world_prompt_settings`;
CREATE TABLE `world_prompt_settings`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `module_templates` json NULL,
  `final_templates` json NULL,
  `field_refine_template` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_world_prompt_user`(`user_id`),
  CONSTRAINT `fk_world_prompt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Version 2.3 Changes
-- ----------------------------

-- Create structured scene character table
CREATE TABLE IF NOT EXISTS `scene_characters`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene_id` bigint NOT NULL,
  `character_card_id` bigint NULL DEFAULT NULL,
  `character_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `thought` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `action` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_scene_characters_scene_id` (`scene_id`),
  KEY `idx_scene_characters_card_id` (`character_card_id`),
  CONSTRAINT `fk_scene_characters_scene` FOREIGN KEY (`scene_id`) REFERENCES `outline_scenes` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_scene_characters_card` FOREIGN KEY (`character_card_id`) REFERENCES `character_cards` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- 如果生产环境中仍然保留 legacy 的 outline_scenes.character_states JSON 字段，
-- 需要在删除该列之前先将数据迁移到 scene_characters 表。
-- 以下示例脚本覆盖了两种常见的历史结构，执行前请根据实际 JSON 结构验证并在事务中运行：
--
-- 1) character_states 为数组结构（每个元素带有 characterName/status/thought/action 字段）时：
-- INSERT INTO scene_characters (scene_id, character_name, status, thought, action)
-- SELECT
--   os.id,
--   jt.character_name,
--   jt.status,
--   jt.thought,
--   jt.action
-- FROM outline_scenes AS os
-- CROSS JOIN JSON_TABLE(
--   os.character_states,
--   '$[*]' COLUMNS (
--     character_name VARCHAR(255) PATH '$.characterName',
--     status TEXT PATH '$.status',
--     thought TEXT PATH '$.thought',
--     action TEXT PATH '$.action'
--   )
-- ) AS jt
-- WHERE os.character_states IS NOT NULL;
--
-- 2) character_states 为简单的键值对（{"角色名": "人物状态描述"}）时：
-- INSERT INTO scene_characters (scene_id, character_name, status)
-- SELECT
--   os.id,
--   key_list.character_name,
--   JSON_UNQUOTE(JSON_EXTRACT(os.character_states, CONCAT('$."', key_list.character_name, '"')))
-- FROM outline_scenes AS os
-- CROSS JOIN JSON_TABLE(
--   JSON_KEYS(os.character_states),
--   '$[*]' COLUMNS (character_name VARCHAR(255) PATH '$')
-- ) AS key_list
-- WHERE JSON_TYPE(os.character_states) = 'OBJECT';
--
-- 无论采用哪种方式，均应迁移完成并人工抽样校验后，再执行下面的 DROP COLUMN 操作。

-- Remove legacy character_states column from outline_scenes if it exists
ALTER TABLE `outline_scenes` DROP COLUMN IF EXISTS `character_states`;

-- Rename temporary character columns to align with new schema
ALTER TABLE `temporary_characters`
  CHANGE COLUMN `status_in_scene` `status` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在本节中的状态',
  CHANGE COLUMN `mood_in_scene` `thought` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在本节中的想法',
  CHANGE COLUMN `actions_in_scene` `action` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '在本节中的行动';

-- Note: The column names in the SQL script use snake_case (e.g., status_in_scene)
-- to follow common SQL conventions, which will be automatically mapped by Hibernate
-- to the camelCase field names in the Java Entity (e.g., statusInScene).

-- ----------------------------
-- Step 6: World integration for workbench entities
-- ----------------------------
ALTER TABLE `story_cards`
  ADD COLUMN IF NOT EXISTS `world_id` bigint NULL DEFAULT NULL;
ALTER TABLE `story_cards`
  ADD INDEX IF NOT EXISTS `idx_story_cards_world` (`world_id`);

ALTER TABLE `outline_cards`
  ADD COLUMN IF NOT EXISTS `world_id` bigint NULL DEFAULT NULL;
ALTER TABLE `outline_cards`
  ADD INDEX IF NOT EXISTS `idx_outline_cards_world` (`world_id`);

ALTER TABLE `manuscripts`
  ADD COLUMN IF NOT EXISTS `world_id` bigint NULL DEFAULT NULL;
ALTER TABLE `manuscripts`
  ADD INDEX IF NOT EXISTS `idx_manuscripts_world` (`world_id`);

-- ----------------------------
-- Version 2.3 Schema Alignment
-- ----------------------------
-- Align user_settings with simplified AI settings (OpenAI-only):
-- 1) Add base_url column
-- 2) Drop deprecated llm_provider column
ALTER TABLE `user_settings` ADD COLUMN `base_url` varchar(255) NULL DEFAULT NULL AFTER `custom_prompt`;
ALTER TABLE `user_settings` DROP COLUMN `llm_provider`;


