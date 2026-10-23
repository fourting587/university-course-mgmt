package com.university.cms.controller.api;

import com.university.cms.common.Constants;
import com.university.cms.common.CourseAccess;
import com.university.cms.common.LoginUser;
import com.university.cms.entity.*;
import com.university.cms.mapper.*;
import com.university.cms.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 前后端分离版的教学写接口：考勤、课堂表现、实验、作业、期末成绩。
 */
@RestController
@RequestMapping("/api")
public class ApiTeachingController {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired private AttendanceMapper attendanceMapper;
    @Autowired private PerformanceMapper performanceMapper;
    @Autowired private ExperimentMapper experimentMapper;
    @Autowired private HomeworkMapper homeworkMapper;
    @Autowired private ExamScoreMapper examScoreMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private CourseAccess courseAccess;
    @Autowired private AuditService auditService;

    // ---------- 录入辅助数据 ----------

    @GetMapping("/courses/{id}/students")
    public ResponseEntity<?> courseStudents(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        if (!courseAccess.canAccess(user, id)) return forbidden();

        Map<Integer, Student> map = new LinkedHashMap<>();
        for (Integer clazzId : courseMapper.findClazzIdsByCourse(id)) {
            for (Student s : studentMapper.findByClazzId(clazzId)) {
                map.put(s.getId(), s);
            }
        }
        return ResponseEntity.ok(map.values());
    }

    @GetMapping("/experiment-defs")
    public ResponseEntity<?> experimentDefs(HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(user.isAdmin()
                ? experimentMapper.findAll() : experimentMapper.findByTeacherId(user.getRefId()));
    }

    @GetMapping("/homework-defs")
    public ResponseEntity<?> homeworkDefs(HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(user.isAdmin()
                ? homeworkMapper.findAll() : homeworkMapper.findByTeacherId(user.getRefId()));
    }

    @GetMapping("/exams")
    public ResponseEntity<?> exams(@RequestParam Integer courseId, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        if (!courseAccess.canAccess(user, courseId)) return forbidden();
        return ResponseEntity.ok(examScoreMapper.search(null, courseId, null, null));
    }

    // ---------- 考勤 ----------

    @PostMapping("/attendance")
    public ResponseEntity<?> saveAttendance(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = intg(body, "courseId");
        if (!courseAccess.canAccess(user, courseId)) return forbidden();

        Attendance a = new Attendance();
        a.setCourseId(courseId);
        a.setStudentId(intg(body, "studentId"));
        a.setAttendDate(date(body, "attendDate"));
        a.setStatus(str(body, "status"));
        a.setRemark(str(body, "remark"));
        a.setRecorderId(user.getUserId());
        try {
            attendanceMapper.insert(a);
        } catch (Exception e) {
            return error(HttpStatus.CONFLICT, "该学生当天已有考勤记录");
        }
        auditService.record(user, "ATT_SAVE", "学生" + a.getStudentId() + " 课程" + courseId, a.getStatus());
        return ok();
    }

