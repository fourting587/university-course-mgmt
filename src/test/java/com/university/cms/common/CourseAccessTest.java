package com.university.cms.common;

import com.university.cms.entity.Course;
import com.university.cms.mapper.CourseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessTest {

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseAccess courseAccess;

    private LoginUser user(String role, Integer refId) {
        LoginUser u = new LoginUser();
        u.setRoleCode(role);
        u.setRefId(refId);
        return u;
    }

    @Test
    void adminCanAccessAnyCourse() {
        assertTrue(courseAccess.canAccess(user(Constants.ROLE_ADMIN, null), 99));
    }

    @Test
    void teacherCanAccessOwnCourseOnly() {
        Course own = new Course();
        own.setId(1);
        own.setTeacherId(5);
        when(courseMapper.findById(1)).thenReturn(own);

        assertTrue(courseAccess.canAccess(user(Constants.ROLE_TEACHER, 5), 1));
        assertFalse(courseAccess.canAccess(user(Constants.ROLE_TEACHER, 6), 1));
    }

    @Test
    void missingCourseIsDenied() {
        when(courseMapper.findById(2)).thenReturn(null);
        assertFalse(courseAccess.canAccess(user(Constants.ROLE_TEACHER, 5), 2));
        assertFalse(courseAccess.canAccess(null, 2));
        assertFalse(courseAccess.canAccess(user(Constants.ROLE_TEACHER, 5), null));
    }

    @Test
    void teacherScopeReturnsRefIdOnlyForTeacher() {
        assertEquals(5, courseAccess.teacherScope(user(Constants.ROLE_TEACHER, 5)));
        assertNull(courseAccess.teacherScope(user(Constants.ROLE_ADMIN, null)));
        assertNull(courseAccess.teacherScope(user(Constants.ROLE_STUDENT, 1)));
    }
}
