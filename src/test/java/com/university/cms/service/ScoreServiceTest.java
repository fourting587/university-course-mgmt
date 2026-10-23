package com.university.cms.service;

import com.university.cms.entity.EvalRule;
import com.university.cms.entity.Student;
import com.university.cms.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock private AttendanceMapper attendanceMapper;
    @Mock private PerformanceMapper performanceMapper;
    @Mock private ExperimentMapper experimentMapper;
    @Mock private HomeworkMapper homeworkMapper;
    @Mock private EvalRuleMapper evalRuleMapper;
    @Mock private ExamScoreMapper examScoreMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private CourseMapper courseMapper;

    @InjectMocks
    private ScoreService scoreService;

    @Test
    void calculatesWeightedScoresWithSingleAggregateQueryPerMetric() {
        Student s1 = student(1, "202201001", "张三");
        Student s2 = student(2, "202201002", "李四");

        when(courseMapper.findClazzIdsByCourse(1)).thenReturn(List.of(10));
        when(studentMapper.findByClazzId(10)).thenReturn(List.of(s1, s2));
        when(evalRuleMapper.findByCourseId(1)).thenReturn(rule());
        when(attendanceMapper.countByCourse(1)).thenReturn(List.of(
                row("studentId", 1, "status", "PRESENT", "cnt", 4),
                row("studentId", 2, "status", "LATE", "cnt", 2),
                row("studentId", 2, "status", "PRESENT", "cnt", 2)));
        when(performanceMapper.avgByCourse(1)).thenReturn(List.of(
                avg(1, 80.0), avg(2, 70.0)));
        when(experimentMapper.avgByCourse(1)).thenReturn(List.of(
                avg(1, 90.0), avg(2, 80.0)));
        when(homeworkMapper.avgByCourse(1)).thenReturn(List.of(
                avg(1, 100.0), avg(2, 60.0)));
        when(examScoreMapper.scoreByCourse(1)).thenReturn(List.of(
                scoreRow(1, 90.0), scoreRow(2, 50.0)));

        List<Map<String, Object>> rows = scoreService.buildScoreReport(1, null, null);

        assertEquals(2, rows.size());

        // 100*20 + 80*20 + 90*30 + 100*30 = 9300 / 100 = 93.0；93*0.4 + 90*0.6 = 91.2
        assertEquals(93.0, num(rows.get(0).get("regularScore")), 0.001);
        assertEquals(91.2, num(rows.get(0).get("totalScore")), 0.001);
        assertEquals(100.0, num(rows.get(0).get("attendanceScore")), 0.001);

        // 考勤 (80*2+100*2)/4 = 90；(90*20+70*20+80*30+60*30)/100 = 74；74*0.4+50*0.6 = 59.6
        assertEquals(90.0, num(rows.get(1).get("attendanceScore")), 0.001);
        assertEquals(74.0, num(rows.get(1).get("regularScore")), 0.001);
        assertEquals(59.6, num(rows.get(1).get("totalScore")), 0.001);

        // 每个分项只查一次，不再按学生循环查询
        verify(attendanceMapper, times(1)).countByCourse(1);
        verify(performanceMapper, times(1)).avgByCourse(1);
        verify(experimentMapper, times(1)).avgByCourse(1);
        verify(homeworkMapper, times(1)).avgByCourse(1);
        verify(examScoreMapper, times(1)).scoreByCourse(1);
        verify(attendanceMapper, never()).countByStatus(any(), any());
        verify(performanceMapper, never()).avgScore(any(), any());
    }

    @Test
    void singleStudentReportOnlyReturnsThatStudent() {
        Student s2 = student(2, "202201002", "李四");
        when(studentMapper.findById(2)).thenReturn(s2);
        when(evalRuleMapper.findByCourseId(1)).thenReturn(rule());
        when(attendanceMapper.countByCourse(1)).thenReturn(List.of());
        when(performanceMapper.avgByCourse(1)).thenReturn(List.of());
        when(experimentMapper.avgByCourse(1)).thenReturn(List.of());
        when(homeworkMapper.avgByCourse(1)).thenReturn(List.of());
        when(examScoreMapper.scoreByCourse(1)).thenReturn(List.of());

        List<Map<String, Object>> rows = scoreService.buildScoreReport(1, null, 2);

        assertEquals(1, rows.size());
        assertEquals("202201002", rows.get(0).get("studentNo"));
        assertEquals(0.0, num(rows.get(0).get("totalScore")), 0.001);
        verify(courseMapper, never()).findClazzIdsByCourse(any());
    }

    private Student student(int id, String no, String name) {
        Student s = new Student();
        s.setId(id);
        s.setStudentNo(no);
        s.setName(name);
        s.setClassName("23gb物联网工程1班");
        return s;
    }

    private EvalRule rule() {
        EvalRule r = new EvalRule();
        r.setCourseId(1);
        r.setAttendanceWeight(new BigDecimal("20"));
        r.setPerformanceWeight(new BigDecimal("20"));
        r.setExperimentWeight(new BigDecimal("30"));
        r.setHomeworkWeight(new BigDecimal("30"));
        r.setRegularRatio(new BigDecimal("40"));
        r.setExamRatio(new BigDecimal("60"));
        return r;
    }

    private Map<String, Object> row(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        return m;
    }

    private Map<String, Object> avg(int studentId, double avg) {
        Map<String, Object> m = new HashMap<>();
        m.put("studentId", studentId);
        m.put("avgScore", avg);
        return m;
    }

    private Map<String, Object> scoreRow(int studentId, double score) {
        Map<String, Object> m = new HashMap<>();
        m.put("studentId", studentId);
        m.put("score", score);
        return m;
    }

    private double num(Object o) {
        return ((Number) o).doubleValue();
    }
}
