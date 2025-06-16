package com.box.login.controller;

import com.box.login.dto.LoginRequest;
import com.box.login.dto.RegisterRequest;
import com.box.login.dto.ResetPasswordRequest;
import com.box.login.entity.User;
import com.box.login.filter.JwtTokenProvider;
import com.box.login.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String registerKey = "register:" + ip;

        Long registerCount = redisTemplate.opsForValue().increment(registerKey);
        if (registerCount != null && registerCount == 1) {
            redisTemplate.expire(registerKey, 24, TimeUnit.HOURS);
        }

        // 需要验证码时校验
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
                return Result.success(user);
            } else {
                return Result.<User>error("用户名已存在");
            }
        } catch (Exception e) {
            // 失败回滚计数
            if (registerCount != null) {
                redisTemplate.opsForValue().decrement(registerKey);
            }
            return Result.<User>error(e.getMessage());
        }
    }


    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // --- 登录失败次数跟踪和条件验证码逻辑 --- START
        String loginFailKey = "login_fail:" + username;
        String captchaKey = "captcha:" + request.getCaptchaId();

        Integer failCount = redisTemplate.opsForValue().get(loginFailKey) != null ?
                Integer.parseInt(redisTemplate.opsForValue().get(loginFailKey)) : 0;

        boolean captchaRequired = failCount >= 3;

        if (captchaRequired) {
            String savedCaptcha = redisTemplate.opsForValue().get(captchaKey);
            if (savedCaptcha == null) {
                return Result.<String>error("验证码已过期", true);
            }
            if (!savedCaptcha.equalsIgnoreCase(request.getCaptcha())) {
                incrementLoginFailCount(username, loginFailKey, failCount);
                redisTemplate.delete(captchaKey);
                return Result.<String>error("验证码错误", true);
            }
            redisTemplate.delete(captchaKey);
        }
        // --- 登录失败次数跟踪和条件验证码逻辑 --- END

        String token = userService.login(username, password);

        if (token != null) {
            redisTemplate.delete(loginFailKey);
            return Result.success(token);  // 只返回token，不返回用户信息
        } else {
            incrementLoginFailCount(username, loginFailKey, failCount);
            boolean needsCaptchaAfterFail = redisTemplate.opsForValue().get(loginFailKey) != null &&
                    Integer.parseInt(redisTemplate.opsForValue().get(loginFailKey)) >= 3;
            return Result.error("用户名或密码错误", needsCaptchaAfterFail);
        }
    }

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

            String username = jwtTokenProvider.getUsernameFromJWT(token);
            if (username == null) {
                return Result.error("无法解析用户信息");
            }

            User user = userService.findByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 建议去除密码等敏感信息，可自行在User实体里标注@JsonIgnore或复制DTO
            user.setPassword(null);

            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }


    // 辅助方法：增加登录失败次数并设置过期时间
    private void incrementLoginFailCount(String username, String key, Integer currentCount) {
        // 使用 incrementBy 方法原子地增加计数器
        Long newCount = redisTemplate.opsForValue().increment(key);

        // 如果是第一次失败，设置一个过期时间，例如24小时或者更短
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS); // 失败计数器24小时后重置
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

    @GetMapping("/verify-token")
    public Result<Map<String, Object>> verifyToken(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null) {
                return Result.error("Token不存在");
            }

            boolean shouldRefresh = jwtTokenProvider.shouldRefreshToken(token);
            if (jwtTokenProvider.validateToken(token)) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("shouldRefresh", shouldRefresh);
                return Result.success(response);
            }
            return Result.error("Token无效");
        } catch (JwtException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("验证过程发生错误");
        }
    }

    @PostMapping("/refresh-token")
    public Result<String> refreshToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);

        if (token == null) {
            return Result.error("Token不存在");
        }

        try {
            // 验证旧token
            if (!jwtTokenProvider.validateToken(token)) {
                return Result.error("Token无效");
            }

            // 检查是否需要刷新
            if (!jwtTokenProvider.shouldRefreshToken(token)) {
                return Result.success(token); // 返回原token
            }

            // 刷新token
            String newToken = jwtTokenProvider.refreshToken(token);
            if (newToken != null) {
                return Result.success(newToken);
            }

            return Result.error("Token刷新失败");
        } catch (JwtException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("刷新过程发生错误");
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            if (token != null) {
                try {
                    // 验证 token
                    if (jwtTokenProvider.validateToken(token)) {
                        // 获取用户名并废止token
                        String username = jwtTokenProvider.getUsernameFromJWT(token);
                        jwtTokenProvider.invalidateToken(token);

                        // 记录用户登出时间
                        String lastLogoutKey = "last_logout:" + username;
                        redisTemplate.opsForValue().set(lastLogoutKey, String.valueOf(System.currentTimeMillis()));

                        return Result.success();
                    }
                } catch (JwtException e) {
                    // token 已失效或过期，直接返回成功
                    return Result.success();
                }
            }
            return Result.success();
        } catch (Exception e) {
            System.err.println("登出处理异常: " + e.getMessage());
            return Result.error("登出过程发生错误");
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}