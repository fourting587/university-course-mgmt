package com.university.cms.controller;

import com.university.cms.common.Constants;
import com.university.cms.common.CourseAccess;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.ExamScore;
import com.university.cms.mapper.ClazzMapper;
import com.university.cms.mapper.CourseMapper;
import com.university.cms.mapper.ExamScoreMapper;
import com.university.cms.mapper.StudentMapper;
import com.university.cms.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Controller
public class ScoreController {

    @Autowired
    private ScoreService scoreService;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ClazzMapper clazzMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ExamScoreMapper examScoreMapper;
    @Autowired
    private CourseAccess courseAccess;
    @Autowired
    private com.university.cms.service.AuditService auditService;

    private static final int PAGE_SIZE = 30;

    @GetMapping("/score/regular")
    public String regular(@RequestParam(required = false) Integer courseId,
                          @RequestParam(required = false) Integer clazzId,
                          @RequestParam(defaultValue = "1") int page,
                          HttpSession session, Model model) {
        return report("regular", courseId, clazzId, page, session, model);
    }

    @GetMapping("/score/total")
    public String total(@RequestParam(required = false) Integer courseId,
                        @RequestParam(required = false) Integer clazzId,
                        @RequestParam(defaultValue = "1") int page,
                        HttpSession session, Model model) {
        return report("total", courseId, clazzId, page, session, model);
    }

    private String report(String type, Integer courseId, Integer clazzId, int page,
                          HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer studentId = user.isStudent() ? user.getRefId() : null;

        // 教师只能查看自己授课课程的成绩，防止手动改 URL 越权
        if (courseId != null && user.isTeacher() && !courseAccess.canAccess(user, courseId)) {
            return "redirect:/home?error=forbidden";
        }

        if (user.isTeacher()) {
            model.addAttribute("courses", courseMapper.findByTeacherId(user.getRefId()));
        } else if (user.isStudent()) {
            model.addAttribute("courses", courseMapper.findByStudentId(user.getRefId(), null));
        } else {
            model.addAttribute("courses", courseMapper.findAll());
        }
        model.addAttribute("clazzList", clazzMapper.findAll());
        model.addAttribute("courseId", courseId);
        model.addAttribute("clazzId", clazzId);
        model.addAttribute("user", user);
        model.addAttribute("type", type);

        if (courseId != null) {
            List<Map<String, Object>> rows = scoreService.buildScoreReport(courseId, clazzId, studentId);
            int total = rows.size();
            int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            int current = Math.min(Math.max(1, page), totalPages);
            int from = Math.min((current - 1) * PAGE_SIZE, total);
            int to = Math.min(from + PAGE_SIZE, total);

            model.addAttribute("rows", rows.subList(from, to));
            model.addAttribute("totalRows", total);
            model.addAttribute("page", current);
            model.addAttribute("totalPages", totalPages);
        }
        return "score/report";
    }

    @GetMapping("/teach/exam")
    public String examPage(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        Integer teacherId = user.isTeacher() ? user.getRefId() : null;
        model.addAttribute("list", examScoreMapper.search(null, null, null, teacherId));
        model.addAttribute("courses", user.isTeacher()
                ? courseMapper.findByTeacherId(teacherId) : courseMapper.findAll());
        model.addAttribute("students", user.isTeacher()
                ? studentMapper.findByTeacherId(teacherId) : studentMapper.findAll());
        model.addAttribute("user", user);
        return "score/exam";
    }

    @PostMapping("/teach/exam/save")
    public String saveExam(ExamScore examScore, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (!courseAccess.canAccess(user, examScore.getCourseId())) {
            return "redirect:/home?error=forbidden";
        }
        if (examScore.getScore() == null
                || examScore.getScore().compareTo(java.math.BigDecimal.ZERO) < 0
                || examScore.getScore().compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "期末成绩必须在 0-100 之间");
        }
        ExamScore exist = examScoreMapper.findOne(examScore.getStudentId(), examScore.getCourseId());
        if (exist == null) {
            examScoreMapper.insert(examScore);
        } else {
            examScore.setId(exist.getId());
            examScoreMapper.update(examScore);
        }
        auditService.record(user, "EXAM_SAVE",
                "学生" + examScore.getStudentId() + " 课程" + examScore.getCourseId(),
                String.valueOf(examScore.getScore()));
        return "redirect:/teach/exam";
    }

    @PostMapping("/teach/exam/delete/{id}")
    public String deleteExam(@PathVariable Integer id, HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        ExamScore existing = examScoreMapper.findById(id);
        if (existing != null) {
            if (!courseAccess.canAccess(user, existing.getCourseId())) {
                return "redirect:/home?error=forbidden";
            }
            examScoreMapper.deleteById(id);
            auditService.record(user, "EXAM_DELETE", "期末成绩ID " + id);
        }
        return "redirect:/teach/exam";
    }

    /** 导出成绩为 CSV（Excel 可直接打开） */
    @GetMapping("/score/export")
    public void export(@RequestParam(required = false) Integer courseId,
                       @RequestParam(required = false) Integer clazzId,
                       @RequestParam(defaultValue = "total") String type,
                       HttpSession session, HttpServletResponse response) throws IOException {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (courseId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "请先选择课程");
            return;
        }
        if (user.isTeacher() && !courseAccess.canAccess(user, courseId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "无权导出该课程");
            return;
        }
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        List<Map<String, Object>> rows = scoreService.buildScoreReport(courseId, clazzId, studentId);
        auditService.record(user, "SCORE_EXPORT", "课程" + courseId, "共" + rows.size() + " 行");

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=score-report.csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.write('\uFEFF'); // BOM，保证 Excel 打开中文不乱码
            if ("regular".equals(type)) {
                writer.println("学号,姓名,班级,考勤分,表现分,实验分,作业分,平时成绩");
                for (Map<String, Object> row : rows) {
                    writer.println(csv(row.get("studentNo")) + "," + csv(row.get("studentName")) + ","
                            + csv(row.get("className")) + "," + csv(row.get("attendanceScore")) + ","
                            + csv(row.get("performanceScore")) + "," + csv(row.get("experimentScore")) + ","
                            + csv(row.get("homeworkScore")) + "," + csv(row.get("regularScore")));
                }
            } else {
                writer.println("学号,姓名,班级,考勤分,表现分,实验分,作业分,平时成绩,期末成绩,总成绩");
                for (Map<String, Object> row : rows) {
                    writer.println(csv(row.get("studentNo")) + "," + csv(row.get("studentName")) + ","
                            + csv(row.get("className")) + "," + csv(row.get("attendanceScore")) + ","
                            + csv(row.get("performanceScore")) + "," + csv(row.get("experimentScore")) + ","
                            + csv(row.get("homeworkScore")) + "," + csv(row.get("regularScore")) + ","
                            + csv(row.get("examScore")) + "," + csv(row.get("totalScore")));
                }
            }
        }
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
