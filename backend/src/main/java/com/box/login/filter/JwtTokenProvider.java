package com.box.login.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    private static final String REDIS_KEY_PREFIX_ACTIVE_TOKEN = "active_token:";
    private static final String REDIS_KEY_PREFIX_BLACKLIST = "token_blacklist:";
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:7200000}")  // 默认2小时
    private long jwtExpirationInMs;
    
    @Value("${jwt.refresh-window:1800000}")  // 默认30分钟
    private long refreshWindowInMs;
    
    @Value("${jwt.blacklist-expiration:86400000}")  // 默认24小时
    private long blacklistExpirationInMs;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
            
            String token = Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                    .compact();
            
            addToActiveTokens(username, token);
            return token;
        } catch (Exception e) {
            logger.error("生成Token时发生错误", e);
            throw new JwtException("Token生成失败");
        }
    }

    private void addToActiveTokens(String username, String token) {
        String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + token;
        redisTemplate.opsForValue().set(activeTokenKey, "1", jwtExpirationInMs, TimeUnit.MILLISECONDS);
    }

    public String getUsernameFromJWT(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            // 如果是因为过期导致的异常，仍然可以从过期的token中获取用户名
            return e.getClaims().getSubject();
        } catch (Exception e) {
            logger.error("从Token获取用户名时发生错误", e);
            throw new JwtException("无法从Token获取用户信息");
        }
    }

    public boolean validateToken(String authToken) {
        try {
            validateTokenBasics(authToken);
            validateTokenBlacklist(authToken);
            validateTokenActive(authToken);
            validateTokenSignature(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            // 如果token已过期但在刷新窗口期内，我们仍然认为它是有效的
            if (isInRefreshWindow(e.getClaims().getExpiration())) {
                logger.info("Token已过期但在刷新窗口期内");
                return true;
            }
            logger.warn("Token已过期且超出刷新窗口期: {}", e.getMessage());
            throw new JwtException("Token已过期", e);
        } catch (JwtException e) {
            logger.warn("Token验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Token验证过程中发生错误", e);
            throw new JwtException("Token验证失败");
        }
    }

    private boolean isInRefreshWindow(Date expiration) {
        if (expiration == null) return false;
        long expirationTime = expiration.getTime();
        long currentTime = System.currentTimeMillis();
        // 如果当前时间在过期时间和刷新窗口时间之内，则允许刷新
        return currentTime > expirationTime && 
               currentTime - expirationTime <= refreshWindowInMs;
    }

    private void validateTokenBasics(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            throw new JwtException("Token为空");
        }
    }

    private void validateTokenBlacklist(String authToken) {
        String blacklistKey = REDIS_KEY_PREFIX_BLACKLIST + authToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new JwtException("Token已被禁用");
        }
    }

    private void validateTokenActive(String authToken) {
        try {
            String username = getUsernameFromJWT(authToken);
            String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + authToken;
            
            // 检查token是否在Redis中
            boolean isActiveInRedis = Boolean.TRUE.equals(redisTemplate.hasKey(activeTokenKey));
            
            // 如果token在Redis中，直接通过
            if (isActiveInRedis) {
                return;
            }
            
            try {
                // 验证JWT的有效性
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSecretKey())
                        .build()
                        .parseClaimsJws(authToken)
                        .getBody();
                
                // 如果JWT未过期但不在Redis中，说明可能是Redis数据丢失，重新激活
                if (claims.getExpiration().after(new Date())) {
                    redisTemplate.opsForValue().set(activeTokenKey, "1", 
                            claims.getExpiration().getTime() - System.currentTimeMillis(), 
                            TimeUnit.MILLISECONDS);
                    logger.info("Token有效但未在Redis中，已重新激活: {}", username);
                    return;
                }
            } catch (ExpiredJwtException e) {
                // 检查是否在刷新窗口期内
                if (isInRefreshWindow(e.getClaims().getExpiration())) {
                    redisTemplate.opsForValue().set(activeTokenKey, "1", 
                            refreshWindowInMs, 
                            TimeUnit.MILLISECONDS);
                    logger.info("过期Token在刷新窗口期内，已临时激活: {}", username);
                    return;
                }
                throw new JwtException("Token已过期且超出刷新窗口期");
            }
            
            throw new JwtException("Token未激活或已过期");
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            logger.error("验证Token激活状态时发生错误", e);
            throw new JwtException("Token验证失败");
        }
    }

    private void validateTokenSignature(String authToken) {
        Jwts.parserBuilder()
            .setSigningKey(getSecretKey())
            .build()
            .parseClaimsJws(authToken);
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
            
            return now.getTime() > expiration.getTime() - refreshWindowInMs;
        } catch (Exception e) {
            logger.warn("检查Token是否需要刷新时发生错误", e);
            return false;
        }
    }

    public String refreshToken(String oldToken) {
        try {
            String username = getUsernameFromJWT(oldToken);
            if (username == null) {
                return null;
            }

            // 检查token是否在黑名单中
            String blacklistKey = REDIS_KEY_PREFIX_BLACKLIST + oldToken;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                return null;
            }

            // 判断token是否需要刷新
            boolean needsRefresh = false;
            try {
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(oldToken)
                    .getBody();
                    
                needsRefresh = claims.getExpiration().getTime() - System.currentTimeMillis() <= refreshWindowInMs;
            } catch (ExpiredJwtException e) {
                // 如果token已经过期，检查是否在刷新窗口期内
                needsRefresh = isInRefreshWindow(e.getClaims().getExpiration());
            } catch (Exception e) {
                return null;
            }

            if (needsRefresh) {
                // 生成新token
                String newToken = generateToken(username);
                if (newToken != null) {
                    // 将旧token标记为即将过期，但不立即失效
                    String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + oldToken;
                    redisTemplate.expire(activeTokenKey, 60, TimeUnit.SECONDS); // 给1分钟的过渡期
                    logger.info("Token已刷新，旧Token将在60秒后失效: {}", username);
                    return newToken;
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("刷新Token时发生错误", e);
            return null;
        }
    }

    public void invalidateToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            try {
                String username = getUsernameFromJWT(token);
                if (username != null) {
                    // 先从活跃token集合中移除
                    String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + token;
                    redisTemplate.delete(activeTokenKey);
                    
                    // 再加入黑名单
                    String blacklistKey = REDIS_KEY_PREFIX_BLACKLIST + token;
                    redisTemplate.opsForValue().set(blacklistKey, "1", blacklistExpirationInMs, TimeUnit.MILLISECONDS);
                    
                    logger.info("Token已成功禁用: {}", username);
                }
            } catch (Exception e) {
                logger.error("禁用Token时发生错误: {}", e.getMessage());
            }
        }
    }
}
