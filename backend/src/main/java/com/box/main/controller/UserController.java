package com.box.main.controller;

import com.box.main.dto.ApiResponse;
import com.box.main.dto.RegisterRequest;
import com.box.main.dto.LoginRequest;
import com.box.main.dto.UserLoginResponseDTO;
import com.box.main.dto.ResetPasswordRequest;
import com.box.main.entity.User;
import com.box.main.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        // --- 获取IP地址并检查注册频率 --- START
        String ip = httpRequest.getRemoteAddr();
        String registerKey = "register:" + ip;
        // 使用 incrementBy 方法原子地增加计数器，并设置过期时间
        Long registerCount = redisTemplate.opsForValue().increment(registerKey);

        // 如果是第一次注册，设置24小时过期时间
        if (registerCount != null && registerCount == 1) {
             redisTemplate.expire(registerKey, 24, TimeUnit.HOURS);
        }
        // --- 获取IP地址并检查注册频率 --- END

        // --- 条件验证码逻辑 --- START
        // 当注册次数大于1时需要验证码
        boolean captchaRequired = registerCount != null && registerCount > 1;

        if (captchaRequired) {
        // 验证验证码
        String captchaKey = "captcha:" + request.getCaptchaId();
        String savedCaptcha = redisTemplate.opsForValue().get(captchaKey);
        
        if (savedCaptcha == null) {
                // 如果需要验证码但验证码过期或不存在，返回错误并指示需要验证码
                return ResponseEntity.badRequest().body(new ApiResponse(false, "验证码已过期", null, true));
        }
        
        if (!savedCaptcha.equalsIgnoreCase(request.getCaptcha())) {
                 // 如果需要验证码但验证码错误，返回错误并指示需要验证码
                // 注意：验证码错误也应该刷新验证码，但这里只是后端逻辑，刷新由前端根据showCaptcha标志触发
                 return ResponseEntity.badRequest().body(new ApiResponse(false, "验证码错误", null, true));
        }
        
            // 验证码验证成功，清除验证码
        redisTemplate.delete(captchaKey);
        } else {
             // 第一次注册不需要验证码，但如果前端误传了验证码，可以选择忽略或报错
             // 这里我们假设前端在不需要验证码时不会发送captchaId和captcha
             // 如果request中包含了captchaId或captcha，并且captchaRequired为false，说明前端逻辑可能需要调整或者忽略这些字段
             // 为了简化，我们在这里不对此情况进行特殊处理，依赖前端正确判断是否需要发送验证码
        }
        // --- 条件验证码逻辑 --- END

        // --- 继续原有的注册逻辑... --- START
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            
            boolean registered = userService.register(user);
            
            if (registered) {
                return ResponseEntity.ok(new ApiResponse(true, "注册成功"));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "用户名已存在"));
            }
        } catch (Exception e) {
            // 如果注册过程中出现异常，回滚注册次数计数器（可选，取决于您的需求）
            // redisTemplate.opsForValue().decrement(registerKey);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
        // --- 继续原有的注册逻辑... --- END
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
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
                return new Result(false, "验证码已过期", null, true);
            }
            if (!savedCaptcha.equalsIgnoreCase(request.getCaptcha())) {
                incrementLoginFailCount(username, loginFailKey, failCount);
                redisTemplate.delete(captchaKey);
                return new Result(false, "验证码错误", null, true);
            }
            redisTemplate.delete(captchaKey);
        }
        // --- 登录失败次数跟踪和条件验证码逻辑 --- END

        // 调用登录服务，此服务应负责验证凭据并更新 last_login_time
        String token = userService.login(username, password); 

        if (token != null) {
            redisTemplate.delete(loginFailKey); // 清除登录失败次数

            // 通过用户名获取完整的用户信息（包括更新后的 last_login_time）
            // 假设 userService.findByUsername 能获取到 User 实体
            // 并且 User 实体有 getUsername(), getEmail(), getLastLoginTime() 方法
            User userEntity = userService.findByUsername(username);

            if (userEntity != null) {
                UserLoginResponseDTO responseData = new UserLoginResponseDTO(
                        token,
                        userEntity.getUsername(),
                        userEntity.getEmail(),
                        userEntity.getLastLoginTime() 
                );
                return new Result(true, "登录成功", responseData);
            } else {
                // 理论上，如果token生成成功，用户应该能被找到
                // 但作为防御性编程，处理此情况
                return new Result(false, "登录成功但无法检索用户信息", null); 
            }
        } else {
            incrementLoginFailCount(username, loginFailKey, failCount);
            boolean needsCaptchaAfterFail = redisTemplate.opsForValue().get(loginFailKey) != null &&
                                            Integer.parseInt(redisTemplate.opsForValue().get(loginFailKey)) >= 3;
            return new Result(false, "用户名或密码错误", null, needsCaptchaAfterFail);
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
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // 1. 查找用户
            User user = userService.findByUsername(request.getUsername());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "用户不存在"));
            }
            // 2. 校验旧密码
            if (!userService.checkPassword(user, request.getOldPassword())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "旧密码错误"));
            }
            // 3. 更新新密码
            boolean updated = userService.updatePassword(user, request.getNewPassword());
            if (updated) {
                return ResponseEntity.ok(new ApiResponse(true, "密码重置成功！"));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "密码重置失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "服务器异常: " + e.getMessage()));
        }
    }
}

class Result {
    private boolean success;
    private String message;
    private Object data;
    private boolean showCaptcha;

    public Result(boolean success, String message, Object data, boolean showCaptcha) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.showCaptcha = showCaptcha;
    }

    public Result(boolean success, String message) {
        this(success, message, null, false);
    }

    public Result(boolean success, String message, Object data) {
        this(success, message, data, false);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public boolean isShowCaptcha() { return showCaptcha; }
}