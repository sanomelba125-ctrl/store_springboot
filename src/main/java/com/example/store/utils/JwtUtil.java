package com.example.store.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 *
 * 负责 JWT 的生成、解析、校验。
 * 每个 token 携带：userId、userType、jti（唯一ID，用于黑名单）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // 毫秒

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT
     *
     * @param userId   用户 ID
     * @param userType 用户类型（1=超级管理员, 2=信息管理员, 3=前端用户）
     * @return JWT 字符串
     */
    public String generateToken(String userId, Integer userType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())   // jti：JWT 唯一标识，用于黑名单
                .subject(userId)                     // sub：用户 ID
                .claim("type", userType)             // 自定义 claim：用户角色
                .issuedAt(now)                       // iat：签发时间
                .expiration(expiryDate)              // exp：过期时间
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT，返回 Claims（包含所有载荷信息）
     * 如果 token 无效或已过期，会抛出 JwtException
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 中提取用户 ID
     */
    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 token 中提取用户类型
     */
    public Integer getUserType(String token) {
        return parseToken(token).get("type", Integer.class);
    }

    /**
     * 从 token 中提取 JTI（JWT 唯一标识）
     */
    public String getJti(String token) {
        return parseToken(token).getId();
    }

    /**
     * 获取 token 的剩余有效时间（毫秒）
     * 用于设置黑名单的 TTL
     */
    public long getRemainingExpiration(String token) {
        Date expiry = parseToken(token).getExpiration();
        long remaining = expiry.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }
}
