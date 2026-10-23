package com.university.cms.controller;

import com.university.cms.common.Constants;
import com.university.cms.common.LoginAttemptService;
import com.university.cms.common.LoginUser;
import com.university.cms.common.PasswordUtil;
import com.university.cms.common.SessionRegistry;
import com.university.cms.entity.SysUser;
import com.university.cms.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private com.university.cms.service.AuditService auditService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/doLogin")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        if (loginAttemptService.isBlocked(username)) {
            auditService.record(null, "LOGIN_BLOCKED", username, "失败次数过多被锁定");
            model.addAttribute("error", "登录失败次数过多，请 " + loginAttemptService.remainingMinutes(username) + " 分钟后再试");
            return "login";
        }
        SysUser user = sysUserMapper.findByUsername(username.trim());
        if (user == null || !passwordUtil.matches(password, user.getPassword())) {
            loginAttemptService.onFailure(username);
            auditService.record(null, "LOGIN_FAIL", username, "用户名或密码错误");
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            auditService.record(null, "LOGIN_DISABLED", username, "账号已禁用");
            model.addAttribute("error", "账号已禁用，请联系管理员");
            return "login";
        }
        loginAttemptService.onSuccess(username);
        // 历史明文密码登录成功后自动升级为 BCrypt 密文
        if (!passwordUtil.isHashed(user.getPassword())) {
            user.setPassword(passwordUtil.encode(password));
            sysUserMapper.update(user);
        }
        boolean usingDefaultPassword = passwordUtil.matches(Constants.DEFAULT_PASSWORD, user.getPassword());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRoleCode(user.getRoleCode());
        loginUser.setRefId(user.getRefId());
        session.setAttribute(Constants.SESSION_USER, loginUser);
        sessionRegistry.register(user.getId(), session);
        auditService.record(loginUser, "LOGIN", loginUser.getUsername(),
                usingDefaultPassword ? "使用默认密码登录" : null);
        if (usingDefaultPassword) {
            // 默认密码登录：强制先修改密码，改完才能进入系统
            session.setAttribute(Constants.SESSION_MUST_CHANGE, Boolean.TRUE);
            return "redirect:/password/change";
        }
        session.removeAttribute(Constants.SESSION_MUST_CHANGE);
        return "redirect:/home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        sessionRegistry.remove(session);
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model,
                       @RequestParam(required = false) String error) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        if ("forbidden".equals(error)) {
            model.addAttribute("error", "无权访问该功能");
        }
        return "home";
    }
}
