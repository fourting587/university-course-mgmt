package com.university.cms.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void lockedAfterFiveFailures() {
        String user = "202201001";
        assertFalse(service.isBlocked(user));
        for (int i = 0; i < 4; i++) {
            service.onFailure(user);
        }
        assertFalse(service.isBlocked(user), "4 次失败还不应锁定");

        service.onFailure(user);
        assertTrue(service.isBlocked(user), "第 5 次失败后应锁定");
        assertTrue(service.remainingMinutes(user) >= 1);
    }

    @Test
    void successClearsFailures() {
        String user = "T1001";
        for (int i = 0; i < 4; i++) {
            service.onFailure(user);
        }
        service.onSuccess(user);
        assertFalse(service.isBlocked(user));
        // 之前计数被清空，再失败 4 次仍不应锁定
        for (int i = 0; i < 4; i++) {
            service.onFailure(user);
        }
        assertFalse(service.isBlocked(user));
    }

    @Test
    void lockIsPerUser() {
        String a = "alice";
        String b = "bob";
        for (int i = 0; i < 5; i++) {
            service.onFailure(a);
        }
        assertTrue(service.isBlocked(a));
        assertFalse(service.isBlocked(b));
    }

    @Test
    void usernameIsCaseInsensitiveAndTrimmed() {
        for (int i = 0; i < 5; i++) {
            service.onFailure("  Admin ");
        }
        assertTrue(service.isBlocked("admin"));
    }
}
