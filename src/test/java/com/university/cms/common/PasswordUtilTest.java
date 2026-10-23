package com.university.cms.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    private final PasswordUtil passwordUtil = new PasswordUtil();

    @Test
    void encodedPasswordShouldMatchRaw() {
        String hash = passwordUtil.encode("abc12345");
        assertNotEquals("abc12345", hash);
        assertTrue(passwordUtil.matches("abc12345", hash));
        assertFalse(passwordUtil.matches("abc12346", hash));
    }

    @Test
    void legacyPlainTextPasswordShouldStillMatch() {
        assertTrue(passwordUtil.matches("123456", "123456"));
        assertFalse(passwordUtil.matches("123456", "654321"));
    }

    @Test
    void hashDetection() {
        assertTrue(passwordUtil.isHashed(passwordUtil.encode("x")));
        assertFalse(passwordUtil.isHashed("123456"));
        assertFalse(passwordUtil.isHashed(null));
    }

    @Test
    void nullInputsAreRejected() {
        assertFalse(passwordUtil.matches(null, "123456"));
        assertFalse(passwordUtil.matches("123456", null));
    }
}
