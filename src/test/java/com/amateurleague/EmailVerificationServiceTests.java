
package com.amateurleague;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:verification-tests;DB_CLOSE_DELAY=-1",
        "server.address=127.0.0.1"
    }
)
class EmailVerificationServiceTests {

    @Autowired
    EmailVerificationService verification;

    @Autowired
    JdbcTemplate db;

    private static final BCryptPasswordEncoder ENCODER =
        new BCryptPasswordEncoder();

    @BeforeEach
    void reset() {
        db.update("DELETE FROM email_verification_codes");
        db.update("DELETE FROM team_memberships");
        db.update("DELETE FROM users");
    }

    private void insertCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        db.update(
            """
            INSERT INTO email_verification_codes
                (id, email, code_hash, display_name, expires_at, attempts, created_at)
            VALUES (?, ?, ?, ?, ?, 0, ?)
            """,
            UUID.randomUUID().toString(),
            email,
            ENCODER.encode(code),
            "Test User",
            now.plusMinutes(10),
            now
        );
    }

    @Test
    void invalidEmailIsRejected() {
        assertThrows(
            ResponseStatusException.class,
            () -> verification.requestCode("not-an-email", "Test User", "password123")
        );
    }

    @Test
    void invalidCodeFormatIsRejected() {
        assertThrows(
            ResponseStatusException.class,
            () -> verification.verifyCode("test@example.com", "123")
        );
    }

    @Test
    void missingCodeIsRejected() {
        assertThrows(
            ResponseStatusException.class,
            () -> verification.verifyCode("test@example.com", "123456")
        );
    }

    @Test
    void correctCodeCreatesUserAccount() {
        insertCode("test@example.com", "123456");

        var user = verification.verifyCode(
            "test@example.com",
            "123456"
        );

        assertEquals("test@example.com", user.email());
        assertEquals("Test User", user.displayName());

        Integer count = db.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?",
            Integer.class,
            "test@example.com"
        );

        assertEquals(1, count);

        Integer remainingCodes = db.queryForObject(
            "SELECT COUNT(*) FROM email_verification_codes WHERE email = ?",
            Integer.class,
            "test@example.com"
        );

        assertEquals(0, remainingCodes);
    }

    @Test
    void incorrectCodeIncrementsAttemptCount() {
        insertCode("test@example.com", "123456");

        assertThrows(
            ResponseStatusException.class,
            () -> verification.verifyCode("test@example.com", "999999")
        );

        Integer attempts = db.queryForObject(
            "SELECT attempts FROM email_verification_codes WHERE email = ?",
            Integer.class,
            "test@example.com"
        );

        assertEquals(1, attempts);
    }
}
