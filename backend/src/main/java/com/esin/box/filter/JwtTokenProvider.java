package com.esin.box.filter;

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
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String REDIS_KEY_PREFIX_ACTIVE_TOKEN = "active_token:";
    private static final String REDIS_KEY_PREFIX_REFRESH_TOKEN = "refresh_token:";
    private static final String REDIS_KEY_PREFIX_BLACKLIST = "token_blacklist:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

     @Value("${jwt.access-token-expiration:15000}") // 15秒，用于测试
    //@Value("${jwt.access-token-expiration:7200000}") // 2小时 访问令牌有效期
    private long accessTokenExpirationInMs;

     @Value("${jwt.refresh-token-expiration:60000}") // 20秒，用于测试
    //@Value("${jwt.refresh-token-expiration:2592000000}") // 30天，毫秒
    private long refreshTokenExpirationInMs;

     @Value("${jwt.refresh-window:10000}") // 10秒，用于测试
    //@Value("${jwt.refresh-window:1800000}") // 30分钟 刷新窗口时间
    private long refreshWindowInMs;

    @Value("${jwt.blacklist-expiration:86400000}") // 24小时，毫秒
    private long blacklistExpirationInMs;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成Access Token，type = access
    public String generateAccessToken(String username) {
        return generateToken(username, "access", accessTokenExpirationInMs);
    }

    // 生成Refresh Token，type = refresh
    public String generateRefreshToken(String username) {
        String token = generateToken(username, "refresh", refreshTokenExpirationInMs);

        // 记录到 Redis，用于管理和清理
        String refreshTokenKey = REDIS_KEY_PREFIX_REFRESH_TOKEN + username + ":" + token;
        redisTemplate.opsForValue().set(refreshTokenKey, "1", refreshTokenExpirationInMs, TimeUnit.MILLISECONDS);

        return token;
    }

    private String generateToken(String username, String type, long expirationMs) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationMs);

            String token = Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .claim("type", type) // 区分token类型
                    .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                    .compact();

            if ("access".equals(type)) {
                addToActiveTokens(username, token, expirationMs);
            }

            return token;
        } catch (Exception e) {
            logger.error("生成Token时发生错误", e);
            throw new JwtException("Token生成失败");
        }
    }

    private void addToActiveTokens(String username, String token, long expirationMs) {
        String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + token;
        redisTemplate.opsForValue().set(activeTokenKey, "1", expirationMs, TimeUnit.MILLISECONDS);
    }

    public String getUsernameFromJWT(String token) {
        try {
            Claims claims = parseClaimsAllowExpired(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("从Token获取用户名时发生错误", e);
            throw new JwtException("无法从Token获取用户信息");
        }
    }

    public String getTokenType(String token) {
        try {
            Claims claims = parseClaimsAllowExpired(token);
            Object type = claims.get("type");
            return type != null ? type.toString() : null;
        } catch (Exception e) {
            logger.error("获取Token类型时发生错误", e);
            return null;
        }
    }

    private Claims parseClaimsAllowExpired(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 校验token（access或refresh）
    public boolean validateToken(String token) {
        try {
            validateTokenBasics(token);
            validateTokenBlacklist(token);

            // 先尝试解析token - 这里可能抛出ExpiredJwtException
            Claims claims;
            boolean isExpired = false;
            try {
                claims = Jwts.parserBuilder()
                        .setSigningKey(getSecretKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (ExpiredJwtException e) {
                isExpired = true;
                claims = e.getClaims();

                // 检查token类型
                String type = getTokenType(token);
                if ("access".equals(type)) {
                    // 如果是access token且在刷新窗口内，继续验证
                    if (isInRefreshWindow(claims.getExpiration())) {
                        logger.info("Access Token过期但处于刷新窗口内: {}", claims.getSubject());
                        // 继续后续验证
                    } else {
                        // 过期且不在刷新窗口内，直接抛出ExpiredJwtException
                        logger.warn("Access Token已过期且超出刷新窗口: {}", e.getMessage());
                        throw e; // 重要：直接抛出ExpiredJwtException
                    }
                } else if ("refresh".equals(type)) {
                    // refresh token过期直接抛出异常
                    logger.warn("Refresh Token已过期: {}", e.getMessage());
                    throw e; // 重要：直接抛出ExpiredJwtException
                }
            }

            // 🔥 关键修复：只对 access token 进行激活状态检查
            String type = getTokenType(token);
            if ("access".equals(type)) {
                validateTokenActive(token, isExpired, claims.getExpiration());
            }
            // refresh token 不需要 Redis 激活状态检查，只需要签名和过期时间验证

            return true;
        } catch (ExpiredJwtException e) {
            // 重要：不要包装ExpiredJwtException，直接向上抛出
            throw e;
        } catch (JwtException e) {
            logger.warn("Token验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Token验证错误", e);
            throw new JwtException("Token验证失败");
        }
    }

    private void validateTokenBasics(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new JwtException("Token为空");
        }
    }

    private void validateTokenBlacklist(String token) {
        String blacklistKey = REDIS_KEY_PREFIX_BLACKLIST + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new JwtException("Token已被禁用");
        }
    }

    private void validateTokenActive(String token, boolean isExpired, Date expiration) {
        try {
            String username = getUsernameFromJWT(token);
            String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + token;
            boolean isActive = Boolean.TRUE.equals(redisTemplate.hasKey(activeTokenKey));

            if (isActive) return;

            long now = System.currentTimeMillis();

            if (!isExpired && expiration != null && expiration.after(new Date())) {
                // token未过期但不在Redis中，重新激活
                redisTemplate.opsForValue().set(activeTokenKey, "1", expiration.getTime() - now, TimeUnit.MILLISECONDS);
                logger.info("Token有效但未在Redis中，已重新激活: {}", username);
                return;
            }

            if (isExpired && isInRefreshWindow(expiration)) {
                // token过期但在刷新窗口内，临时激活
                redisTemplate.opsForValue().set(activeTokenKey, "1", refreshWindowInMs, TimeUnit.MILLISECONDS);
                logger.info("Access Token过期但处于刷新窗口内，已临时激活: {}", username);
                return;
            }

            throw new JwtException("Token未激活或已过期");
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            logger.error("验证Token激活状态时发生错误", e);
            throw new JwtException("Token验证失败");
        }
    }

    // 验证 refresh token 的激活状态（仅在特殊需要时使用）
    private void validateRefreshTokenActive(String token) {
        try {
            String username = getUsernameFromJWT(token);
            String refreshTokenKey = REDIS_KEY_PREFIX_REFRESH_TOKEN + username + ":" + token;

            if (!Boolean.TRUE.equals(redisTemplate.hasKey(refreshTokenKey))) {
                throw new JwtException("Refresh Token未激活或已失效");
            }
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            logger.error("验证Refresh Token激活状态时发生错误", e);
            throw new JwtException("Refresh Token验证失败");
        }
    }

    private boolean isInRefreshWindow(Date expiration) {
        if (expiration == null) return false;
        long expTime = expiration.getTime();
        long now = System.currentTimeMillis();
        return now > expTime && now - expTime <= refreshWindowInMs;
    }

    public boolean shouldRefreshToken(String token) {
        try {
            String type = getTokenType(token);
            if (!"access".equals(type)) return false;

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            return now > expiration.getTime() - refreshWindowInMs;
        } catch (ExpiredJwtException e) {
            Date expiration = e.getClaims().getExpiration();
            return isInRefreshWindow(expiration);
        } catch (Exception e) {
            logger.warn("检查Token是否需要刷新失败", e);
            return false;
        }
    }

    // 刷新token，旧token必须是access token且处于刷新窗口内
    public String refreshToken(String oldToken) {
        try {
            String type = getTokenType(oldToken);
            if (!"access".equals(type)) {
                logger.warn("尝试刷新非access token，拒绝操作");
                return null;
            }

            String username = getUsernameFromJWT(oldToken);
            if (username == null) return null;

            String blacklistKey = REDIS_KEY_PREFIX_BLACKLIST + oldToken;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) return null;

            Claims claims = parseClaimsAllowExpired(oldToken);
            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            boolean inRefreshWindow = now > expiration.getTime() &&
                    now - expiration.getTime() <= refreshWindowInMs;

            if (inRefreshWindow) {
                String newToken = generateAccessToken(username);
                if (newToken != null) {
                    // 旧token延迟失效
                    String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + oldToken;
                    redisTemplate.expire(activeTokenKey, 60, TimeUnit.SECONDS);
                    logger.info("Access Token已刷新，旧Token将在60秒后失效: {}", username);
                    return newToken;
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("刷新Token时发生错误", e);
            return null;
        }
    }

    // 增强的 token 失效方法，处理不同类型的 token
    public void invalidateToken(String token) {
        if (token == null || token.trim().isEmpty()) return;

        try {
            String username = getUsernameFromJWT(token);
            String tokenType = getTokenType(token);

            if (username == null) return;

            if ("access".equals(tokenType)) {
                // 处理 access token
                String activeTokenKey = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":" + token;
                redisTemplate.delete(activeTokenKey);
            } else if ("refresh".equals(tokenType)) {
                // 处理 refresh token
                String refreshTokenKey = REDIS_KEY_PREFIX_REFRESH_TOKEN + username + ":" + token;
                redisTemplate.delete(refreshTokenKey);
            }

            // 加入黑名单
            String blacklistKey = REDIS_KEY_PREFIX_BLACKLIST + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", blacklistExpirationInMs, TimeUnit.MILLISECONDS);

            logger.info("Token已成功禁用: username={}, type={}", username, tokenType);
        } catch (Exception e) {
            logger.error("禁用Token时发生错误: {}", e.getMessage());
        }
    }

    // 清理用户的所有 token
    public void invalidateAllUserTokens(String username) {
        try {
            // 清理所有 active tokens
            String activePattern = REDIS_KEY_PREFIX_ACTIVE_TOKEN + username + ":*";
            Set<String> activeKeys = redisTemplate.keys(activePattern);
            if (activeKeys != null && !activeKeys.isEmpty()) {
                redisTemplate.delete(activeKeys);
                logger.info("已清理用户的 {} 个 access token: {}", activeKeys.size(), username);
            }

            // 清理所有 refresh tokens
            String refreshPattern = REDIS_KEY_PREFIX_REFRESH_TOKEN + username + ":*";
            Set<String> refreshKeys = redisTemplate.keys(refreshPattern);
            if (refreshKeys != null && !refreshKeys.isEmpty()) {
                redisTemplate.delete(refreshKeys);
                logger.info("已清理用户的 {} 个 refresh token: {}", refreshKeys.size(), username);
            }

            logger.info("已清理用户所有Token: {}", username);
        } catch (Exception e) {
            logger.error("清理用户Token时发生错误: username={}, error={}", username, e.getMessage());
        }
    }
}