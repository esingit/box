package com.box.login.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    // token 的过期时间 2小时
    private final long jwtExpirationInMs = 2 * 60 * 60 * 1000; // 2小时
    
    // token 的刷新时间窗口 30分钟
    private final long refreshWindowInMs = 30 * 60 * 1000; // 30分钟
    
    // token黑名单过期时间 24小时
    private final long blacklistExpirationInMs = 24 * 60 * 60 * 1000; // 24小时

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
        
        // 将token加入活跃token集合
        String activeTokenKey = "active_token:" + username + ":" + token;
        redisTemplate.opsForValue().set(activeTokenKey, "1", jwtExpirationInMs, TimeUnit.MILLISECONDS);
        
        return token;
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            if (authToken == null || authToken.trim().isEmpty()) {
                throw new JwtException("Token is empty");
            }
            
            // 检查 token 是否在黑名单中
            String blacklistKey = "token_blacklist:" + authToken;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                throw new JwtException("Token已失效");
            }
            
            // 解析token获取用户名
            String username = getUsernameFromJWT(authToken);
            
            // 检查token是否在活跃token集合中
            String activeTokenKey = "active_token:" + username + ":" + authToken;
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(activeTokenKey))) {
                throw new JwtException("Token未激活或已过期");
            }
            
            // 验证token签名和过期时间
            Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(authToken);
            
            // 检查是否需要刷新
            boolean shouldRefresh = shouldRefreshToken(authToken);
            
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token已过期", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Token无效", e);
        }
    }

    public boolean shouldRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Date expiration = claims.getExpiration();
            Date now = new Date();
            
            // 如果 token 在刷新窗口期内（距离过期小于 refreshWindowInMs）
            return now.getTime() > expiration.getTime() - refreshWindowInMs;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public String refreshToken(String oldToken) {
        try {
            // 验证旧token
            if (!validateToken(oldToken)) {
                return null;
            }
            
            // 获取用户名
            String username = getUsernameFromJWT(oldToken);
            
            // 将旧token加入黑名单
            invalidateToken(oldToken);
            
            // 生成新token
            return generateToken(username);
        } catch (Exception e) {
            return null;
        }
    }

    public void invalidateToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            // 加入黑名单
            String blacklistKey = "token_blacklist:" + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", blacklistExpirationInMs, TimeUnit.MILLISECONDS);
            
            try {
                // 从活跃token集合中移除
                String username = getUsernameFromJWT(token);
                String activeTokenKey = "active_token:" + username + ":" + token;
                redisTemplate.delete(activeTokenKey);
            } catch (Exception e) {
                // 即使获取用户名失败，token仍然会被加入黑名单
                System.err.println("从活跃token集合移除失败: " + e.getMessage());
            }
        }
    }

    public void invalidateAllUserTokens(String username) {
        try {
            // 查找并废止该用户的所有活跃token
            String pattern = "active_token:" + username + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    String token = key.substring(key.lastIndexOf(":") + 1);
                    invalidateToken(token);
                }
            }
        } catch (Exception e) {
            System.err.println("废止用户所有token失败: " + e.getMessage());
        }
    }
}
