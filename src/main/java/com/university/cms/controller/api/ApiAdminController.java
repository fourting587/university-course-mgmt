package com.university.cms.controller.api;

import com.university.cms.common.Constants;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.*;
import com.university.cms.mapper.EvalRuleMapper;
import com.university.cms.mapper.TeacherMapper;
import com.university.cms.service.AdminService;
import com.university.cms.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 前后端分离版的管理端写接口：学生/教师/班级/课程/评价规则 CRUD。
 */
@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    @Autowired private AdminService adminService;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private EvalRuleMapper evalRuleMapper;
    @Autowired private AuditService auditService;

    // ---------- 下拉数据 ----------

    @GetMapping("/teachers")
    public ResponseEntity<?> teachers(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(teacherMapper.findAll());
    }

    @GetMapping("/rules")
    public ResponseEntity<?> rules(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(evalRuleMapper.findAll());
    }

    @GetMapping("/courses")
    public ResponseEntity<?> courses(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(adminService.listCoursesWithClasses());
    }

    // ---------- 班级 ----------

    @PostMapping("/classes")
    public ResponseEntity<?> saveClass(@RequestBody Clazz clazz, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        boolean isNew = clazz.getId() == null;
        adminService.saveClazz(clazz);
        auditService.record(user(session), isNew ? "CLAZZ_ADD" : "CLAZZ_UPDATE", "班级 " + clazz.getClassNo());
        return ok();
    }

    @DeleteMapping("/classes/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (!adminService.deleteClazz(id)) {
            return error(HttpStatus.CONFLICT, "该班级下还有学生，不能删除");
        }
        auditService.record(user(session), "CLAZZ_DELETE", "班级ID " + id);
        return ok();
    }

    // ---------- 教师 ----------

    @PostMapping("/teachers")
    public ResponseEntity<?> saveTeacher(@RequestBody Teacher teacher, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        boolean isNew = teacher.getId() == null;
        adminService.saveTeacher(teacher);
        auditService.record(user(session), isNew ? "TEACHER_ADD" : "TEACHER_UPDATE", "教师 " + teacher.getTeacherNo());
        return ok();
    }

    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<?> deleteTeacher(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (!adminService.deleteTeacher(id)) {
            return error(HttpStatus.CONFLICT, "该教师名下还有课程，不能删除");
        }
        auditService.record(user(session), "TEACHER_DELETE", "教师ID " + id);
        return ok();
    }

    @PostMapping("/teachers/{id}/reset")
    public ResponseEntity<?> resetTeacher(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.resetTeacherPassword(id);
        auditService.record(user(session), "TEACHER_RESET_PWD", "教师ID " + id);
        return ok();
    }

    @PostMapping("/teachers/{id}/toggle")
    public ResponseEntity<?> toggleTeacher(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.toggleTeacher(id);
        auditService.record(user(session), "TEACHER_TOGGLE", "教师ID " + id);
        return ok();
    }

    // ---------- 学生 ----------

    @PostMapping("/students")
    public ResponseEntity<?> saveStudent(@RequestBody Student student, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        boolean isNew = student.getId() == null;
        adminService.saveStudent(student);
        auditService.record(user(session), isNew ? "STUDENT_ADD" : "STUDENT_UPDATE", "学生 " + student.getStudentNo());
        return ok();
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.deleteStudent(id);
        auditService.record(user(session), "STUDENT_DELETE", "学生ID " + id);
        return ok();
    }

    @PostMapping("/students/{id}/reset")
    public ResponseEntity<?> resetStudent(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.resetStudentPassword(id);
        auditService.record(user(session), "STUDENT_RESET_PWD", "学生ID " + id);
        return ok();
    }

    @PostMapping("/students/{id}/toggle")
    public ResponseEntity<?> toggleStudent(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.toggleStudent(id);
        auditService.record(user(session), "STUDENT_TOGGLE", "学生ID " + id);
        return ok();
    }

    // ---------- 课程 ----------

    @PostMapping("/courses")
    public ResponseEntity<?> saveCourse(@RequestBody Course course, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        boolean isNew = course.getId() == null;
        adminService.saveCourse(course, course.getClazzIds());
        auditService.record(user(session), isNew ? "COURSE_ADD" : "COURSE_UPDATE", "课程 " + course.getCourseNo());
        return ok();
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.deleteCourse(id);
        auditService.record(user(session), "COURSE_DELETE", "课程ID " + id);
        return ok();
    }

    // ---------- 评价规则 ----------

    @PostMapping("/rules")
    public ResponseEntity<?> saveRule(@RequestBody EvalRule rule, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.saveRule(rule);
        auditService.record(user(session), "RULE_SAVE", "课程ID " + rule.getCourseId());
        return ok();
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable Integer id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        adminService.deleteRule(id);
        auditService.record(user(session), "RULE_DELETE", "规则ID " + id);
        return ok();
    }

    private boolean isAdmin(HttpSession session) {
        LoginUser user = user(session);
        return user != null && user.isAdmin();
    }

    private LoginUser user(HttpSession session) {
        return (LoginUser) session.getAttribute(Constants.SESSION_USER);
    }

    private ResponseEntity<Map<String, Object>> ok() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "仅管理员可操作"));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
