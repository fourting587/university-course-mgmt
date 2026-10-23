package com.university.cms.controller;

import com.university.cms.common.Constants;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.*;
import com.university.cms.service.AdminService;
import com.university.cms.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * 管理员：学生/教师/班级/课程/评价规则 基础数据 CRUD。
 * 业务逻辑在 AdminService，这里只做参数接收、分页、审计和视图返回。
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int STUDENT_PAGE_SIZE = 20;

    @Autowired
    private AdminService adminService;
    @Autowired
    private AuditService auditService;

    // ---------- 班级 ----------

    @GetMapping("/clazz")
    public String clazzList(Model model) {
        model.addAttribute("list", adminService.listClazzes());
        return "admin/clazz";
    }

    @PostMapping("/clazz/save")
    public String clazzSave(Clazz clazz, HttpSession session) {
        boolean isNew = clazz.getId() == null;
        adminService.saveClazz(clazz);
        auditService.record(user(session), isNew ? "CLAZZ_ADD" : "CLAZZ_UPDATE",
                "班级 " + clazz.getClassNo(), clazz.getClassName());
        return "redirect:/admin/clazz";
    }

    @PostMapping("/clazz/delete/{id}")
    public String clazzDelete(@PathVariable Integer id, HttpSession session) {
        if (!adminService.deleteClazz(id)) {
            return "redirect:/admin/clazz?error=hasStudents";
        }
        auditService.record(user(session), "CLAZZ_DELETE", "班级ID " + id);
        return "redirect:/admin/clazz";
    }

    // ---------- 教师 ----------

    @GetMapping("/teacher")
    public String teacherList(Model model) {
        model.addAttribute("list", adminService.listTeachers());
        return "admin/teacher";
    }

    @PostMapping("/teacher/save")
    public String teacherSave(Teacher teacher, HttpSession session) {
        boolean isNew = teacher.getId() == null;
        adminService.saveTeacher(teacher);
        auditService.record(user(session), isNew ? "TEACHER_ADD" : "TEACHER_UPDATE",
                "教师 " + teacher.getTeacherNo(), teacher.getName());
        return "redirect:/admin/teacher";
    }

    @PostMapping("/teacher/delete/{id}")
    public String teacherDelete(@PathVariable Integer id, HttpSession session) {
        if (!adminService.deleteTeacher(id)) {
            return "redirect:/admin/teacher?error=hasCourses";
        }
        auditService.record(user(session), "TEACHER_DELETE", "教师ID " + id);
        return "redirect:/admin/teacher";
    }

    @PostMapping("/teacher/reset/{id}")
    public String teacherResetPassword(@PathVariable Integer id, HttpSession session) {
        adminService.resetTeacherPassword(id);
        auditService.record(user(session), "TEACHER_RESET_PWD", "教师ID " + id);
        return "redirect:/admin/teacher?msg=pwdReset";
    }

    @PostMapping("/teacher/toggle/{id}")
    public String teacherToggle(@PathVariable Integer id, HttpSession session) {
        adminService.toggleTeacher(id);
        auditService.record(user(session), "TEACHER_TOGGLE", "教师ID " + id);
        return "redirect:/admin/teacher?msg=toggled";
    }

    // ---------- 学生 ----------

    @GetMapping("/student")
    public String studentList(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Integer clazzId,
                              @RequestParam(defaultValue = "1") int page,
                              Model model) {
        List<Student> all = adminService.searchStudents(keyword, clazzId);
        int total = all.size();
        int totalPages = Math.max(1, (total + STUDENT_PAGE_SIZE - 1) / STUDENT_PAGE_SIZE);
        int current = Math.min(Math.max(1, page), totalPages);
        int from = Math.min((current - 1) * STUDENT_PAGE_SIZE, total);
        int to = Math.min(from + STUDENT_PAGE_SIZE, total);

        model.addAttribute("list", all.subList(from, to));
        model.addAttribute("totalRows", total);
        model.addAttribute("page", current);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("clazzList", adminService.listClazzes());
        model.addAttribute("keyword", keyword);
        model.addAttribute("clazzId", clazzId);
        return "admin/student";
    }

    @PostMapping("/student/save")
    public String studentSave(Student student, HttpSession session) {
        boolean isNew = student.getId() == null;
        adminService.saveStudent(student);
        auditService.record(user(session), isNew ? "STUDENT_ADD" : "STUDENT_UPDATE",
                "学生 " + student.getStudentNo(), student.getName());
        return "redirect:/admin/student";
    }

    @PostMapping("/student/delete/{id}")
    public String studentDelete(@PathVariable Integer id, HttpSession session) {
        adminService.deleteStudent(id);
        auditService.record(user(session), "STUDENT_DELETE", "学生ID " + id);
        return "redirect:/admin/student";
    }

    @PostMapping("/student/reset/{id}")
    public String studentResetPassword(@PathVariable Integer id, HttpSession session) {
        adminService.resetStudentPassword(id);
        auditService.record(user(session), "STUDENT_RESET_PWD", "学生ID " + id);
        return "redirect:/admin/student?msg=pwdReset";
    }

    @PostMapping("/student/toggle/{id}")
    public String studentToggle(@PathVariable Integer id, HttpSession session) {
        adminService.toggleStudent(id);
        auditService.record(user(session), "STUDENT_TOGGLE", "学生ID " + id);
        return "redirect:/admin/student?msg=toggled";
    }

    // ---------- 课程 ----------

    @GetMapping("/course")
    public String courseList(Model model) {
        model.addAttribute("list", adminService.listCoursesWithClasses());
        model.addAttribute("teachers", adminService.listTeachers());
        model.addAttribute("clazzList", adminService.listClazzes());
        return "admin/course";
    }

    @PostMapping("/course/save")
    public String courseSave(Course course, @RequestParam(required = false) List<Integer> clazzIds,
                             HttpSession session) {
        boolean isNew = course.getId() == null;
        adminService.saveCourse(course, clazzIds);
        auditService.record(user(session), isNew ? "COURSE_ADD" : "COURSE_UPDATE",
                "课程 " + course.getCourseNo(), course.getCourseName());
        return "redirect:/admin/course";
    }

    @PostMapping("/course/delete/{id}")
    public String courseDelete(@PathVariable Integer id, HttpSession session) {
        adminService.deleteCourse(id);
        auditService.record(user(session), "COURSE_DELETE", "课程ID " + id);
        return "redirect:/admin/course";
    }

    // ---------- 评价规则 ----------

    @GetMapping("/rule")
    public String ruleList(Model model) {
        model.addAttribute("list", adminService.listRules());
        model.addAttribute("courses", adminService.listCourses());
        return "admin/rule";
    }

    @PostMapping("/rule/save")
    public String ruleSave(EvalRule rule, HttpSession session) {
        adminService.saveRule(rule);
        auditService.record(user(session), "RULE_SAVE", "课程ID " + rule.getCourseId());
        return "redirect:/admin/rule";
    }

    @PostMapping("/rule/delete/{id}")
    public String ruleDelete(@PathVariable Integer id, HttpSession session) {
        adminService.deleteRule(id);
        auditService.record(user(session), "RULE_DELETE", "规则ID " + id);
        return "redirect:/admin/rule";
    }

    @ModelAttribute("user")
    public LoginUser currentUser(HttpSession session) {
        return user(session);
    }

    private LoginUser user(HttpSession session) {
        return (LoginUser) session.getAttribute(Constants.SESSION_USER);
    }
}
