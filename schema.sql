-- 명언 앱 DB 스키마
-- 개발 DB: wiseSaying__dev / 테스트 DB: wiseSaying__test

CREATE DATABASE IF NOT EXISTS wiseSaying__dev CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS wiseSaying__test CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE wiseSaying__dev;

CREATE TABLE IF NOT EXISTS wise_saying (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id),
    createDate DATETIME NOT NULL,
    modifyDate DATETIME NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL
);

USE wiseSaying__test;

CREATE TABLE IF NOT EXISTS wise_saying (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id),
    createDate DATETIME NOT NULL,
    modifyDate DATETIME NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL
);
