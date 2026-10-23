package com.university.cms.controller.api;

import com.university.cms.common.Constants;
import com.university.cms.common.CourseAccess;
import com.university.cms.common.LoginUser;
import com.university.cms.mapper.*;
import com.university.cms.service.ScoreService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前后端分离后的只读查询接口。权限沿用会话角色。
 */
@RestController
@RequestMapping("/api")
public class ApiQueryController {

    @Autowired private StudentMapper studentMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private ClazzMapper clazzMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private AttendanceMapper attendanceMapper;
    @Autowired private PerformanceMapper performanceMapper;
    @Autowired private ExperimentMapper experimentMapper;
    @Autowired private HomeworkMapper homeworkMapper;
    @Autowired private AuditMapper auditMapper;
    @Autowired private ScoreService scoreService;
    @Autowired private CourseAccess courseAccess;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", user.getRoleCode());

        if (user.isAdmin()) {
            data.put("students", studentMapper.findAll().size());
            data.put("teachers", teacherMapper.findAll().size());
            data.put("classes", clazzMapper.findAll().size());
            data.put("courses", courseMapper.findAll().size());
            data.put("attendance", attendanceMapper.search(null, null, null, null, null).size());
        } else if (user.isTeacher()) {
            data.put("courses", courseMapper.findByTeacherId(user.getRefId()).size());
            data.put("students", studentMapper.findByTeacherId(user.getRefId()).size());
        } else {
            data.put("courses", courseMapper.findByStudentId(user.getRefId(), null).size());
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/courses")
    public ResponseEntity<?> courses(HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        if (user.isAdmin()) return ResponseEntity.ok(courseMapper.findAll());
        if (user.isTeacher()) return ResponseEntity.ok(courseMapper.findByTeacherId(user.getRefId()));
        return ResponseEntity.ok(courseMapper.findByStudentId(user.getRefId(), null));
    }

    @GetMapping("/classes")
    public ResponseEntity<?> classes(HttpSession session) {
        if (current(session) == null) return unauthorized();
        return ResponseEntity.ok(clazzMapper.findAll());
    }

    @GetMapping("/students")
    public ResponseEntity<?> students(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer clazzId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        if (!user.isAdmin()) return forbidden();
        return ResponseEntity.ok(page(studentMapper.search(keyword, clazzId), page, size));
    }

    @GetMapping("/attendance")
    public ResponseEntity<?> attendance(@RequestParam(required = false) Integer courseId,
                                        @RequestParam(required = false) Integer clazzId,
                                        @RequestParam(required = false) String semester,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        List<?> rows = attendanceMapper.search(studentId, courseId, clazzId, semester, courseAccess.teacherScope(user));
        return ResponseEntity.ok(page(rows, page, size));
    }

    @GetMapping("/performance")
    public ResponseEntity<?> performance(@RequestParam(required = false) Integer courseId,
                                         @RequestParam(required = false) Integer clazzId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        List<?> rows = performanceMapper.search(studentId, courseId, clazzId, courseAccess.teacherScope(user));
        return ResponseEntity.ok(page(rows, page, size));
    }

    @GetMapping("/experiments")
    public ResponseEntity<?> experiments(@RequestParam(required = false) Integer courseId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        List<?> rows = experimentMapper.searchRecords(studentId, courseId, null, courseAccess.teacherScope(user));
        return ResponseEntity.ok(page(rows, page, size));
    }

    @GetMapping("/homework")
    public ResponseEntity<?> homework(@RequestParam(required = false) Integer courseId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        Integer studentId = user.isStudent() ? user.getRefId() : null;
        List<?> rows = homeworkMapper.searchRecords(studentId, courseId, null, courseAccess.teacherScope(user));
        return ResponseEntity.ok(page(rows, page, size));
    }

    @GetMapping("/scores")
    public ResponseEntity<?> scores(@RequestParam(required = false) Integer courseId,
                                    @RequestParam(required = false) Integer clazzId,
                                    @RequestParam(defaultValue = "total") String type,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "30") int size,
                                    HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        if (courseId == null) return badRequest("请先选择课程");
        if (user.isTeacher() && !courseAccess.canAccess(user, courseId)) return forbidden();

        Integer studentId = user.isStudent() ? user.getRefId() : null;
        List<Map<String, Object>> rows = scoreService.buildScoreReport(courseId, clazzId, studentId);
        Map<String, Object> result = page(rows, page, size);
        result.put("type", type);
        return ResponseEntity.ok(result);
    }

    /** 导出成绩 CSV（前端用 <a download> 直接下载） */
    @GetMapping("/scores/export")
    public void exportScores(@RequestParam(required = false) Integer courseId,
                             @RequestParam(required = false) Integer clazzId,
                             @RequestParam(defaultValue = "total") String type,
                             HttpSession session, HttpServletResponse response) throws IOException {
        LoginUser user = current(session);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return;
        }
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

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=score-report.csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.write('\uFEFF');
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
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "30") int size,
                                   HttpSession session) {
        LoginUser user = current(session);
        if (user == null) return unauthorized();
        if (!user.isAdmin()) return forbidden();

        int total = auditMapper.count();
        int totalPages = Math.max(1, (total + size - 1) / size);
        int current = Math.min(Math.max(1, page), totalPages);
        int offset = (current - 1) * size;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", auditMapper.findPage(offset, size));
        result.put("total", total);
        result.put("page", current);
        result.put("totalPages", totalPages);
        return ResponseEntity.ok(result);
    }

    private <T> Map<String, Object> page(List<T> all, int page, int size) {
        int total = all.size();
        int totalPages = Math.max(1, (total + size - 1) / size);
        int current = Math.min(Math.max(1, page), totalPages);
        int from = Math.min((current - 1) * size, total);
        int to = Math.min(from + size, total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", all.subList(from, to));
        result.put("total", total);
        result.put("page", current);
        result.put("totalPages", totalPages);
        return result;
    }

    private LoginUser current(HttpSession session) {
        return (LoginUser) session.getAttribute(Constants.SESSION_USER);
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未登录"));
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权访问"));
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
