-- 为已有数据库补充外键约束（只需执行一次；新建库直接用 schema-mysql.sql 即可）
-- 执行前请确认没有孤儿数据：删除班级前先清空学生、删除教师前先转走课程。
-- 重复执行会提示约束已存在，可忽略。

ALTER TABLE student
    ADD CONSTRAINT fk_student_clazz FOREIGN KEY (clazz_id) REFERENCES clazz(id) ON DELETE RESTRICT;

ALTER TABLE course
    ADD CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE RESTRICT;

ALTER TABLE course_class
    ADD CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_cc_clazz FOREIGN KEY (clazz_id) REFERENCES clazz(id) ON DELETE CASCADE;

ALTER TABLE eval_rule
    ADD CONSTRAINT fk_rule_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE;

ALTER TABLE attendance
    ADD CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_att_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE;

ALTER TABLE performance
    ADD CONSTRAINT fk_perf_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_perf_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE;

ALTER TABLE experiment
    ADD CONSTRAINT fk_exp_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE;

ALTER TABLE experiment_record
    ADD CONSTRAINT fk_expr_exp FOREIGN KEY (experiment_id) REFERENCES experiment(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_expr_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;

ALTER TABLE homework
    ADD CONSTRAINT fk_hw_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE;

ALTER TABLE homework_record
    ADD CONSTRAINT fk_hwr_hw FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_hwr_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;

ALTER TABLE exam_score
    ADD CONSTRAINT fk_exam_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_exam_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE;
