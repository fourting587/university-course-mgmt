package com.university.cms.controller.api;

import com.university.cms.common.Constants;
import com.university.cms.common.LoginAttemptService;
import com.university.cms.common.LoginUser;
import com.university.cms.common.PasswordUtil;
import com.university.cms.common.SessionRegistry;
import com.university.cms.entity.SysUser;
import com.university.cms.mapper.SysUserMapper;
import com.university.cms.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 前后端分离后的认证接口：JSON 登录 / 登出 / 当前用户 / 修改密码。
 * 会话仍用 HttpSession（Cookie），前端与后端同源部署（Nginx 反代 /api）。
 */
@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private PasswordUtil passwordUtil;
    @Autowired private LoginAttemptService loginAttemptService;
    @Autowired private SessionRegistry sessionRegistry;
    @Autowired private AuditService auditService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        if (username.isEmpty() || password.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "用户名和密码不能为空");
        }
        if (loginAttemptService.isBlocked(username)) {
            auditService.record(null, "LOGIN_BLOCKED", username, "失败次数过多被锁定");
            return error(HttpStatus.TOO_MANY_REQUESTS,
                    "登录失败次数过多，请 " + loginAttemptService.remainingMinutes(username) + " 分钟后再试");
        }

        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null || !passwordUtil.matches(password, user.getPassword())) {
            loginAttemptService.onFailure(username);
            auditService.record(null, "LOGIN_FAIL", username, "用户名或密码错误");
            return error(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            auditService.record(null, "LOGIN_DISABLED", username, "账号已禁用");
            return error(HttpStatus.FORBIDDEN, "账号已禁用，请联系管理员");
        }

        loginAttemptService.onSuccess(username);
        if (!passwordUtil.isHashed(user.getPassword())) {
            user.setPassword(passwordUtil.encode(password));
            sysUserMapper.update(user);
        }
        boolean mustChange = passwordUtil.matches(Constants.DEFAULT_PASSWORD, user.getPassword());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRoleCode(user.getRoleCode());
        loginUser.setRefId(user.getRefId());
        session.setAttribute(Constants.SESSION_USER, loginUser);
        sessionRegistry.register(user.getId(), session);
        if (mustChange) {
            session.setAttribute(Constants.SESSION_MUST_CHANGE, Boolean.TRUE);
        } else {
            session.removeAttribute(Constants.SESSION_MUST_CHANGE);
        }
        auditService.record(loginUser, "LOGIN", loginUser.getUsername(), mustChange ? "使用默认密码登录" : null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", userView(loginUser));
        result.put("mustChangePassword", mustChange);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            return error(HttpStatus.UNAUTHORIZED, "未登录");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", userView(user));
        result.put("mustChangePassword",
                Boolean.TRUE.equals(session.getAttribute(Constants.SESSION_MUST_CHANGE)));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (user != null) {
            auditService.record(user, "LOGOUT", user.getUsername(), null);
        }
        sessionRegistry.remove(session);
        session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body,
                                                              HttpSession session) {
        LoginUser loginUser = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (loginUser == null) {
            return error(HttpStatus.UNAUTHORIZED, "未登录");
        }
        String newPassword = body.getOrDefault("newPassword", "");
        String confirmPassword = body.getOrDefault("confirmPassword", "");
        String error = validate(newPassword, confirmPassword);
        if (error != null) {
            return error(HttpStatus.BAD_REQUEST, error);
        }
        SysUser dbUser = sysUserMapper.findById(loginUser.getUserId());
        if (dbUser == null) {
            return error(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        dbUser.setPassword(passwordUtil.encode(newPassword.trim()));
        sysUserMapper.update(dbUser);
        sessionRegistry.invalidateOthers(loginUser.getUserId(), session);
        session.removeAttribute(Constants.SESSION_MUST_CHANGE);
        auditService.record(loginUser, "PASSWORD_CHANGE", loginUser.getUsername(), null);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private String validate(String password, String confirm) {
        if (password == null || password.trim().length() < 6) {
            return "新密码长度不能少于 6 位";
        }
        if (password.length() > 50) {
            return "新密码长度不能超过 50 位";
        }
        if (!password.equals(confirm)) {
            return "两次输入的新密码不一致";
        }
        if (Constants.DEFAULT_PASSWORD.equals(password.trim())) {
            return "新密码不能与默认密码相同";
        }
        return null;
    }

    private Map<String, Object> userView(LoginUser user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getUserId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("roleCode", user.getRoleCode());
        map.put("refId", user.getRefId());
        return map;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
