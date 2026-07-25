package com.example.myblog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DjangoPasswordUtilTests {

    @Test
    void encodedPasswordCanBeVerified() {
        String encoded = DjangoPasswordUtil.encode("springboot17");
        assertTrue(DjangoPasswordUtil.matches("springboot17", encoded));
        assertFalse(DjangoPasswordUtil.matches("wrong-password", encoded));
    }

    @Test
    void invalidDjangoHashReturnsFalse() {
        assertFalse(DjangoPasswordUtil.matches("password", "not-a-django-hash"));
    }

    @Test
    void matchesPasswordVectorCalculatedByPythonDjangoAlgorithm() {
        String pythonGenerated = "pbkdf2_sha256$390000$FixedSalt123$"
                + "kALyJDtJu2qnKQf+DgI+RkDjfVbelIsjWKm4NWeHA2s=";
        assertTrue(DjangoPasswordUtil.matches("django-test", pythonGenerated));
        assertFalse(DjangoPasswordUtil.matches("other", pythonGenerated));
    }
}
