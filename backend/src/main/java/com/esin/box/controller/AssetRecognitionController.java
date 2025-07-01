package com.esin.box.controller;

import com.esin.box.config.UserContextHolder;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.dto.Result;
import com.esin.box.service.AssetRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/asset/recognition")
public class AssetRecognitionController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CT = Set.of(
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE, "image/webp", "image/bmp"
    );
    private static final Set<String> ALLOWED_EXT = Set.of(
            ".jpg",".jpeg",".png",".gif",".webp",".bmp"
    );

    @Autowired
    private AssetRecognitionService service;

    /**
     * 智能识别图片 - 自动选择最佳方案
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<AssetScanImageDTO>> recognizeImage(@RequestParam("file") MultipartFile file) {
        if (file != null) {
            log.info("上传文件: {}, 大小: {}, 类型: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
        }

        String err = validateFile(file);
        if (err != null) return Result.error(err);

        String user = UserContextHolder.getCurrentUsername();
        if (!StringUtils.hasText(user)) return Result.error("用户未登录");

        try {
            long t0 = System.currentTimeMillis();
            List<AssetScanImageDTO> list = service.recognizeImage(file, user);
            long elapsed = System.currentTimeMillis() - t0;

            log.info("智能识别完成: {} 条, 耗时 {} ms", list.size(), elapsed);

            if (list.isEmpty()) {
                return Result.of(true, "未识别到有效的资产信息", list);
            }
            return Result.of(true, "成功识别 " + list.size() + " 条资产记录", list);
        } catch (Exception e) {
            log.error("智能识别异常", e);
            return Result.error(getErrorMessage(e));
        }
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return "请选择要上传的图片文件";
        if (file.getSize() > MAX_FILE_SIZE) return "文件大小超过限制（10MB）";
        String ct = file.getContentType();
        if (ct == null || (!ALLOWED_CT.contains(ct) && !ct.toLowerCase().startsWith("image/")))
            return "文件类型不支持，请上传图片(JPG/PNG/GIF/WEBP/BMP)";
        String fn = file.getOriginalFilename();
        if (fn != null && !fn.isBlank()) {
            String lc = fn.toLowerCase();
            boolean ok = ALLOWED_EXT.stream().anyMatch(lc::endsWith);
            if (!ok) return "文件扩展名不正确，请上传图片文件";
        }
        return null;
    }

    private String getErrorMessage(Exception e) {
        String m = e.getMessage();
        if (e instanceof IllegalArgumentException) return "参数错误：" + (m != null ? m : "");
        if (m != null && m.contains("OCR")) return "图片识别失败，请确保图片清晰可读";
        if (m != null && m.contains("timeout")) return "图片识别超时，请稍后重试";
        return "图片识别失败，请检查图片质量后重试";
    }
}