    /** 批量考勤：{ courseId, attendDate, records:[{studentId,status}] } */
    @PostMapping("/attendance/batch")
    public ResponseEntity<?> batchAttendance(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = intg(body, "courseId");
        if (!courseAccess.canAccess(user, courseId)) return forbidden();
        LocalDate day = LocalDate.parse(str(body, "attendDate"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) body.get("records");
        if (records == null || records.isEmpty()) return error(HttpStatus.BAD_REQUEST, "没有可保存的记录");

        int saved = 0, skipped = 0;
        for (Map<String, Object> item : records) {
            Attendance a = new Attendance();
            a.setCourseId(courseId);
            a.setStudentId(intg(item, "studentId"));
            a.setAttendDate(java.sql.Date.valueOf(day));
            a.setStatus(str(item, "status"));
            a.setRecorderId(user.getUserId());
            try {
                attendanceMapper.insert(a);
                saved++;
            } catch (Exception e) {
                skipped++;
            }
        }
        auditService.record(user, "ATT_BATCH", "课程" + courseId, "成功" + saved + " 跳过" + skipped);
        return ResponseEntity.ok(Map.of("ok", true, "saved", saved, "skipped", skipped));
    }

    @DeleteMapping("/attendance/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Attendance existing = attendanceMapper.findById(id);
        if (existing == null) return notFound();
        if (!courseAccess.canAccess(user, existing.getCourseId())) return forbidden();
        attendanceMapper.deleteById(id);
        auditService.record(user, "ATT_DELETE", "考勤ID " + id);
        return ok();
    }

    // ---------- 课堂表现 ----------

    @PostMapping("/performance")
    public ResponseEntity<?> savePerformance(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = intg(body, "courseId");
        if (!courseAccess.canAccess(user, courseId)) return forbidden();
        BigDecimal score = dec(body, "score");
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(HUNDRED) > 0) {
            return error(HttpStatus.BAD_REQUEST, "课堂表现分数必须在 0-100 之间");
        }

        Performance p = new Performance();
        p.setId(intg(body, "id"));
        p.setCourseId(courseId);
        p.setStudentId(intg(body, "studentId"));
        p.setRecordDate(date(body, "recordDate"));
        p.setScore(score);
        p.setComment(str(body, "comment"));
        p.setRecorderId(user.getUserId());
        if (p.getId() == null) performanceMapper.insert(p); else performanceMapper.update(p);
        auditService.record(user, "PERF_SAVE", "学生" + p.getStudentId() + " 课程" + courseId, score.toPlainString());
        return ok();
    }

    @DeleteMapping("/performance/{id}")
    public ResponseEntity<?> deletePerformance(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Performance existing = performanceMapper.findById(id);
        if (existing == null) return notFound();
        if (!courseAccess.canAccess(user, existing.getCourseId())) return forbidden();
        performanceMapper.deleteById(id);
        auditService.record(user, "PERF_DELETE", "表现ID " + id);
        return ok();
    }

    // ---------- 实验 ----------

    @PostMapping("/experiments")
    public ResponseEntity<?> saveExperiment(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = intg(body, "courseId");
        if (!courseAccess.canAccess(user, courseId)) return forbidden();

        Experiment e = new Experiment();
        e.setId(intg(body, "id"));
        e.setCourseId(courseId);
        e.setTitle(str(body, "title"));
        BigDecimal full = dec(body, "fullScore");
        e.setFullScore(full == null ? HUNDRED : full);
        e.setDueDate(date(body, "dueDate"));
        if (e.getFullScore().compareTo(BigDecimal.ZERO) <= 0 || e.getFullScore().compareTo(new BigDecimal("1000")) > 0) {
            return error(HttpStatus.BAD_REQUEST, "实验满分必须在 0-1000 之间");
        }
        boolean isNew = e.getId() == null;
        if (isNew) experimentMapper.insert(e); else experimentMapper.update(e);
        auditService.record(user, isNew ? "EXP_ADD" : "EXP_UPDATE", "实验 " + e.getTitle());
        return ok();
    }

    @DeleteMapping("/experiments/{id}")
    public ResponseEntity<?> deleteExperiment(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Experiment e = experimentMapper.findById(id);
        if (e == null) return notFound();
        if (!courseAccess.canAccess(user, e.getCourseId())) return forbidden();
        experimentMapper.deleteById(id);
        auditService.record(user, "EXP_DELETE", "实验ID " + id);
        return ok();
    }

