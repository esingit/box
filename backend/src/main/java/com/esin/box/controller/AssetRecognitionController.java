package com.esin.box.controller;

import com.esin.box.config.UserContextHolder;
import com.esin.box.dto.AssetScanImageDTO;
import com.esin.box.service.AssetRecognitionService;
import com.esin.box.service.impl.recognition.RecognitionScheme;
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
     * 指定方案识别 - 新接口
     */
    @PostMapping(value = "/image/{scheme}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<AssetScanImageDTO>> recognizeImageWithScheme(
            @RequestParam("file") MultipartFile file,
            @PathVariable String scheme) {

        if (file != null) {
            log.info("上传文件: {}, 大小: {}, 类型: {}, 使用方案: {}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType(), scheme);
        }

        String err = validateFile(file);
        if (err != null) return Result.error(err);

        String user = UserContextHolder.getCurrentUsername();
        if (!StringUtils.hasText(user)) return Result.error("用户未登录");

        // 解析识别方案
        RecognitionScheme recognitionScheme = parseScheme(scheme);
        if (recognitionScheme == null) {
            return Result.error("不支持的识别方案: " + scheme + "，支持的方案: scheme1, scheme2, general");
        }

        try {
            long t0 = System.currentTimeMillis();
            List<AssetScanImageDTO> list = service.recognizeWithScheme(file, user, recognitionScheme);
            long elapsed = System.currentTimeMillis() - t0;

            log.info("{}识别完成: {} 条, 耗时 {} ms", recognitionScheme.getDescription(), list.size(), elapsed);

            if (list.isEmpty()) {
                return Result.of(true, "未识别到有效的资产信息", list);
            }
            return Result.of(true, String.format("使用%s成功识别 %d 条资产记录",
                    recognitionScheme.getDescription(), list.size()), list);
        } catch (Exception e) {
            log.error("{}识别异常", recognitionScheme.getDescription(), e);
            return Result.error(getErrorMessage(e));
        }
    }

    /**
     * 参数方式指定方案识别 - 新接口
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, params = "scheme")
    public Result<List<AssetScanImageDTO>> recognizeImageWithSchemeParam(
            @RequestParam("file") MultipartFile file,
            @RequestParam("scheme") String scheme) {

        return recognizeImageWithScheme(file, scheme);
    }

    /**
     * 自动识别 - 保持原有接口兼容
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<AssetScanImageDTO>> recognizeImage(@RequestParam("file") MultipartFile file) {
        if (file != null) {
            log.info("上传文件: {}, 大小: {}, 类型: {} (使用自动识别)",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
        }

        String err = validateFile(file);
        if (err != null) return Result.error(err);

        String user = UserContextHolder.getCurrentUsername();
        if (!StringUtils.hasText(user)) return Result.error("用户未登录");

        try {
            long t0 = System.currentTimeMillis();
            List<AssetScanImageDTO> list = service.recognizeImageAuto(file, user);
            log.info("自动识别完成: {} 条, 耗时 {} ms", list.size(), System.currentTimeMillis() - t0);
            if (list.isEmpty()) {
                return Result.of(true, "未识别到有效的资产信息", list);
            }
            return Result.of(true, "成功识别 " + list.size() + " 条资产记录", list);
        } catch (Exception e) {
            log.error("自动识别异常", e);
            return Result.error(getErrorMessage(e));
        }
    }

    /**
     * 获取支持的识别方案列表
     */
    @GetMapping("/schemes")
    public Result<List<SchemeInfo>> getSupportedSchemes() {
        List<SchemeInfo> schemes = List.of(
                new SchemeInfo("scheme1", RecognitionScheme.SCHEME_1.getDescription(), "适用于横向布局的资产信息"),
                new SchemeInfo("scheme2", RecognitionScheme.SCHEME_2.getDescription(), "适用于纵向布局的资产信息"),
                new SchemeInfo("general", RecognitionScheme.GENERAL.getDescription(), "通用识别方案，自动适配多种格式")
        );
        return Result.success(schemes);
    }

    /**
     * 解析识别方案
     */
    private RecognitionScheme parseScheme(String scheme) {
        if (!StringUtils.hasText(scheme)) return null;

        switch (scheme.toLowerCase()) {
            case "scheme1":
            case "1":
                return RecognitionScheme.SCHEME_1;
            case "scheme2":
            case "2":
                return RecognitionScheme.SCHEME_2;
            case "general":
            case "auto":
            case "default":
                return RecognitionScheme.GENERAL;
            default:
                return null;
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

    /**
     * 方案信息DTO
     */
    public static class SchemeInfo {
        private String code;
        private String name;
        private String description;

        public SchemeInfo(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        // getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}