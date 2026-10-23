package com.university.cms.controller;

import com.university.cms.common.Constants;
import com.university.cms.common.CourseAccess;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.Homework;
import com.university.cms.entity.HomeworkRecord;
import com.university.cms.mapper.CourseMapper;
import com.university.cms.mapper.HomeworkMapper;
import com.university.cms.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Date;

@Controller
public class HomeworkController {

    @Autowired
    private HomeworkMapper homeworkMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseAccess courseAccess;
    @Autowired
    private com.university.cms.service.AuditService auditService;

    private static final int PAGE_SIZE = 20;

    @GetMapping("/homework")
    public String list(@RequestParam(required = false) Integer courseId,
                       @RequestParam(defaultValue = "1") int page,
                       HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        java.util.List<HomeworkRecord> all = homeworkMapper.searchRecords(studentId, courseId, null,
                courseAccess.teacherScope(user));
        int total = all.size();
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(Math.max(1, page), totalPages);
        int from = Math.min((current - 1) * PAGE_SIZE, total);
        int to = Math.min(from + PAGE_SIZE, total);

        model.addAttribute("records", all.subList(from, to));
        model.addAttribute("totalRows", total);
        model.addAttribute("page", current);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("homeworks", courseId == null
                ? (user.isTeacher() ? homeworkMapper.findByTeacherId(user.getRefId()) : homeworkMapper.findAll())
                : homeworkMapper.findByCourseId(courseId));
        model.addAttribute("courses", user.isTeacher()
                ? courseMapper.findByTeacherId(user.getRefId()) : courseMapper.findAll());
        model.addAttribute("courseId", courseId);
        model.addAttribute("user", user);
        return "homework/list";
    }

    @GetMapping("/teach/homework")
    public String manage(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        model.addAttribute("courses", user.isTeacher()
                ? courseMapper.findByTeacherId(user.getRefId()) : courseMapper.findAll());
        model.addAttribute("homeworks", user.isTeacher()
                ? homeworkMapper.findByTeacherId(user.getRefId()) : homeworkMapper.findAll());
        model.addAttribute("students", user.isTeacher()
                ? studentMapper.findByTeacherId(user.getRefId()) : studentMapper.findAll());
        model.addAttribute("user", user);
        return "homework/form";
    }

    @PostMapping("/teach/homework/save")
    public String saveHw(Homework homework, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (!courseAccess.canAccess(user, homework.getCourseId())) {
            return "redirect:/home?error=forbidden";
        }
        if (homework.getFullScore() == null) {
            homework.setFullScore(new java.math.BigDecimal("100"));
        }
        if (homework.getFullScore().compareTo(java.math.BigDecimal.ZERO) <= 0
                || homework.getFullScore().compareTo(new java.math.BigDecimal("1000")) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "作业满分必须在 0-1000 之间");
        }
        boolean isNew = homework.getId() == null;
        if (isNew) {
            homeworkMapper.insert(homework);
        } else {
            homeworkMapper.update(homework);
        }
        auditService.record(user, isNew ? "HW_ADD" : "HW_UPDATE",
                "作业 " + homework.getTitle(), "课程" + homework.getCourseId());
        return "redirect:/teach/homework";
    }

    @PostMapping("/teach/homework/record")
    public String saveRecord(HomeworkRecord record, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Homework homework = homeworkMapper.findById(record.getHomeworkId());
        if (homework == null || !courseAccess.canAccess(user, homework.getCourseId())) {
            return "redirect:/home?error=forbidden";
        }
        if (record.getScore() != null
                && (record.getScore().compareTo(java.math.BigDecimal.ZERO) < 0
                    || record.getScore().compareTo(new java.math.BigDecimal("100")) > 0)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "作业得分必须在 0-100 之间");
        }
        if (record.getSubmitTime() == null) {
            record.setSubmitTime(new Date());
        }
        if (record.getStatus() == null || record.getStatus().isEmpty()) {
            record.setStatus(record.getScore() != null ? "GRADED" : "SUBMITTED");
        }
        if (record.getId() == null) {
            homeworkMapper.insertRecord(record);
        } else {
            homeworkMapper.updateRecord(record);
        }
        auditService.record(user, "HW_RECORD_SAVE",
                "作业" + record.getHomeworkId() + " 学生" + record.getStudentId(), record.getStatus());
        return "redirect:/homework";
    }

    @PostMapping("/teach/homework/delete/{id}")
    public String deleteHw(@PathVariable Integer id, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Homework homework = homeworkMapper.findById(id);
        if (homework != null) {
            if (!courseAccess.canAccess(user, homework.getCourseId())) {
                return "redirect:/home?error=forbidden";
            }
            homeworkMapper.deleteById(id);
            auditService.record(user, "HW_DELETE", "作业ID " + id);
        }
        return "redirect:/teach/homework";
    }

    @PostMapping("/teach/homework/record/delete/{id}")
    public String deleteRecord(@PathVariable Integer id, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer courseId = homeworkMapper.findCourseIdByRecordId(id);
        if (courseId != null && !courseAccess.canAccess(user, courseId)) {
            return "redirect:/home?error=forbidden";
        }
        homeworkMapper.deleteRecord(id);
        auditService.record(user, "HW_RECORD_DELETE", "作业记录ID " + id);
        return "redirect:/homework";
    }
}
