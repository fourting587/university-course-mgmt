package com.university.cms.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败次数限制：同一账号连续失败 5 次后锁定 10 分钟。
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private static class Attempt {
        int count;
        long lockUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username) {
        Attempt a = attempts.get(normalize(username));
        return a != null && a.lockUntil > System.currentTimeMillis();
    }

    public long remainingMinutes(String username) {
        Attempt a = attempts.get(normalize(username));
        if (a == null || a.lockUntil <= System.currentTimeMillis()) {
            return 0;
        }
        return Math.max(1, TimeUnit.MILLISECONDS.toMinutes(a.lockUntil - System.currentTimeMillis()) + 1);
    }

    public void onFailure(String username) {
        attempts.compute(normalize(username), (k, a) -> {
            if (a == null) {
                a = new Attempt();
            }
            if (a.lockUntil <= System.currentTimeMillis()) {
                a.count++;
                if (a.count >= MAX_ATTEMPTS) {
                    a.lockUntil = System.currentTimeMillis() + LOCK_MILLIS;
                    a.count = 0;
                }
            }
            return a;
        });
    }

    public void onSuccess(String username) {
        attempts.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
