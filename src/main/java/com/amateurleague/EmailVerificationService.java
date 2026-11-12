
package com.amateurleague;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {

    public record VerifiedUser(String id, String email, String displayName) {}

    private record StoredCode(
    String hash,
    String displayName,
    String passwordHash,
    String accountRole,
    LocalDateTime expiresAt,
    int attempts
) {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_EXPIRATION_MINUTES = 10;
    private static final int RESEND_DELAY_SECONDS = 60;
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate db;
    private final boolean logCodes;
    private final Set<String> commissionerEmails;

    public EmailVerificationService(
        JdbcTemplate db,
        @Value("${app.email-verification.log-code:false}") boolean logCodes,
        @Value("${app.commissioner-emails:}") String commissionerEmails
    ) {
        this.db = db;
        this.logCodes = logCodes;
        this.commissionerEmails = java.util.Arrays.stream(commissionerEmails.split(","))
            .map(String::trim)
            .filter(email -> !email.isEmpty())
            .map(email -> email.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public void requestCode(String rawEmail, String rawDisplayName, String rawPassword) {
        String email = normalizeEmail(rawEmail);
        String displayName = rawDisplayName == null ? "" : rawDisplayName.trim();

        if (displayName.isBlank() || displayName.length() > 60) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Display name must contain between 1 and 60 characters."
            );
        }

        if (rawPassword == null || rawPassword.length() < 8 || rawPassword.length() > 72) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Password must contain between 8 and 72 characters."
            );
    }

    String passwordHash = ENCODER.encode(rawPassword);

        LocalDateTime now = LocalDateTime.now(UTC);

        List<LocalDateTime> recentCodes = db.query(
            "SELECT created_at FROM email_verification_codes WHERE email = ? ORDER BY created_at DESC LIMIT 1",
            (rs, row) -> rs.getTimestamp("created_at").toLocalDateTime(),
            email
        );

        if (!recentCodes.isEmpty()
            && recentCodes.getFirst().isAfter(now.minusSeconds(RESEND_DELAY_SECONDS))) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Please wait before requesting another verification code."
            );
        }

        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        String hash = ENCODER.encode(code);

        db.update("DELETE FROM email_verification_codes WHERE email = ?", email);

        db.update(
            """
            INSERT INTO email_verification_codes
            (id, email, code_hash, display_name, password_hash, account_role, expires_at, attempts, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)
            """,
            UUID.randomUUID().toString(),
            email,
            hash,
            displayName,
            passwordHash,
            "PLAYER",
            now.plusMinutes(CODE_EXPIRATION_MINUTES),
            now
        );

        if (logCodes) {
            System.out.println("Development verification code for " + email + ": " + code);
        }
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public VerifiedUser verifyCode(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);

        if (code == null || !code.matches("\\d{6}")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Enter a valid six-digit verification code."
            );
        }

        List<StoredCode> rows = db.query(
            """
            SELECT code_hash, display_name, password_hash, account_role, expires_at, attempts
            FROM email_verification_codes
            WHERE email = ?
            ORDER BY created_at DESC
            LIMIT 1
            """,
            (rs, row) -> new StoredCode(
                rs.getString("code_hash"),
                rs.getString("display_name"),
                rs.getString("password_hash"),
                rs.getString("account_role"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getInt("attempts")
            ),
            email
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No active verification code exists. Request a new one."
            );
        }

        StoredCode stored = rows.getFirst();

        if (stored.expiresAt().isBefore(LocalDateTime.now(UTC))) {
            db.update("DELETE FROM email_verification_codes WHERE email = ?", email);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Your verification code has expired. Request a new one."
            );
        }

        if (stored.attempts() >= MAX_ATTEMPTS) {
            db.update("DELETE FROM email_verification_codes WHERE email = ?", email);
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many incorrect attempts. Request a new code."
            );
        }

        if (!ENCODER.matches(code, stored.hash())) {
            db.update(
                "UPDATE email_verification_codes SET attempts = attempts + 1 WHERE email = ?",
                email
            );
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Incorrect verification code."
            );
        }

        List<VerifiedUser> existingUsers = db.query(
            "SELECT id, email, display_name FROM users WHERE email = ?",
            (rs, row) -> new VerifiedUser(
                rs.getString("id"),
                rs.getString("email"),
                rs.getString("display_name")
            ),
            email
        );

        if (!existingUsers.isEmpty()) {
            db.update("DELETE FROM email_verification_codes WHERE email = ?", email);
            return existingUsers.getFirst();
        }

        VerifiedUser user = new VerifiedUser(
            UUID.randomUUID().toString(),
            email,
            stored.displayName()
        );

        db.update(
            "INSERT INTO users (id, email, display_name, password_hash, account_role, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            user.id(),
            user.email(),
            user.displayName(),
            stored.passwordHash(),
            commissionerEmails.contains(user.email()) ? "COMMISSIONER" : stored.accountRole(),
            LocalDateTime.now(UTC)
        );

        db.update("DELETE FROM email_verification_codes WHERE email = ?", email);

        return user;
    }

    private String normalizeEmail(String rawEmail) {
        String email = rawEmail == null
            ? ""
            : rawEmail.trim().toLowerCase(Locale.ROOT);

        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Enter a valid email address."
            );
        }

        return email;
    }
}
