package com.university.cms.interceptor;

import com.university.cms.common.Constants;
import com.university.cms.common.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginInterceptorTest {

    private LoginInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        interceptor = new LoginInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("");
        when(request.getServerName()).thenReturn("localhost");
    }

    private LoginUser login(String role, Integer refId) {
        LoginUser u = new LoginUser();
        u.setRoleCode(role);
        u.setRefId(refId);
        return u;
    }

    @Test
    void anonymousRedirectsToLogin() throws Exception {
        when(request.getRequestURI()).thenReturn("/home");
        when(session.getAttribute(Constants.SESSION_USER)).thenReturn(null);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendRedirect("/login");
    }

    @Test
    void crossSitePostIsBlocked() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn("http://evil.example.com");
        when(session.getAttribute(Constants.SESSION_USER)).thenReturn(login(Constants.ROLE_ADMIN, null));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }

    @Test
    void sameSitePostPasses() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn("http://localhost:1001");
        when(request.getRequestURI()).thenReturn("/admin/clazz/save");
        when(session.getAttribute(Constants.SESSION_USER)).thenReturn(login(Constants.ROLE_ADMIN, null));

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void mustChangePasswordBlocksOtherPages() throws Exception {
        when(request.getRequestURI()).thenReturn("/home");
        when(session.getAttribute(Constants.SESSION_USER)).thenReturn(login(Constants.ROLE_STUDENT, 1));
        when(session.getAttribute(Constants.SESSION_MUST_CHANGE)).thenReturn(Boolean.TRUE);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendRedirect("/password/change");
    }

    @Test
    void mustChangePasswordAllowsChangePage() throws Exception {
        when(request.getRequestURI()).thenReturn("/password/change");
        when(session.getAttribute(Constants.SESSION_USER)).thenReturn(login(Constants.ROLE_STUDENT, 1));
        when(session.getAttribute(Constants.SESSION_MUST_CHANGE)).thenReturn(Boolean.TRUE);

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void studentCannotAccessAdmin() throws Exception {
        when(request.getRequestURI()).thenReturn("/admin/student");
        when(session.getAttribute(Constants.SESSION_USER)).thenReturn(login(Constants.ROLE_STUDENT, 1));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendRedirect("/home?error=forbidden");
    }

    @Test
    void studentCannotAccessTeach() throws Exception {
        when(request.getRequestURI()).thenReturn("/teach/exam");
        when(session.getAttribute(Constants.SESSION_USER)).thenReturn(login(Constants.ROLE_STUDENT, 1));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).sendRedirect("/home?error=forbidden");
    }
}
