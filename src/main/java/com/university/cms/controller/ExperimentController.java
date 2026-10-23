package com.university.cms.controller;

import com.university.cms.common.Constants;
import com.university.cms.common.CourseAccess;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.Experiment;
import com.university.cms.entity.ExperimentRecord;
import com.university.cms.mapper.CourseMapper;
import com.university.cms.mapper.ExperimentMapper;
import com.university.cms.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Date;

@Controller
public class ExperimentController {

    @Autowired
    private ExperimentMapper experimentMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseAccess courseAccess;
    @Autowired
    private com.university.cms.service.AuditService auditService;

    private static final int PAGE_SIZE = 20;

    @GetMapping("/experiment")
    public String list(@RequestParam(required = false) Integer courseId,
                       @RequestParam(defaultValue = "1") int page,
                       HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        java.util.List<ExperimentRecord> all = experimentMapper.searchRecords(studentId, courseId, null,
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
        model.addAttribute("experiments", courseId == null
                ? (user.isTeacher() ? experimentMapper.findByTeacherId(user.getRefId()) : experimentMapper.findAll())
                : experimentMapper.findByCourseId(courseId));
        model.addAttribute("courses", user.isTeacher()
                ? courseMapper.findByTeacherId(user.getRefId()) : courseMapper.findAll());
        model.addAttribute("courseId", courseId);
        model.addAttribute("user", user);
        return "experiment/list";
    }

    @GetMapping("/teach/experiment")
    public String manage(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        model.addAttribute("courses", user.isTeacher()
                ? courseMapper.findByTeacherId(user.getRefId()) : courseMapper.findAll());
        model.addAttribute("experiments", user.isTeacher()
                ? experimentMapper.findByTeacherId(user.getRefId()) : experimentMapper.findAll());
        model.addAttribute("students", user.isTeacher()
                ? studentMapper.findByTeacherId(user.getRefId()) : studentMapper.findAll());
        model.addAttribute("user", user);
        return "experiment/form";
    }

    @PostMapping("/teach/experiment/save")
    public String saveExp(Experiment experiment, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (!courseAccess.canAccess(user, experiment.getCourseId())) {
            return "redirect:/home?error=forbidden";
        }
        if (experiment.getFullScore() == null) {
            experiment.setFullScore(new java.math.BigDecimal("100"));
        }
        if (experiment.getFullScore().compareTo(java.math.BigDecimal.ZERO) <= 0
                || experiment.getFullScore().compareTo(new java.math.BigDecimal("1000")) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "实验满分必须在 0-1000 之间");
        }
        boolean isNew = experiment.getId() == null;
        if (isNew) {
            experimentMapper.insert(experiment);
        } else {
            experimentMapper.update(experiment);
        }
        auditService.record(user, isNew ? "EXP_ADD" : "EXP_UPDATE",
                "实验 " + experiment.getTitle(), "课程" + experiment.getCourseId());
        return "redirect:/teach/experiment";
    }

    @PostMapping("/teach/experiment/record")
    public String saveRecord(ExperimentRecord record, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Experiment experiment = experimentMapper.findById(record.getExperimentId());
        if (experiment == null || !courseAccess.canAccess(user, experiment.getCourseId())) {
            return "redirect:/home?error=forbidden";
        }
        if (record.getCompletion() == null
                || record.getCompletion().compareTo(java.math.BigDecimal.ZERO) < 0
                || record.getCompletion().compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "完成度必须在 0-100 之间");
        }
        if (record.getScore() != null
                && (record.getScore().compareTo(java.math.BigDecimal.ZERO) < 0
                    || record.getScore().compareTo(new java.math.BigDecimal("100")) > 0)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "实验得分必须在 0-100 之间");
        }
        if (record.getSubmitTime() == null) {
            record.setSubmitTime(new Date());
        }
        if (record.getId() == null) {
            experimentMapper.insertRecord(record);
        } else {
            experimentMapper.updateRecord(record);
        }
        auditService.record(user, "EXP_RECORD_SAVE",
                "实验" + record.getExperimentId() + " 学生" + record.getStudentId(), null);
        return "redirect:/experiment";
    }

    @PostMapping("/teach/experiment/delete/{id}")
    public String deleteExp(@PathVariable Integer id, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Experiment experiment = experimentMapper.findById(id);
        if (experiment != null) {
            if (!courseAccess.canAccess(user, experiment.getCourseId())) {
                return "redirect:/home?error=forbidden";
            }
            experimentMapper.deleteById(id);
            auditService.record(user, "EXP_DELETE", "实验ID " + id);
        }
        return "redirect:/teach/experiment";
    }

    @PostMapping("/teach/experiment/record/delete/{id}")
    public String deleteRecord(@PathVariable Integer id, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer courseId = experimentMapper.findCourseIdByRecordId(id);
        if (courseId != null && !courseAccess.canAccess(user, courseId)) {
            return "redirect:/home?error=forbidden";
        }
        experimentMapper.deleteRecord(id);
        auditService.record(user, "EXP_RECORD_DELETE", "实验记录ID " + id);
        return "redirect:/experiment";
    }
}
