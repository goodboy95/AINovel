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
CREATE TABLE `manuscript_sections`  (
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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKppqtshxh48rvqalsyxjmaonjt`(`story_card_id` ASC) USING BTREE,
  INDEX `FKjurndtjfa6wkjg6iwyexqgy5n`(`user_id` ASC) USING BTREE,
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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKbyhxq84adsppi4alnbuo7q52o`(`user_id` ASC) USING BTREE,
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
  `llm_provider` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
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
