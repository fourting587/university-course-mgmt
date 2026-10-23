package com.university.cms.common;

import com.university.cms.entity.Course;
import com.university.cms.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 课程归属校验：教师只能操作自己授课课程的数据，管理员不受限。
 */
@Component
public class CourseAccess {

    @Autowired
    private CourseMapper courseMapper;

    public boolean canAccess(LoginUser user, Integer courseId) {
        if (user == null || courseId == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        Course course = courseMapper.findById(courseId);
        return course != null
                && user.getRefId() != null
                && user.getRefId().equals(course.getTeacherId());
    }

    /** 教师返回自己的 refId 作为过滤条件；其他角色返回 null（不过滤）。 */
    public Integer teacherScope(LoginUser user) {
        return (user != null && user.isTeacher()) ? user.getRefId() : null;
    }
}
