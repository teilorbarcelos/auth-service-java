package com.app.infrastructure.auth;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.jboss.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * JWT service for token creation, validation, and Redis session management.
 * Equivalent to JwtService.php
 */
@ApplicationScoped
public class JwtService {
    private static final String SESSION_PREFIX = "session:user:";
    private static final long SESSION_TTL = 7L * 24L * 3600L;

    private static final Logger LOG = Logger.getLogger(JwtService.class);

    @ConfigProperty(name = "app.jwt.secret")
    String jwtSecret;

    @ConfigProperty(name = "app.jwt.expiration", defaultValue = "3600")
    long jwtExpiration;

    @ConfigProperty(name = "app.jwt.issuer", defaultValue = "http://localhost:8888")
    String issuer;

    @Inject
    RedisDataSource redisDataSource;

    private JwtConsumer jwtConsumer;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        HmacKey hmacKey = new HmacKey(secretBytes);
        this.jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer(issuer)
                .setExpectedAudience(issuer)
                .setVerificationKey(hmacKey)
                .setJwsAlgorithmConstraints(org.jose4j.jwa.AlgorithmConstraints.ConstraintType.PERMIT,
                        org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA256)
                .build();
    }

    private void ensureInitialized() {
        if (signingKey == null) {
            init();
        }
    }

    /**
     * Create a JWT token with custom claims.
     */
    public String createToken(String userId, Map<String, Object> claims, long expiresInSeconds) {
        ensureInitialized();
        var builder = Jwt.issuer(issuer)
                .audience(issuer)
                .subject(userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(expiresInSeconds))
                .claim("uid", userId);

        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }

        return builder.sign(signingKey);
    }

    /**
     * Create an access + refresh token pair.
     */
    public Map<String, String> createTokenPair(String userId, Map<String, Object> claims) {
        String token = createToken(userId, claims, jwtExpiration);
        String refreshToken = createToken(userId, claims, 7 * 24 * 3600);

        return Map.of(
                "token", token,
                "refreshToken", refreshToken);
    }

    /**
     * Validate a JWT token and return its claims.
     */
    public Map<String, Object> validateToken(String token) {
        ensureInitialized();
        try {
            if (token == null || token.isBlank())
                return null;

            JwtClaims jwtClaims = jwtConsumer.processToClaims(token);

            Map<String, Object> claims = new HashMap<>();
            claims.put("uid", jwtClaims.getClaimValue("uid"));
            claims.put("email", jwtClaims.getClaimValue("email"));
            claims.put("roleId", jwtClaims.getClaimValue("roleId"));
            claims.put("permissions", jwtClaims.getClaimValue("permissions"));
            claims.put("sv", jwtClaims.getClaimValue("sv"));

            return claims;
        } catch (Exception e) {
            LOG.warnv("JwtService: Token validation failed for token: {0}...", token.substring(0, Math.min(token.length(), 10)));
            return null;
        }
    }

    /**
     * Write session version to Redis for session validation.
     */
    public void saveSessionVersion(String userId, long version) {
        ValueCommands<String, String> values = redisDataSource.value(String.class, String.class);
        values.setex(SESSION_PREFIX + userId, SESSION_TTL, String.valueOf(version));
    }

    /**
     * Read current session version from Redis.
     * Returns -1 if no session exists (invalid/expired).
     */
    public long getSessionVersion(String userId) {
        ValueCommands<String, String> values = redisDataSource.value(String.class, String.class);
        String version = values.get(SESSION_PREFIX + userId);
        return version != null ? Long.parseLong(version) : -1;
    }

    /**
     * Delete session version key (invalidates all sessions for the user).
     */
    public void deleteSessionVersion(String userId) {
        KeyCommands<String> keys = redisDataSource.key(String.class);
        keys.del(SESSION_PREFIX + userId);
    }
}