    @PostMapping("/experiments/records")
    public ResponseEntity<?> saveExperimentRecord(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Experiment e = experimentMapper.findById(intg(body, "experimentId"));
        if (e == null) return notFound();
        if (!courseAccess.canAccess(user, e.getCourseId())) return forbidden();

        BigDecimal completion = dec(body, "completion");
        BigDecimal score = dec(body, "score");
        if (completion == null || completion.compareTo(BigDecimal.ZERO) < 0 || completion.compareTo(HUNDRED) > 0) {
            return error(HttpStatus.BAD_REQUEST, "完成度必须在 0-100 之间");
        }
        if (score != null && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(HUNDRED) > 0)) {
            return error(HttpStatus.BAD_REQUEST, "实验得分必须在 0-100 之间");
        }

        ExperimentRecord r = new ExperimentRecord();
        r.setId(intg(body, "id"));
        r.setExperimentId(e.getId());
        r.setStudentId(intg(body, "studentId"));
        r.setCompletion(completion);
        r.setScore(score);
        r.setRemark(str(body, "remark"));
        r.setSubmitTime(new Date());
        if (str(body, "submitTime") != null) r.setSubmitTime(datetime(body, "submitTime"));
        if (r.getId() == null) {
            ExperimentRecord existing = experimentMapper.findRecord(e.getId(), r.getStudentId());
            if (existing != null) r.setId(existing.getId());
        }
        if (r.getId() == null) experimentMapper.insertRecord(r); else experimentMapper.updateRecord(r);
        auditService.record(user, "EXP_RECORD_SAVE", "实验" + e.getId() + " 学生" + r.getStudentId());
        return ok();
    }

    @DeleteMapping("/experiments/records/{id}")
    public ResponseEntity<?> deleteExperimentRecord(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = experimentMapper.findCourseIdByRecordId(id);
        if (courseId == null) return notFound();
        if (!courseAccess.canAccess(user, courseId)) return forbidden();
        experimentMapper.deleteRecord(id);
        auditService.record(user, "EXP_RECORD_DELETE", "记录ID " + id);
        return ok();
    }

    // ---------- 作业 ----------

    @PostMapping("/homework")
    public ResponseEntity<?> saveHomework(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = intg(body, "courseId");
        if (!courseAccess.canAccess(user, courseId)) return forbidden();

        Homework h = new Homework();
        h.setId(intg(body, "id"));
        h.setCourseId(courseId);
        h.setTitle(str(body, "title"));
        BigDecimal full = dec(body, "fullScore");
        h.setFullScore(full == null ? HUNDRED : full);
        h.setDueDate(date(body, "dueDate"));
        if (h.getFullScore().compareTo(BigDecimal.ZERO) <= 0 || h.getFullScore().compareTo(new BigDecimal("1000")) > 0) {
            return error(HttpStatus.BAD_REQUEST, "作业满分必须在 0-1000 之间");
        }
        boolean isNew = h.getId() == null;
        if (isNew) homeworkMapper.insert(h); else homeworkMapper.update(h);
        auditService.record(user, isNew ? "HW_ADD" : "HW_UPDATE", "作业 " + h.getTitle());
        return ok();
    }

    @DeleteMapping("/homework/{id}")
    public ResponseEntity<?> deleteHomework(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Homework h = homeworkMapper.findById(id);
        if (h == null) return notFound();
        if (!courseAccess.canAccess(user, h.getCourseId())) return forbidden();
        homeworkMapper.deleteById(id);
        auditService.record(user, "HW_DELETE", "作业ID " + id);
        return ok();
    }

    @PostMapping("/homework/records")
    public ResponseEntity<?> saveHomeworkRecord(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Homework h = homeworkMapper.findById(intg(body, "homeworkId"));
        if (h == null) return notFound();
        if (!courseAccess.canAccess(user, h.getCourseId())) return forbidden();

        BigDecimal score = dec(body, "score");
        if (score != null && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(HUNDRED) > 0)) {
            return error(HttpStatus.BAD_REQUEST, "作业得分必须在 0-100 之间");
        }

        HomeworkRecord r = new HomeworkRecord();
        r.setId(intg(body, "id"));
        r.setHomeworkId(h.getId());
        r.setStudentId(intg(body, "studentId"));
        r.setScore(score);
        r.setStatus(str(body, "status"));
        if (r.getStatus() == null || r.getStatus().isEmpty()) {
            r.setStatus(score != null ? "GRADED" : "SUBMITTED");
        }
        r.setRemark(str(body, "remark"));
        r.setSubmitTime(new Date());
        if (str(body, "submitTime") != null) r.setSubmitTime(datetime(body, "submitTime"));
        if (r.getId() == null) {
            HomeworkRecord existing = homeworkMapper.findRecord(h.getId(), r.getStudentId());
            if (existing != null) r.setId(existing.getId());
        }
        if (r.getId() == null) homeworkMapper.insertRecord(r); else homeworkMapper.updateRecord(r);
        auditService.record(user, "HW_RECORD_SAVE", "作业" + h.getId() + " 学生" + r.getStudentId());
        return ok();
    }

    @DeleteMapping("/homework/records/{id}")
    public ResponseEntity<?> deleteHomeworkRecord(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = homeworkMapper.findCourseIdByRecordId(id);
        if (courseId == null) return notFound();
        if (!courseAccess.canAccess(user, courseId)) return forbidden();
        homeworkMapper.deleteRecord(id);
        auditService.record(user, "HW_RECORD_DELETE", "记录ID " + id);
        return ok();
    }

    // ---------- 期末成绩 ----------

    @PostMapping("/exams")
    public ResponseEntity<?> saveExam(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        Integer courseId = intg(body, "courseId");
        if (!courseAccess.canAccess(user, courseId)) return forbidden();
        BigDecimal score = dec(body, "score");
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(HUNDRED) > 0) {
            return error(HttpStatus.BAD_REQUEST, "期末成绩必须在 0-100 之间");
        }

        ExamScore e = new ExamScore();
        e.setStudentId(intg(body, "studentId"));
        e.setCourseId(courseId);
        e.setScore(score);
        e.setExamDate(date(body, "examDate"));
        ExamScore exist = examScoreMapper.findOne(e.getStudentId(), courseId);
        if (exist == null) {
            examScoreMapper.insert(e);
        } else {
            e.setId(exist.getId());
            examScoreMapper.update(e);
        }
        auditService.record(user, "EXAM_SAVE", "学生" + e.getStudentId() + " 课程" + courseId, score.toPlainString());
        return ok();
    }

    @DeleteMapping("/exams/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable Integer id, HttpSession session) {
        LoginUser user = user(session);
        if (user == null) return unauthorized();
        ExamScore existing = examScoreMapper.findById(id);
        if (existing == null) return notFound();
        if (!courseAccess.canAccess(user, existing.getCourseId())) return forbidden();
        examScoreMapper.deleteById(id);
        auditService.record(user, "EXAM_DELETE", "期末成绩ID " + id);
        return ok();
    }

    // ---------- 工具方法 ----------

    private LoginUser user(HttpSession session) {
        return (LoginUser) session.getAttribute(Constants.SESSION_USER);
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private Integer intg(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Integer.valueOf(String.valueOf(v));
    }

    private BigDecimal dec(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return new BigDecimal(String.valueOf(v));
    }

    private java.sql.Date date(Map<String, Object> map, String key) {
        String s = str(map, key);
        if (s == null) return null;
        return java.sql.Date.valueOf(LocalDate.parse(s.substring(0, 10)));
    }

    private Date datetime(Map<String, Object> map, String key) {
        String s = str(map, key);
        if (s == null) return null;
        String normalized = s.replace('T', ' ');
        if (normalized.length() == 16) normalized += ":00";
        return java.sql.Timestamp.valueOf(LocalDateTime.parse(normalized,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private ResponseEntity<Map<String, Object>> ok() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未登录"));
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权操作该课程"));
    }

    private ResponseEntity<Map<String, Object>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "记录不存在"));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
