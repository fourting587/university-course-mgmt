package com.university.cms.service;

import com.university.cms.common.LoginUser;
import com.university.cms.entity.AuditLog;
import com.university.cms.mapper.AuditMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作审计：记录关键写操作，失败不影响主流程。
 */
@Service
public class AuditService {

    @Autowired
    private AuditMapper auditMapper;

    public void record(LoginUser user, String action, String target, String detail) {
        try {
            AuditLog log = new AuditLog();
            if (user != null) {
                log.setUserId(user.getUserId());
                log.setUsername(user.getUsername());
            } else {
                log.setUsername(target);
            }
            log.setAction(action);
            log.setTarget(truncate(target, 100));
            log.setDetail(truncate(detail, 500));
            log.setIp(currentIp());
            auditMapper.insert(log);
        } catch (Exception ignored) {
            // 审计失败不应影响业务
        }
    }

    public void record(LoginUser user, String action, String target) {
        record(user, action, target, null);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
