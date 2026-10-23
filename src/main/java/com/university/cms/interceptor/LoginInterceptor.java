package com.university.cms.interceptor;

import com.university.cms.common.Constants;
import com.university.cms.common.LoginUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;

/**
 * 登录拦截：未登录跳转登录页；POST 同源校验防 CSRF；按路径前缀做粗粒度角色校验
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        String uri = request.getRequestURI();

        // CSRF 防护：POST 请求的来源必须是本站
        if ("POST".equalsIgnoreCase(request.getMethod()) && !isSameOrigin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-site request blocked");
            return false;
        }

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        // 使用默认密码登录后，必须先修改密码，其他页面一律拦截
        if (Boolean.TRUE.equals(session.getAttribute(Constants.SESSION_MUST_CHANGE))
                && !uri.startsWith("/password/change")
                && !uri.equals("/logout")) {
            response.sendRedirect(request.getContextPath() + "/password/change");
            return false;
        }

        // 管理端基础数据仅管理员可访问
        if (uri.startsWith("/admin") && !user.isAdmin()) {
            response.sendRedirect(request.getContextPath() + "/home?error=forbidden");
            return false;
        }

        // 成绩录入、批量操作：教师或管理员
        if (uri.startsWith("/teach") && !(user.isTeacher() || user.isAdmin())) {
            response.sendRedirect(request.getContextPath() + "/home?error=forbidden");
            return false;
        }

        return true;
    }

    /**
     * 同源校验：浏览器发起的跨站 POST 会带外站 Origin/Referer，这里直接拒绝。
     * 没有 Origin/Referer 的请求（如 curl、服务端调用）不拦截。
     */
    private boolean isSameOrigin(HttpServletRequest request) {
        String source = request.getHeader("Origin");
        if (source == null || source.isEmpty()) {
            source = request.getHeader("Referer");
        }
        if (source == null || source.isEmpty()) {
            return true;
        }
        try {
            String host = URI.create(source).getHost();
            return host != null && host.equalsIgnoreCase(request.getServerName());
        } catch (Exception e) {
            return false;
        }
    }
}
