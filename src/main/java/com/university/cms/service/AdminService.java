package com.university.cms.service;

import com.university.cms.common.Constants;
import com.university.cms.common.PasswordUtil;
import com.university.cms.entity.*;
import com.university.cms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 管理端基础数据业务：学生/教师/班级/课程/评价规则的增删改查与账号联动。
 * Controller 只负责接收参数和返回视图。
 */
@Service
public class AdminService {

    @Autowired private StudentMapper studentMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private ClazzMapper clazzMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private EvalRuleMapper evalRuleMapper;
    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private AttendanceMapper attendanceMapper;
    @Autowired private PerformanceMapper performanceMapper;
    @Autowired private ExperimentMapper experimentMapper;
    @Autowired private HomeworkMapper homeworkMapper;
    @Autowired private ExamScoreMapper examScoreMapper;
    @Autowired private PasswordUtil passwordUtil;

    // ---------- 班级 ----------

    public List<Clazz> listClazzes() {
        return clazzMapper.findAll();
    }

    public void saveClazz(Clazz clazz) {
        if (clazz.getId() == null) {
            clazzMapper.insert(clazz);
        } else {
            clazzMapper.update(clazz);
        }
    }

    /** 班级下还有学生时返回 false，不删除 */
    @Transactional
    public boolean deleteClazz(Integer id) {
        if (studentMapper.countByClazz(id) > 0) {
            return false;
        }
        clazzMapper.deleteById(id);
        return true;
    }

    // ---------- 教师 ----------

    public List<Teacher> listTeachers() {
        return teacherMapper.findAll();
    }

    @Transactional
    public void saveTeacher(Teacher teacher) {
        if (teacher.getId() == null) {
            teacherMapper.insert(teacher);
            SysUser account = new SysUser();
            account.setUsername(teacher.getTeacherNo());
            account.setPassword(passwordUtil.encode(Constants.DEFAULT_PASSWORD));
            account.setRoleCode(Constants.ROLE_TEACHER);
            account.setRefId(teacher.getId());
            account.setRealName(teacher.getName());
            account.setEnabled(true);
            sysUserMapper.insert(account);
        } else {
            teacherMapper.update(teacher);
            sysUserMapper.updateRealNameByRef(Constants.ROLE_TEACHER, teacher.getId(), teacher.getName());
        }
    }

    /** 名下还有课程时返回 false，不删除 */
    @Transactional
    public boolean deleteTeacher(Integer id) {
        if (courseMapper.countByTeacher(id) > 0) {
            return false;
        }
        teacherMapper.deleteById(id);
        sysUserMapper.deleteByRef(Constants.ROLE_TEACHER, id);
        return true;
    }

    // ---------- 学生 ----------

    public List<Student> searchStudents(String keyword, Integer clazzId) {
        return studentMapper.search(keyword, clazzId);
    }

    @Transactional
    public void saveStudent(Student student) {
        if (student.getId() == null) {
            studentMapper.insert(student);
            SysUser account = new SysUser();
            account.setUsername(student.getStudentNo());
            account.setPassword(passwordUtil.encode(Constants.DEFAULT_PASSWORD));
            account.setRoleCode(Constants.ROLE_STUDENT);
            account.setRefId(student.getId());
            account.setRealName(student.getName());
            account.setEnabled(true);
            sysUserMapper.insert(account);
        } else {
            studentMapper.update(student);
            sysUserMapper.updateRealNameByRef(Constants.ROLE_STUDENT, student.getId(), student.getName());
        }
    }

    @Transactional
    public void deleteStudent(Integer id) {
        attendanceMapper.deleteByStudent(id);
        performanceMapper.deleteByStudent(id);
        experimentMapper.deleteRecordsByStudent(id);
        homeworkMapper.deleteRecordsByStudent(id);
        examScoreMapper.deleteByStudent(id);
        studentMapper.deleteById(id);
        sysUserMapper.deleteByRef(Constants.ROLE_STUDENT, id);
    }

    // ---------- 课程 ----------

    public List<Course> listCourses() {
        return courseMapper.findAll();
    }

    /** 课程列表，并回填每门课绑定的班级（编辑回显用） */
    public List<Course> listCoursesWithClasses() {
        List<Course> courses = courseMapper.findAll();
        for (Course course : courses) {
            course.setClazzIds(courseMapper.findClazzIdsByCourse(course.getId()));
        }
        return courses;
    }

    @Transactional
    public void saveCourse(Course course, List<Integer> clazzIds) {
        if (course.getId() == null) {
            courseMapper.insert(course);
        } else {
            courseMapper.update(course);
            courseMapper.clearClasses(course.getId());
        }
        if (clazzIds != null) {
            for (Integer clazzId : clazzIds) {
                courseMapper.bindClass(course.getId(), clazzId);
            }
        }
    }

    @Transactional
    public void deleteCourse(Integer id) {
        experimentMapper.deleteRecordsByCourse(id);
        experimentMapper.deleteByCourse(id);
        homeworkMapper.deleteRecordsByCourse(id);
        homeworkMapper.deleteByCourse(id);
        attendanceMapper.deleteByCourse(id);
        performanceMapper.deleteByCourse(id);
        examScoreMapper.deleteByCourse(id);
        evalRuleMapper.deleteByCourse(id);
        courseMapper.clearClasses(id);
        courseMapper.deleteById(id);
    }

    // ---------- 评价规则 ----------

    public List<EvalRule> listRules() {
        return evalRuleMapper.findAll();
    }

    public void saveRule(EvalRule rule) {
        BigDecimal[] values = {
                rule.getAttendanceWeight(), rule.getPerformanceWeight(),
                rule.getExperimentWeight(), rule.getHomeworkWeight(),
                rule.getRegularRatio(), rule.getExamRatio()
        };
        BigDecimal hundred = new BigDecimal("100");
        for (BigDecimal value : values) {
            if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(hundred) > 0) {
                throw new ResponseStatusException(BAD_REQUEST, "权重与占比必须在 0-100 之间");
            }
        }
        if (rule.getId() == null) {
            evalRuleMapper.insert(rule);
        } else {
            evalRuleMapper.update(rule);
        }
    }

    public void deleteRule(Integer id) {
        evalRuleMapper.deleteById(id);
    }

    // ---------- 账号：重置密码 / 启用禁用 ----------

    @Transactional
    public void resetTeacherPassword(Integer id) {
        resetPassword(Constants.ROLE_TEACHER, id);
    }

    @Transactional
    public void resetStudentPassword(Integer id) {
        resetPassword(Constants.ROLE_STUDENT, id);
    }

    @Transactional
    public void toggleTeacher(Integer id) {
        toggle(Constants.ROLE_TEACHER, id);
    }

    @Transactional
    public void toggleStudent(Integer id) {
        toggle(Constants.ROLE_STUDENT, id);
    }

    private void resetPassword(String roleCode, Integer refId) {
        SysUser account = sysUserMapper.findByRef(roleCode, refId);
        if (account != null) {
            account.setPassword(passwordUtil.encode(Constants.DEFAULT_PASSWORD));
            sysUserMapper.update(account);
        }
    }

    private void toggle(String roleCode, Integer refId) {
        SysUser account = sysUserMapper.findByRef(roleCode, refId);
        if (account != null) {
            boolean enable = account.getEnabled() == null || !account.getEnabled();
            sysUserMapper.updateEnabled(account.getId(), enable);
        }
    }
}
