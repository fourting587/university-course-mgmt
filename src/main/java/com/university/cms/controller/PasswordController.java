package com.university.cms.controller;

import com.university.cms.common.Constants;
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

/**
 * 修改密码：默认密码登录后强制修改，改完用新密码登录。
 */
@Controller
public class PasswordController {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    private com.university.cms.service.AuditService auditService;

    @GetMapping("/password/change")
    public String page(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("forced",
                Boolean.TRUE.equals(session.getAttribute(Constants.SESSION_MUST_CHANGE)));
        return "password/change";
    }

    @PostMapping("/password/change")
    public String change(@RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         HttpSession session, Model model) {
        LoginUser loginUser = (LoginUser) session.getAttribute(Constants.SESSION_USER);
        if (loginUser == null) {
            return "redirect:/login";
        }

        String error = validate(newPassword, confirmPassword);
        if (error != null) {
            model.addAttribute("user", loginUser);
            model.addAttribute("forced",
                    Boolean.TRUE.equals(session.getAttribute(Constants.SESSION_MUST_CHANGE)));
            model.addAttribute("error", error);
            return "password/change";
        }

        SysUser dbUser = sysUserMapper.findById(loginUser.getUserId());
        if (dbUser == null) {
            return "redirect:/login";
        }
        dbUser.setPassword(passwordUtil.encode(newPassword.trim()));
        sysUserMapper.update(dbUser);

        // 改密后让该账号其他设备上的会话立即失效
        sessionRegistry.invalidateOthers(loginUser.getUserId(), session);
        auditService.record(loginUser, "PASSWORD_CHANGE", loginUser.getUsername(), null);

        session.removeAttribute(Constants.SESSION_MUST_CHANGE);
        return "redirect:/home?msg=pwdChanged";
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
}
