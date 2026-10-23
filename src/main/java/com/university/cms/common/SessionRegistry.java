package com.university.cms.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线会话登记：用于在修改密码后让该账号的其他会话立即失效。
 */
@Component
public class SessionRegistry {

    private final Map<Integer, Set<HttpSession>> sessions = new ConcurrentHashMap<>();

    public void register(Integer userId, HttpSession session) {
        if (userId == null || session == null) {
            return;
        }
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(HttpSession session) {
        if (session == null) {
            return;
        }
        for (Set<HttpSession> set : sessions.values()) {
            set.remove(session);
        }
    }

    /** 使该用户除当前会话外的其他会话全部失效 */
    public void invalidateOthers(Integer userId, HttpSession current) {
        if (userId == null) {
            return;
        }
        Set<HttpSession> set = sessions.get(userId);
        if (set == null) {
            return;
        }
        for (HttpSession s : set) {
            if (s != current) {
                try {
                    s.invalidate();
                } catch (Exception ignored) {
                    // 会话可能已经过期
                }
            }
        }
        set.clear();
        if (current != null) {
            set.add(current);
        }
    }
}
