package com.ainovel.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PowService {
    private static final Duration TOKEN_MAX_AGE = Duration.ofMinutes(10);
    private static final Duration TOKEN_MAX_FUTURE_SKEW = Duration.ofSeconds(30);
    private static final Duration REPLAY_TTL = Duration.ofMinutes(15);
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 6;

    public record PowToken(String email, long ts, long nonce, int difficulty, String hash) {}

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public boolean verify(String email, String token) {
        if (email == null || email.isBlank()) return false;
        PowToken payload = parse(token);
        if (payload == null) return false;

        if (payload.email() == null || !payload.email().equalsIgnoreCase(email)) return false;
        if (payload.hash() == null || payload.hash().isBlank()) return false;

        int difficulty = payload.difficulty();
        if (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY) return false;

        Instant now = Instant.now();
        Instant ts = Instant.ofEpochMilli(payload.ts());
        if (ts.isBefore(now.minus(TOKEN_MAX_AGE)) || ts.isAfter(now.plus(TOKEN_MAX_FUTURE_SKEW))) return false;

        String expected = sha256Hex(payload.email() + "|" + payload.ts() + "|" + payload.nonce());
        if (!expected.equalsIgnoreCase(payload.hash())) return false;
        if (!expected.startsWith("0".repeat(difficulty))) return false;

        return markUsed(expected);
    }

    private PowToken parse(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(token);
            return objectMapper.readValue(decoded, PowToken.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private boolean markUsed(String hash) {
        if (stringRedisTemplate == null) return true;
        String key = "pow:used:" + hash;
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", REPLAY_TTL);
            return Boolean.TRUE.equals(ok);
        } catch (DataAccessException ignored) {
            return true;
        }
    }
}

