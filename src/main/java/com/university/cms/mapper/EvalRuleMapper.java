package com.university.cms.mapper;

import com.university.cms.entity.EvalRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvalRuleMapper {

    @Select("SELECT r.*, c.course_name AS courseName FROM eval_rule r LEFT JOIN course c ON r.course_id = c.id ORDER BY r.id")
    List<EvalRule> findAll();

    @Select("SELECT r.*, c.course_name AS courseName FROM eval_rule r LEFT JOIN course c ON r.course_id = c.id WHERE r.course_id = #{courseId}")
    EvalRule findByCourseId(Integer courseId);

    @Select("SELECT * FROM eval_rule WHERE id = #{id}")
    EvalRule findById(Integer id);

    @Insert("INSERT INTO eval_rule(course_id, attendance_weight, performance_weight, experiment_weight, homework_weight, regular_ratio, exam_ratio) " +
            "VALUES(#{courseId}, #{attendanceWeight}, #{performanceWeight}, #{experimentWeight}, #{homeworkWeight}, #{regularRatio}, #{examRatio})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EvalRule rule);

    @Update("UPDATE eval_rule SET attendance_weight=#{attendanceWeight}, performance_weight=#{performanceWeight}, " +
            "experiment_weight=#{experimentWeight}, homework_weight=#{homeworkWeight}, " +
            "regular_ratio=#{regularRatio}, exam_ratio=#{examRatio} WHERE id=#{id}")
    int update(EvalRule rule);

    @Delete("DELETE FROM eval_rule WHERE id = #{id}")
    int deleteById(Integer id);

    @Delete("DELETE FROM eval_rule WHERE course_id = #{courseId}")
    int deleteByCourse(Integer courseId);
}
