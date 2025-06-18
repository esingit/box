package com.box.login.controller;

import com.box.login.dto.CaptchaResponse;
import com.box.login.utils.CaptchaUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final StringRedisTemplate redisTemplate;

    public CaptchaController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<CaptchaResponse> getCaptcha() {
        // 生成验证码
        CaptchaUtil.CaptchaResult captcha = CaptchaUtil.generateCaptcha();

        // 生成唯一标识
        String captchaId = UUID.randomUUID().toString();

        // 将验证码保存到Redis，设置5分钟过期
        redisTemplate.opsForValue().set(
                "captcha:" + captchaId,
                captcha.getCode(),
                5,
                TimeUnit.MINUTES
        );

        // 构建图片URL
        String captchaUrl = "/captcha/image/" + captchaId;

        // 返回JSON响应
        return ResponseEntity.ok(new CaptchaResponse(captchaId, captchaUrl));
    }

    @GetMapping("/image/{captchaId}")
    public void getCaptchaImage(@PathVariable String captchaId, HttpServletResponse response) throws IOException {
        // 从Redis获取验证码文本
        String captchaCode = redisTemplate.opsForValue().get("captcha:" + captchaId);

        if (captchaCode != null) {
            // 生成图片
            BufferedImage image = CaptchaUtil.generateImage(captchaCode);

            // 设置响应头
            response.setContentType("image/png");
            response.setHeader("Cache-Control", "no-store, no-cache");

            // 输出图片
            ImageIO.write(image, "png", response.getOutputStream());
        } else {
            // 验证码不存在或已过期
            response.sendError(HttpStatus.NOT_FOUND.value());
        }
    }
} 