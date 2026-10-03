-- MySQL 初始化脚本
-- 创建数据库表结构和初始数据

USE education_question_bank;

-- 创建学校表
CREATE TABLE IF NOT EXISTS schools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(255),
    description VARCHAR(255)
);

-- 创建学院表
CREATE TABLE IF NOT EXISTS colleges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    school_id BIGINT NOT NULL,
    FOREIGN KEY (school_id) REFERENCES schools(id)
);

-- 创建专业表
CREATE TABLE IF NOT EXISTS majors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255),
    description VARCHAR(255),
    college_id BIGINT NOT NULL,
    FOREIGN KEY (college_id) REFERENCES colleges(id)
);

-- 创建班级表
CREATE TABLE IF NOT EXISTS classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    grade INT,
    student_count INT,
    studentCount INT,
    major_id BIGINT NOT NULL,
    FOREIGN KEY (major_id) REFERENCES majors(id)
);

-- 创建科目表
CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 创建章节表
CREATE TABLE IF NOT EXISTS chapters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    order_num INT,
    orderNum INT,
    subject_id BIGINT,
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'TEACHER',
    name VARCHAR(255),
    birthday DATE,
    email VARCHAR(255),
    gender VARCHAR(255),
    phone VARCHAR(255),
    enabled BIT(1) NOT NULL DEFAULT 1
);

-- 创建题目表
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    answer TEXT NOT NULL,
    analysis TEXT,
    type ENUM('FILL_IN_THE_BLANK', 'MULTIPLE_CHOICE', 'SHORT_ANSWER', 'SINGLE_CHOICE', 'TRUE_FALSE') NOT NULL,
    difficulty INT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    score INT,
    title VARCHAR(255),
    chapter_id BIGINT,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id)
);

-- 创建题目选项表
CREATE TABLE IF NOT EXISTS question_options (
    question_id BIGINT NOT NULL,
    option_text VARCHAR(255),
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- 创建考试表
CREATE TABLE IF NOT EXISTS exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    create_time DATETIME(6),
    createTime DATETIME(6),
    creator VARCHAR(255),
    duration INT,
    exam_date DATETIME(6),
    examDate DATETIME(6),
    exam_type VARCHAR(255),
    examType VARCHAR(255),
    status VARCHAR(255),
    total_score INT,
    totalScore INT,
    subject_id BIGINT,
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

-- 插入初始数据
INSERT IGNORE INTO schools (name, address, description) VALUES 
('江苏师范大学', '江苏省徐州市', '江苏师范大学');

INSERT IGNORE INTO colleges (name, description, school_id) VALUES 
('计算机科学与技术学院', '计算机科学与技术学院', 1),
('教育科学学院', '教育科学学院', 1);

INSERT IGNORE INTO majors (name, code, description, college_id) VALUES 
('计算机科学与技术', 'CS001', '计算机科学与技术专业', 1),
('软件工程', 'SE001', '软件工程专业', 1),
('教育技术学', 'ET001', '教育技术学专业', 2);

INSERT IGNORE INTO subjects (name, description) VALUES 
('语文', '语文学科'),
('数学', '数学学科'),
('英语', '英语学科');

INSERT IGNORE INTO users (username, password, role, name, email, enabled) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKXgwHNEM5NjjJLkTCiYaXr6lDm2', 'TEACHER', '系统管理员', 'admin@example.com', 1),
('teacher1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKXgwHNEM5NjjJLkTCiYaXr6lDm2', 'TEACHER', '张老师', 'teacher1@example.com', 1);

-- 创建索引
CREATE INDEX idx_questions_subject ON questions(subject);
CREATE INDEX idx_questions_type ON questions(type);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);

COMMIT;
