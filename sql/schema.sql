-- 用户表
CREATE TABLE users (
  id CHAR(36) PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  email VARCHAR(128) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
  user_id CHAR(36) NOT NULL,
  role VARCHAR(64) NOT NULL,
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 系统设置
CREATE TABLE system_settings (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36),
  base_url VARCHAR(255),
  model_name VARCHAR(128),
  api_key_encrypted TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 故事与角色
CREATE TABLE stories (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  synopsis TEXT,
  genre VARCHAR(64),
  tone VARCHAR(64),
  status VARCHAR(32),
  world_id VARCHAR(64),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE character_cards (
  id CHAR(36) PRIMARY KEY,
  story_id CHAR(36) NOT NULL,
  name VARCHAR(128),
  synopsis TEXT,
  details TEXT,
  relationships TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (story_id) REFERENCES stories(id)
);

-- 大纲与稿件
CREATE TABLE outlines (
  id CHAR(36) PRIMARY KEY,
  story_id CHAR(36) NOT NULL,
  title VARCHAR(255),
  world_id VARCHAR(64),
  content_json LONGTEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (story_id) REFERENCES stories(id)
);

CREATE TABLE manuscripts (
  id CHAR(36) PRIMARY KEY,
  outline_id CHAR(36) NOT NULL,
  title VARCHAR(255),
  world_id VARCHAR(64),
  sections_json LONGTEXT,
  character_logs_json LONGTEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (outline_id) REFERENCES outlines(id)
);

-- 素材与上传
CREATE TABLE materials (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36),
  title VARCHAR(255),
  type VARCHAR(32),
  content LONGTEXT,
  summary TEXT,
  tags_json TEXT,
  status VARCHAR(32),
  entities_json TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE material_upload_jobs (
  id CHAR(36) PRIMARY KEY,
  file_name VARCHAR(255),
  status VARCHAR(32),
  progress INT,
  message TEXT,
  result_material_id CHAR(36),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (result_material_id) REFERENCES materials(id)
);

-- 世界观
CREATE TABLE worlds (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36),
  name VARCHAR(255),
  tagline VARCHAR(255),
  status VARCHAR(32),
  version VARCHAR(32),
  themes_json TEXT,
  creative_intent TEXT,
  notes TEXT,
  modules_json LONGTEXT,
  module_progress_json TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 提示词模板
CREATE TABLE prompt_templates (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36),
  story_creation LONGTEXT,
  outline_chapter LONGTEXT,
  manuscript_section LONGTEXT,
  refine_with_instruction LONGTEXT,
  refine_without_instruction LONGTEXT,
  updated_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE world_prompt_templates (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36),
  modules_json LONGTEXT,
  final_templates_json LONGTEXT,
  field_refine LONGTEXT,
  updated_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
