package com.esin.box.controller;

import com.esin.box.dto.LoginRequest;
import com.esin.box.dto.RegisterRequest;
import com.esin.box.dto.ResetPasswordRequest;
import com.esin.box.entity.User;
import com.esin.box.filter.JwtTokenProvider;
import com.esin.box.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // 防并发刷新的缓存
    private final Map<String, Long> refreshingUsers = new ConcurrentHashMap<>();

    // 注册保持不变
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String registerKey = "register:" + ip;

        Long registerCount = redisTemplate.opsForValue().increment(registerKey);
        if (registerCount != null && registerCount == 1) {
            redisTemplate.expire(registerKey, 24, TimeUnit.HOURS);
        }

        if (registerCount != null && registerCount > 1) {
            String captchaId = request.getCaptchaId();
            String captchaKey = "captcha:" + captchaId;
            String savedCaptcha = redisTemplate.opsForValue().get(captchaKey);
            if (captchaId == null) {
                return Result.<User>error("请输入验证码", true);
            }
            if (savedCaptcha == null) {
                return Result.<User>error("验证码已过期", true);
            }
            if (!savedCaptcha.equalsIgnoreCase(request.getCaptcha())) {
                return Result.<User>error("验证码错误", true);
            }
            redisTemplate.delete(captchaKey);
        }

        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());

            boolean registered = userService.register(user);
            if (registered) {
                logger.info("用户注册成功: username={}, ip={}", user.getUsername(), ip);
                return Result.success(user);
            } else {
                return Result.<User>error("用户名已存在");
            }
        } catch (Exception e) {
            if (registerCount != null) {
                redisTemplate.opsForValue().decrement(registerKey);
            }
            logger.error("用户注册失败: username={}, error={}", request.getUsername(), e.getMessage());
            return Result.<User>error(e.getMessage());
        }
    }

    // 登录改造：返回accessToken和refreshToken
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername();
        String password = request.getPassword();
        String ip = getClientIp(httpRequest);

        String loginFailKey = "login_fail:" + username;
        String captchaId = request.getCaptchaId();
        String captchaKey = "captcha:" + captchaId;

        Integer failCount = redisTemplate.opsForValue().get(loginFailKey) != null ?
                Integer.parseInt(redisTemplate.opsForValue().get(loginFailKey)) : 0;

        boolean captchaRequired = failCount >= 3;

        String captcha = request.getCaptcha();
        if (captchaRequired || captcha != null) {
            String savedCaptcha = redisTemplate.opsForValue().get(captchaKey);
            if (captchaId == null) {
                return Result.<Map<String, String>>error("请输入验证码", true);
            }
            if (savedCaptcha == null) {
                return Result.<Map<String, String>>error("验证码已过期", true);
            }
            if (!savedCaptcha.equalsIgnoreCase(captcha)) {
                incrementLoginFailCount(username, loginFailKey, failCount);
                redisTemplate.delete(captchaKey);
                return Result.<Map<String, String>>error("验证码错误", true);
            }
            redisTemplate.delete(captchaKey);
        }

        // 先查用户
        User user = userService.findByUsername(username);
        if (user == null || !userService.checkPassword(user, password)) {
            incrementLoginFailCount(username, loginFailKey, failCount);
            boolean needsCaptchaAfterFail = redisTemplate.opsForValue().get(loginFailKey) != null &&
                    Integer.parseInt(redisTemplate.opsForValue().get(loginFailKey)) >= 3;
            logger.warn("用户登录失败: username={}, ip={}", username, ip);
            return Result.error("用户名或密码错误", needsCaptchaAfterFail);
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(loginFailKey);

        // 生成token对
        String accessToken = jwtTokenProvider.generateAccessToken(username);
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        logger.info("用户登录成功: username={}, ip={}", username, ip);
        return Result.success(tokens);
    }

    // 增强的 refresh-token 接口
    @PostMapping("/refresh-token")
    public Result<Map<String, String>> refreshToken(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return Result.error("Refresh Token不能为空");
        }

        String username = null;
        String ip = getClientIp(httpRequest);

        try {
            username = jwtTokenProvider.getUsernameFromJWT(refreshToken);

            // 防止同一用户并发刷新
            Long lastRefreshTime = refreshingUsers.get(username);
            long now = System.currentTimeMillis();
            if (lastRefreshTime != null && now - lastRefreshTime < 1000) {
                logger.warn("用户刷新Token过于频繁: username={}, ip={}", username, ip);
                return Result.error("刷新过于频繁，请稍后重试");
            }

            refreshingUsers.put(username, now);

            // 🔥 修改：先验证但不立即失效
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return Result.error("Refresh Token无效");
            }

            String type = jwtTokenProvider.getTokenType(refreshToken);
            if (!"refresh".equals(type)) {
                return Result.error("非法的Token类型");
            }

            if (username == null) {
                return Result.error("无法获取用户信息");
            }

            // 生成新的 token 对
            String newAccessToken = jwtTokenProvider.generateAccessToken(username);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            // 🔥 修改：在成功生成新token后再失效旧token
            jwtTokenProvider.invalidateToken(refreshToken);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);

            logger.info("Token刷新成功: username={}, ip={}", username, ip);
            return Result.success(tokens);

        } catch (Exception e) {
            logger.warn("刷新Token失败: username={}, ip={}, error={}", username, ip, e.getMessage());
            return Result.error("刷新Token失败: " + e.getMessage());
        } finally {
            if (username != null) {
                refreshingUsers.remove(username);
            }
        }
    }

    // 其他接口保持不变，只校验accessToken即可
    @GetMapping("/profile")
    public Result<User> getProfile(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null) {
                return Result.error("未登录或Token缺失");
            }

            if (!jwtTokenProvider.validateToken(token)) {
                return Result.error("Token无效或已过期");
            }

            String tokenType = jwtTokenProvider.getTokenType(token);
            if (!"access".equals(tokenType)) {
                return Result.error("无效的Token类型");
            }

            String username = jwtTokenProvider.getUsernameFromJWT(token);
            if (username == null) {
                return Result.error("无法解析用户信息");
            }

            User user = userService.findByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }

            user.setPassword(null);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    // 增强的登出接口，清理所有用户token
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            String ip = getClientIp(request);

            if (token != null) {
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        String tokenType = jwtTokenProvider.getTokenType(token);
                        if (!"access".equals(tokenType)) {
                            return Result.error("无效的Token类型");
                        }
                        String username = jwtTokenProvider.getUsernameFromJWT(token);

                        // 清理用户的所有 token
                        jwtTokenProvider.invalidateAllUserTokens(username);

                        String lastLogoutKey = "last_logout:" + username;
                        redisTemplate.opsForValue().set(lastLogoutKey, String.valueOf(System.currentTimeMillis()));

                        logger.info("用户登出成功: username={}, ip={}", username, ip);
                        return Result.success();
                    }
                } catch (JwtException e) {
                    logger.info("用户登出（token已无效）: ip={}", ip);
                    return Result.success();
                }
            }
            return Result.success();
        } catch (Exception e) {
            logger.error("登出过程发生错误: {}", e.getMessage());
            return Result.error("登出过程发生错误");
        }
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // 1. 查找用户
            User user = userService.findByUsername(request.getUsername());
            if (user == null) {
                return Result.error("用户不存在");
            }
            // 2. 校验旧密码
            if (!userService.checkPassword(user, request.getOldPassword())) {
                return Result.error("旧密码错误");
            }
            // 3. 更新新密码
            boolean updated = userService.updatePassword(user, request.getNewPassword());
            if (updated) {
                return Result.success();
            } else {
                return Result.error("密码重置失败");
            }
        } catch (Exception e) {
            return Result.error("服务器异常: " + e.getMessage());
        }
    }

    // 添加调试接口
    @GetMapping("/token-stats")
    public Result<Map<String, Object>> getTokenStats(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            Map<String, Object> stats = new HashMap<>();

            if (token != null) {
                try {
                    String username = jwtTokenProvider.getUsernameFromJWT(token);
                    String tokenType = jwtTokenProvider.getTokenType(token);
                    boolean isValid = jwtTokenProvider.validateToken(token);
                    boolean shouldRefresh = jwtTokenProvider.shouldRefreshToken(token);

                    stats.put("hasToken", true);
                    stats.put("username", username);
                    stats.put("tokenType", tokenType);
                    stats.put("isValid", isValid);
                    stats.put("shouldRefresh", shouldRefresh);

                } catch (Exception e) {
                    stats.put("tokenError", e.getMessage());
                    stats.put("tokenValid", false);
                }
            } else {
                stats.put("hasToken", false);
            }

            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取Token状态失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private void incrementLoginFailCount(String username, String key, Integer currentCount) {
        Long newCount = redisTemplate.opsForValue().increment(key);
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 获取客户端真实IP
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}