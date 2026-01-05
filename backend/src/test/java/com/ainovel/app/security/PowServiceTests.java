package com.ainovel.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PowServiceTests {
    @Autowired
    private PowService powService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void verifiesValidToken() throws Exception {
        String email = "user@example.com";
        String token = makeToken(email, 2, Instant.now().toEpochMilli());
        assertTrue(powService.verify(email, token));
    }

    @Test
    void rejectsDifferentEmail() throws Exception {
        String token = makeToken("a@example.com", 2, Instant.now().toEpochMilli());
        assertFalse(powService.verify("b@example.com", token));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String email = "user@example.com";
        long oldTs = Instant.now().minusSeconds(60 * 30).toEpochMilli();
        String token = makeToken(email, 2, oldTs);
        assertFalse(powService.verify(email, token));
    }

    private String makeToken(String email, int difficulty, long ts) throws Exception {
        String prefix = "0".repeat(difficulty);
        long nonce = 0;
        String hash;
        while (true) {
            hash = sha256Hex(email + "|" + ts + "|" + nonce);
            if (hash.startsWith(prefix)) break;
            nonce++;
            if (nonce > 2_000_000) throw new IllegalStateException("failed to find pow nonce");
        }
        String json = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "ts", ts,
                "nonce", nonce,
                "difficulty", difficulty,
                "hash", hash
        ));
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }
}
