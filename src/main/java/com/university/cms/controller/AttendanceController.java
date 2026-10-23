package com.university.cms.controller;

import com.university.cms.common.Constants;
import com.university.cms.common.CourseAccess;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.Attendance;
import com.university.cms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class AttendanceController {

    @Autowired
    private AttendanceMapper attendanceMapper;
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

    @GetMapping("/attendance")
    public String list(@RequestParam(required = false) Integer courseId,
                       @RequestParam(required = false) Integer clazzId,
                       @RequestParam(required = false) String semester,
                       @RequestParam(defaultValue = "1") int page,
                       HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        List<Attendance> all = attendanceMapper.search(studentId, courseId, clazzId, semester,
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
        fillFilters(model, user);
        model.addAttribute("courseId", courseId);
        model.addAttribute("clazzId", clazzId);
        model.addAttribute("semester", semester);
        model.addAttribute("user", user);
        return "attendance/list";
    }

    @GetMapping("/teach/attendance")
    public String teachPage(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        fillFilters(model, user);
        model.addAttribute("students", user.isTeacher()
                ? studentMapper.findByTeacherId(user.getRefId()) : studentMapper.findAll());
        model.addAttribute("user", user);
        return "attendance/form";
    }

    @PostMapping("/teach/attendance/save")
    public String save(Attendance attendance, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (!courseAccess.canAccess(user, attendance.getCourseId())) {
            return "redirect:/home?error=forbidden";
        }
        attendance.setRecorderId(user.getUserId());
        try {
            attendanceMapper.insert(attendance);
        } catch (Exception e) {
            // 同一学生、同一课程、同一日期只能有一条记录
            return "redirect:/attendance?error=duplicate";
        }
        auditService.record(user, "ATT_SAVE", "学生" + attendance.getStudentId() + " 课程" + attendance.getCourseId(),
                attendance.getStatus());
        return "redirect:/attendance";
    }

    /** 批量录入：同一课程、同一日期，多学生状态 */
    @PostMapping("/teach/attendance/batch")
    public String batch(@RequestParam Integer courseId,
                        @RequestParam String attendDate,
                        @RequestParam List<Integer> studentIds,
                        @RequestParam List<String> statuses,
                        HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (!courseAccess.canAccess(user, courseId)) {
            return "redirect:/home?error=forbidden";
        }
        int ok = 0, skip = 0;
        for (int i = 0; i < studentIds.size(); i++) {
            Attendance a = new Attendance();
            a.setCourseId(courseId);
            a.setStudentId(studentIds.get(i));
            a.setAttendDate(java.sql.Date.valueOf(attendDate));
            a.setStatus(statuses.get(i));
            a.setRecorderId(user.getUserId());
            try {
                attendanceMapper.insert(a);
                ok++;
            } catch (Exception e) {
                skip++; // 已存在或状态非法，跳过并计数
            }
        }
        auditService.record(user, "ATT_BATCH", "课程" + courseId,
                "成功" + ok + " 条，跳过" + skip + " 条");
        return "redirect:/attendance?courseId=" + courseId + "&ok=" + ok + "&skip=" + skip;
    }

    @PostMapping("/teach/attendance/delete/{id}")
    public String delete(@PathVariable Integer id, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Attendance existing = attendanceMapper.findById(id);
        if (existing != null) {
            if (!courseAccess.canAccess(user, existing.getCourseId())) {
                return "redirect:/home?error=forbidden";
            }
            attendanceMapper.deleteById(id);
            auditService.record(user, "ATT_DELETE", "考勤ID " + id);
        }
        return "redirect:/attendance";
    }

    private void fillFilters(Model model, LoginUser user) {
        if (user.isTeacher()) {
            model.addAttribute("courses", courseMapper.findByTeacherId(user.getRefId()));
        } else {
            model.addAttribute("courses", courseMapper.findAll());
        }
        model.addAttribute("clazzList", clazzMapper.findAll());
    }
}
