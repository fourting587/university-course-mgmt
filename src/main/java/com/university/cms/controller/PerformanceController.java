package com.university.cms.controller;

import com.university.cms.common.Constants;
import com.university.cms.common.CourseAccess;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.Performance;
import com.university.cms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class PerformanceController {

    @Autowired
    private PerformanceMapper performanceMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ClazzMapper clazzMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseAccess courseAccess;
    @Autowired
    private com.university.cms.service.AuditService auditService;

    private static final int PAGE_SIZE = 20;

    @GetMapping("/performance")
    public String list(@RequestParam(required = false) Integer courseId,
                       @RequestParam(required = false) Integer clazzId,
                       @RequestParam(defaultValue = "1") int page,
                       HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        java.util.List<Performance> all = performanceMapper.search(studentId, courseId, clazzId,
                courseAccess.teacherScope(user));
        int total = all.size();
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(Math.max(1, page), totalPages);
        int from = Math.min((current - 1) * PAGE_SIZE, total);
        int to = Math.min(from + PAGE_SIZE, total);

        model.addAttribute("list", all.subList(from, to));
        model.addAttribute("totalRows", total);
        model.addAttribute("page", current);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("courses", user.isTeacher()
                ? courseMapper.findByTeacherId(user.getRefId()) : courseMapper.findAll());
        model.addAttribute("clazzList", clazzMapper.findAll());
        model.addAttribute("courseId", courseId);
        model.addAttribute("clazzId", clazzId);
        model.addAttribute("user", user);
        return "performance/list";
    }

    @GetMapping("/teach/performance")
    public String form(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        model.addAttribute("courses", user.isTeacher()
                ? courseMapper.findByTeacherId(user.getRefId()) : courseMapper.findAll());
        model.addAttribute("students", user.isTeacher()
                ? studentMapper.findByTeacherId(user.getRefId()) : studentMapper.findAll());
        model.addAttribute("user", user);
        return "performance/form";
    }

    @PostMapping("/teach/performance/save")
    public String save(Performance performance, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (!courseAccess.canAccess(user, performance.getCourseId())) {
            return "redirect:/home?error=forbidden";
        }
        if (performance.getScore() == null
                || performance.getScore().compareTo(java.math.BigDecimal.ZERO) < 0
                || performance.getScore().compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "课堂表现分数必须在 0-100 之间");
        }
        performance.setRecorderId(user.getUserId());
        if (performance.getId() == null) {
            performanceMapper.insert(performance);
        } else {
            performanceMapper.update(performance);
        }
        auditService.record(user, "PERF_SAVE", "学生" + performance.getStudentId() + " 课程" + performance.getCourseId(),
                String.valueOf(performance.getScore()));
        return "redirect:/performance";
    }

    @PostMapping("/teach/performance/delete/{id}")
    public String delete(@PathVariable Integer id, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Performance existing = performanceMapper.findById(id);
        if (existing != null) {
            if (!courseAccess.canAccess(user, existing.getCourseId())) {
                return "redirect:/home?error=forbidden";
            }
            performanceMapper.deleteById(id);
            auditService.record(user, "PERF_DELETE", "表现ID " + id);
        }
        return "redirect:/performance";
    }
}
