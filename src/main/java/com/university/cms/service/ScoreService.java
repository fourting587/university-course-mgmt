package com.university.cms.service;

import com.university.cms.entity.EvalRule;
import com.university.cms.entity.Student;
import com.university.cms.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 平时成绩 / 总成绩汇总计算
 * 平时成绩 = 考勤分*考勤权重 + 课堂表现*表现权重 + 实验*实验权重 + 作业*作业权重
 * 总成绩   = 平时成绩*平时占比 + 期末成绩*期末占比
 *
 * 一次性按课程聚合各分项成绩，避免逐个学生查询（N+1）。
 */
@Service
public class ScoreService {

    @Autowired
    private AttendanceMapper attendanceMapper;
    @Autowired
    private PerformanceMapper performanceMapper;
    @Autowired
    private ExperimentMapper experimentMapper;
    @Autowired
    private HomeworkMapper homeworkMapper;
    @Autowired
    private EvalRuleMapper evalRuleMapper;
    @Autowired
    private ExamScoreMapper examScoreMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseMapper courseMapper;

    public List<Map<String, Object>> buildScoreReport(Integer courseId, Integer clazzId, Integer studentId) {
        List<Student> students;
        if (studentId != null) {
            Student one = studentMapper.findById(studentId);
            students = one == null ? Collections.emptyList() : Collections.singletonList(one);
        } else if (clazzId != null) {
            students = studentMapper.findByClazzId(clazzId);
        } else {
            List<Integer> clazzIds = courseMapper.findClazzIdsByCourse(courseId);
            students = new ArrayList<>();
            Set<Integer> seen = new HashSet<>();
            for (Integer cid : clazzIds) {
                for (Student s : studentMapper.findByClazzId(cid)) {
                    if (seen.add(s.getId())) {
                        students.add(s);
                    }
                }
            }
        }

        EvalRule rule = evalRuleMapper.findByCourseId(courseId);
        if (rule == null) {
            rule = defaultRule(courseId);
        }

        // 一次查询按课程聚合，后面纯内存计算
        Map<Integer, Double> attendanceMap = loadAttendance(courseId);
        Map<Integer, Double> performanceMap = loadAvg(performanceMapper.avgByCourse(courseId));
        Map<Integer, Double> experimentMap = loadAvg(experimentMapper.avgByCourse(courseId));
        Map<Integer, Double> homeworkMap = loadAvg(homeworkMapper.avgByCourse(courseId));
        Map<Integer, Double> examMap = loadExam(courseId);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(calcOne(s, courseId, rule, attendanceMap, performanceMap, experimentMap, homeworkMap, examMap));
        }
        return rows;
    }

    private Map<String, Object> calcOne(Student s, Integer courseId, EvalRule rule,
                                        Map<Integer, Double> attendanceMap,
                                        Map<Integer, Double> performanceMap,
                                        Map<Integer, Double> experimentMap,
                                        Map<Integer, Double> homeworkMap,
                                        Map<Integer, Double> examMap) {
        double attendanceScore = nz(attendanceMap.get(s.getId()));
        double performanceScore = nz(performanceMap.get(s.getId()));
        double experimentScore = nz(experimentMap.get(s.getId()));
        double homeworkScore = nz(homeworkMap.get(s.getId()));

        double wAtt = rule.getAttendanceWeight().doubleValue();
        double wPerf = rule.getPerformanceWeight().doubleValue();
        double wExp = rule.getExperimentWeight().doubleValue();
        double wHw = rule.getHomeworkWeight().doubleValue();
        double wSum = wAtt + wPerf + wExp + wHw;
        if (wSum <= 0) {
            wSum = 100;
        }

        double regular = (attendanceScore * wAtt + performanceScore * wPerf
                + experimentScore * wExp + homeworkScore * wHw) / wSum;
        regular = round(regular);

        double examScore = nz(examMap.get(s.getId()));
        double regularRatio = rule.getRegularRatio().doubleValue();
        double examRatio = rule.getExamRatio().doubleValue();
        double total = (regular * regularRatio + examScore * examRatio) / 100.0;
        total = round(total);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("studentId", s.getId());
        row.put("studentNo", s.getStudentNo());
        row.put("studentName", s.getName());
        row.put("className", s.getClassName());
        row.put("attendanceScore", round(attendanceScore));
        row.put("performanceScore", round(performanceScore));
        row.put("experimentScore", round(experimentScore));
        row.put("homeworkScore", round(homeworkScore));
        row.put("regularScore", regular);
        row.put("examScore", round(examScore));
        row.put("totalScore", total);
        row.put("courseId", courseId);
        return row;
    }

    /** 考勤折算百分制：到课=100，迟到=80，请假=60，缺勤=0，再按次数平均 */
    private Map<Integer, Double> loadAttendance(Integer courseId) {
        Map<Integer, double[]> acc = new HashMap<>(); // studentId -> [sum, total]
        List<Map<String, Object>> groups = attendanceMapper.countByCourse(courseId);
        for (Map<String, Object> g : groups) {
            int studentId = ((Number) g.get("studentId")).intValue();
            String status = String.valueOf(g.get("status"));
            int cnt = ((Number) g.get("cnt")).intValue();
            double point;
            switch (status) {
                case "PRESENT": point = 100; break;
                case "LATE": point = 80; break;
                case "LEAVE": point = 60; break;
                default: point = 0;
            }
            double[] v = acc.computeIfAbsent(studentId, k -> new double[2]);
            v[0] += point * cnt;
            v[1] += cnt;
        }
        Map<Integer, Double> result = new HashMap<>();
        for (Map.Entry<Integer, double[]> e : acc.entrySet()) {
            result.put(e.getKey(), e.getValue()[1] == 0 ? 0 : e.getValue()[0] / e.getValue()[1]);
        }
        return result;
    }

    private Map<Integer, Double> loadAvg(List<Map<String, Object>> list) {
        Map<Integer, Double> map = new HashMap<>();
        for (Map<String, Object> row : list) {
            map.put(((Number) row.get("studentId")).intValue(), ((Number) row.get("avgScore")).doubleValue());
        }
        return map;
    }

    private Map<Integer, Double> loadExam(Integer courseId) {
        Map<Integer, Double> map = new HashMap<>();
        for (Map<String, Object> row : examScoreMapper.scoreByCourse(courseId)) {
            map.put(((Number) row.get("studentId")).intValue(), ((Number) row.get("score")).doubleValue());
        }
        return map;
    }

    private EvalRule defaultRule(Integer courseId) {
        EvalRule r = new EvalRule();
        r.setCourseId(courseId);
        r.setAttendanceWeight(new BigDecimal("20"));
        r.setPerformanceWeight(new BigDecimal("20"));
        r.setExperimentWeight(new BigDecimal("30"));
        r.setHomeworkWeight(new BigDecimal("30"));
        r.setRegularRatio(new BigDecimal("40"));
        r.setExamRatio(new BigDecimal("60"));
        return r;
    }

    private double nz(Double v) {
        return v == null ? 0 : v;
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
