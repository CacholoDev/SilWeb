package com.silvaldeweb.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String DEFAULT_DEV_SECRET = "change_me_dev_jwt_secret_for_silvalde_web";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(
            @Value("${app.security.jwt.secret:}") String secret,
            @Value("${app.security.jwt.expiration-minutes:120}") long expirationMinutes,
            @Value("${app.security.jwt.issuer:silvalde-web}") String issuer) {
        this.signingKey = buildSigningKey(secret);
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    private SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is required. Set APP_JWT_SECRET (or app.security.jwt.secret) "
                            + "to a random string of at least " + MIN_SECRET_BYTES + " bytes. "
                            + "Generate one with: openssl rand -base64 64");
        }
        if (DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT secret is still the development default (change_me_dev_jwt_secret_for_silvalde_web). "
                            + "This is unsafe. Set APP_JWT_SECRET in your .env to a random string of at least "
                            + MIN_SECRET_BYTES + " bytes. Generate one with: openssl rand -base64 64");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret is too short (" + keyBytes.length + " bytes). HS256 requires at least "
                            + MIN_SECRET_BYTES + " bytes. Generate one with: openssl rand -base64 64");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationMinutes * 60L);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .issuer(issuer)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("roles", roles)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Object rolesClaim = parseToken(token).get("roles");
        if (rolesClaim instanceof List<?> roleList) {
            return roleList.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parseToken(token);
        return claims.getSubject().equals(userDetails.getUsername()) && claims.getExpiration().after(new Date());
    }
}
