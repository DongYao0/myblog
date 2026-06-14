CREATE DATABASE IF NOT EXISTS myblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE myblog;

CREATE TABLE IF NOT EXISTS health_check_marker (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO health_check_marker(name)
SELECT 'bootstrap'
WHERE NOT EXISTS (SELECT 1 FROM health_check_marker WHERE name = 'bootstrap');
