CREATE TABLE IF NOT EXISTS sys_role (
    id INT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(20) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS clazz (
    id INT NOT NULL AUTO_INCREMENT,
    class_no VARCHAR(30) NOT NULL UNIQUE,
    class_name VARCHAR(80) NOT NULL,
    grade VARCHAR(10) NOT NULL,
    major VARCHAR(80) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS teacher (
    id INT NOT NULL AUTO_INCREMENT,
    teacher_no VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(4),
    title VARCHAR(40),
    phone VARCHAR(20),
    email VARCHAR(80),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS student (
    id INT NOT NULL AUTO_INCREMENT,
    student_no VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(4),
    clazz_id INT NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(80),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_student_clazz (clazz_id),
    CONSTRAINT fk_student_clazz FOREIGN KEY (clazz_id) REFERENCES clazz(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS sys_user (
    id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role_code VARCHAR(20) NOT NULL,
    ref_id INT,
    real_name VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS course (
    id INT NOT NULL AUTO_INCREMENT,
    course_no VARCHAR(30) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    credit DECIMAL(3,1) NOT NULL DEFAULT 2.0,
    semester VARCHAR(20) NOT NULL,
    teacher_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course_teacher (teacher_id),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS course_class (
    id INT NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL,
    clazz_id INT NOT NULL,
    UNIQUE (course_id, clazz_id),
    PRIMARY KEY (id),
    CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_cc_clazz FOREIGN KEY (clazz_id) REFERENCES clazz(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS eval_rule (
    id INT NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL UNIQUE,
    attendance_weight DECIMAL(5,2) NOT NULL DEFAULT 20,
    performance_weight DECIMAL(5,2) NOT NULL DEFAULT 20,
    experiment_weight DECIMAL(5,2) NOT NULL DEFAULT 30,
    homework_weight DECIMAL(5,2) NOT NULL DEFAULT 30,
    regular_ratio DECIMAL(5,2) NOT NULL DEFAULT 40,
    exam_ratio DECIMAL(5,2) NOT NULL DEFAULT 60,
    PRIMARY KEY (id),
    CONSTRAINT fk_rule_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attendance (
    id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    attend_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remark VARCHAR(200),
    recorder_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, course_id, attend_date),
    PRIMARY KEY (id),
    CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    CONSTRAINT fk_att_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS performance (
    id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    record_date DATE NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    comment VARCHAR(200),
    recorder_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_perf_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    CONSTRAINT fk_perf_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS experiment (
    id INT NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    full_score DECIMAL(5,2) NOT NULL DEFAULT 100,
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_exp_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS experiment_record (
    id INT NOT NULL AUTO_INCREMENT,
    experiment_id INT NOT NULL,
    student_id INT NOT NULL,
    score DECIMAL(5,2),
    completion DECIMAL(5,2) NOT NULL DEFAULT 0,
    submit_time TIMESTAMP,
    remark VARCHAR(200),
    UNIQUE (experiment_id, student_id),
    PRIMARY KEY (id),
    CONSTRAINT fk_expr_exp FOREIGN KEY (experiment_id) REFERENCES experiment(id) ON DELETE CASCADE,
    CONSTRAINT fk_expr_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS homework (
    id INT NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    full_score DECIMAL(5,2) NOT NULL DEFAULT 100,
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_hw_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS homework_record (
    id INT NOT NULL AUTO_INCREMENT,
    homework_id INT NOT NULL,
    student_id INT NOT NULL,
    score DECIMAL(5,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submit_time TIMESTAMP,
    remark VARCHAR(200),
    UNIQUE (homework_id, student_id),
    PRIMARY KEY (id),
    CONSTRAINT fk_hwr_hw FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE,
    CONSTRAINT fk_hwr_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exam_score (
    id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    exam_date DATE,
    UNIQUE (student_id, course_id),
    PRIMARY KEY (id),
    CONSTRAINT fk_exam_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT,
    username VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    target VARCHAR(100),
    detail VARCHAR(500),
    ip VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_created (created_at)
);
