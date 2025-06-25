package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esin.box.config.UserContextHolder;
import com.esin.box.converter.AssetRecordConverter;
import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.dto.AssetStatsDTO;
import com.esin.box.entity.AssetName;
import com.esin.box.entity.AssetRecord;
import com.esin.box.entity.CommonMeta;
import com.esin.box.mapper.AssetRecordMapper;
import com.esin.box.service.AssetNameService;
import com.esin.box.service.AssetRecordService;
import com.esin.box.service.CommonMetaService;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class AssetRecordServiceImpl implements AssetRecordService {

    @Autowired
    private AssetRecordMapper assetRecordMapper;

    @Autowired
    private CommonMetaService commonMetaService;

    @Autowired
    private AssetRecordConverter assetRecordConverter;

    @Autowired
    private AssetNameService assetNameService;

    @Autowired
    private ITesseract tesseract;  // 注入配置好的 Tesseract 实例

    @Value("${app.upload.temp-dir:/tmp/asset-images}")
    private String tempDir;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssetRecordServiceImpl.class);

    @Override
    public void addRecord(AssetRecord record) {
        log.debug("Adding new record: {}", record);

        // 参数验证
        if (record.getAssetNameId() == null) {
            throw new RuntimeException("资产名称不能为空");
        }
        if (record.getAssetTypeId() == null) {
            throw new RuntimeException("资产类型不能为空");
        }
        if (record.getAmount() == null || record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("金额必须大于0");
        }
        if (record.getUnitId() == null) {
            throw new RuntimeException("货币单位不能为空");
        }
        if (record.getAssetLocationId() == null) {
            throw new RuntimeException("资产位置不能为空");
        }

        try {
            // 设置创建人
            record.setCreateUser(UserContextHolder.getCurrentUsername());
            // 如果没有设置购入/登记时间，则使用当前时间
            if (record.getAcquireTime() == null) {
                record.setAcquireTime(LocalDateTime.now());
            }

            log.debug("Inserting record with data: {}", record);
            assetRecordMapper.insert(record);
            log.debug("Record inserted successfully");
        } catch (Exception e) {
            log.error("Failed to insert record: {}", e.getMessage(), e);
            throw new RuntimeException("添加记录失败：" + e.getMessage());
        }
    }

    @Override
    public void updateRecord(AssetRecord record) {
        // 验证是否是记录创建人
        AssetRecord existing = assetRecordMapper.selectById(record.getId());
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限修改此记录");
        }
        assetRecordMapper.updateById(record);
    }

    @Override
    public void deleteRecord(Long id) {
        // 验证是否是记录创建人
        AssetRecord existing = assetRecordMapper.selectById(id);
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限删除此记录");
        }
        assetRecordMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<AssetRecordDTO> pageByConditions(Page<AssetRecord> page, List<Long> assetNameIdList, List<Long> assetLocationIdList,
                                                  List<Long> assetTypeIdList, String remark, String startDate, String endDate, String createUser) {
        return assetRecordMapper.selectPageWithMeta(page, assetNameIdList, assetLocationIdList, assetTypeIdList, remark, startDate, endDate, createUser);
    }

    @Override
    public void copyLastRecords(boolean force) {
        String currentUser = UserContextHolder.getCurrentUsername();
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        // 首先检查今天是否已有记录
        if (!force) {
            QueryWrapper<AssetRecord> todayWrapper = new QueryWrapper<>();
            todayWrapper.eq("create_user", currentUser)
                    .eq("deleted", 0)
                    .apply("DATE(acquire_time) = CURRENT_DATE()");

            Long todayCount = assetRecordMapper.selectCount(todayWrapper).longValue();
            if (todayCount > 0) {
                throw new RuntimeException("今日已有记录，如需重复复制请使用强制模式");
            }
        }

        // 查询最近的一天的日期（不包括今天）
        QueryWrapper<AssetRecord> dateWrapper = new QueryWrapper<>();
        dateWrapper.select("DATE(acquire_time) as date_only")
                .eq("create_user", currentUser)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) < CURRENT_DATE()")
                .groupBy("DATE(acquire_time)")
                .orderByDesc("date_only")
                .last("LIMIT 1");

        List<Map<String, Object>> dateList = assetRecordMapper.selectMaps(dateWrapper);

        if (dateList.isEmpty()) {
            log.info("没有找到历史记录");
            throw new RuntimeException("没有找到可以复制的历史记录");
        }

        String dateStr = dateList.get(0).get("date_only").toString();
        log.info("找到最近记录日期: {}", dateStr);

        // 查询指定日期的所有记录
        QueryWrapper<AssetRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("create_user", currentUser)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) = STR_TO_DATE({0}, '%Y-%m-%d')", dateStr)
                .orderByAsc("create_time");

        List<AssetRecord> recordsToCopy = assetRecordMapper.selectList(wrapper);

        if (recordsToCopy.isEmpty()) {
            log.info("在日期 {} 没有找到用户 {} 的记录", dateStr, currentUser);
            throw new RuntimeException("在最近的记录日期中没有找到可复制的记录");
        }

        log.info("找到 {} 条记录需要复制", recordsToCopy.size());

        // 如果是强制模式，先删除今天的所有记录
        if (force) {
            QueryWrapper<AssetRecord> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("create_user", currentUser)
                    .eq("deleted", 0)
                    .apply("DATE(acquire_time) = CURRENT_DATE()");
            assetRecordMapper.delete(deleteWrapper);
        }

        // 复制记录
        for (AssetRecord record : recordsToCopy) {
            AssetRecord newRecord = new AssetRecord();
            newRecord.setAssetNameId(record.getAssetNameId());
            newRecord.setAssetTypeId(record.getAssetTypeId());
            newRecord.setUnitId(record.getUnitId());
            newRecord.setAssetLocationId(record.getAssetLocationId());
            newRecord.setAcquireTime(today);
            newRecord.setCreateUser(currentUser);
            newRecord.setAmount(record.getAmount());
            newRecord.setRemark(record.getRemark());

            assetRecordMapper.insert(newRecord);
            log.info("Copied record: type={}, amount={}", record.getAssetTypeId(), record.getAmount());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssetStatsDTO getLatestStats(String createUser, Integer offset) {
        log.debug("获取用户 {} 的资产统计, 偏移天数: {}", createUser, offset);

        // 1. 找到指定偏移日期的记录日期
        QueryWrapper<AssetRecord> dateWrapper = new QueryWrapper<>();
        dateWrapper.select("DATE(acquire_time) as date_only")
                .eq("create_user", createUser)
                .eq("deleted", 0)
                .groupBy("DATE(acquire_time)")
                .orderByDesc("date_only");

        if (offset > 0) {
            dateWrapper.last("LIMIT " + offset + ", 1");
        } else {
            dateWrapper.last("LIMIT 1");
        }

        Map<String, Object> dateResult = assetRecordMapper.selectMaps(dateWrapper).stream().findFirst().orElse(null);
        if (dateResult == null || dateResult.get("date_only") == null) {
            log.info("未找到任何记录，返回零值统计");
            return AssetStatsDTO.builder()
                    .totalAssets(0.0)
                    .totalLiabilities(0.0)
                    .latestDate(LocalDateTime.now().toString())
                    .build();
        }

        String latestDate = dateResult.get("date_only").toString();
        log.debug("找到最新记录日期: {}", latestDate);

        // 2. 获取最新日期的资产记录，按类型分组统计
        QueryWrapper<AssetRecord> latestWrapper = new QueryWrapper<>();
        latestWrapper.eq("create_user", createUser)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) = STR_TO_DATE({0}, '%Y-%m-%d')", latestDate);

        List<AssetRecord> latestRecords = assetRecordMapper.selectList(latestWrapper);

        // 3. 获取上一个日期的资产记录，用于计算变化率
        QueryWrapper<AssetRecord> previousWrapper = new QueryWrapper<>();
        previousWrapper.select("DATE(acquire_time) as date_only")
                .eq("create_user", createUser)
                .eq("deleted", 0)
                .lt("DATE(acquire_time)", latestDate)
                .orderByDesc("acquire_time")
                .last("LIMIT 1");

        String previousDate = assetRecordMapper.selectMaps(previousWrapper).stream()
                .findFirst()
                .map(m -> m.get("date_only").toString())
                .orElse(null);

        List<AssetRecord> previousRecords = new ArrayList<>();
        if (previousDate != null) {
            QueryWrapper<AssetRecord> prevRecordsWrapper = new QueryWrapper<>();
            prevRecordsWrapper.eq("create_user", createUser)
                    .eq("deleted", 0)
                    .apply("DATE(acquire_time) = STR_TO_DATE({0}, '%Y-%m-%d')", previousDate);
            previousRecords = assetRecordMapper.selectList(prevRecordsWrapper);
        }

        // 4. 计算最新日期的资产和负债总额
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        Map<Long, BigDecimal> typeAmounts = new HashMap<>();

        for (AssetRecord record : latestRecords) {
            CommonMeta type = commonMetaService.getById(record.getAssetTypeId());
            if (type != null) {
                // 按类型累加金额
                typeAmounts.merge(record.getAssetTypeId(), record.getAmount(), BigDecimal::add);
            }
        }

        // 5. 按类型统计资产和负债
        for (Map.Entry<Long, BigDecimal> entry : typeAmounts.entrySet()) {
            CommonMeta type = commonMetaService.getById(entry.getKey());
            if (type != null && "ASSET_TYPE".equals(type.getTypeCode()) && "DEBT".equals(type.getKey1())) {
                totalLiabilities = totalLiabilities.add(entry.getValue());
            } else {
                totalAssets = totalAssets.add(entry.getValue());
            }
        }

        // 6. 计算变化率
        BigDecimal previousAssets = BigDecimal.ZERO;
        BigDecimal previousLiabilities = BigDecimal.ZERO;
        Map<Long, BigDecimal> prevTypeAmounts = new HashMap<>();

        for (AssetRecord record : previousRecords) {
            CommonMeta type = commonMetaService.getById(record.getAssetTypeId());
            if (type != null) {
                // 按类型累加金额
                prevTypeAmounts.merge(record.getAssetTypeId(), record.getAmount(), BigDecimal::add);
            }
        }

        // 7. 按类型统计前一天的资产和负债
        for (Map.Entry<Long, BigDecimal> entry : prevTypeAmounts.entrySet()) {
            CommonMeta type = commonMetaService.getById(entry.getKey());
            if (type != null && "ASSET_TYPE".equals(type.getTypeCode()) && "DEBT".equals(type.getKey1())) {
                previousLiabilities = previousLiabilities.add(entry.getValue());
            } else {
                previousAssets = previousAssets.add(entry.getValue());
            }
        }

        // 8. 计算变化额
        BigDecimal assetsChange = totalAssets.subtract(previousAssets);
        BigDecimal liabilitiesChange = totalLiabilities.subtract(previousLiabilities);

        // 格式化日期显示
        String formattedDate;
        try {
            LocalDate date = LocalDate.parse(latestDate);
            formattedDate = date.format(DateTimeFormatter.ofPattern("MM月dd日"));
        } catch (Exception e) {
            formattedDate = latestDate;
        }

        log.info("计算完成 - 总资产: {}, 总负债: {}, 资产变化: {}, 负债变化: {}",
                totalAssets, totalLiabilities, assetsChange, liabilitiesChange);

        return AssetStatsDTO.builder()
                .totalAssets(totalAssets.doubleValue())
                .totalLiabilities(totalLiabilities.doubleValue())
                .latestDate(latestDate)
                .formattedDate(formattedDate)
                .assetsChange(assetsChange.doubleValue())
                .liabilitiesChange(liabilitiesChange.doubleValue())
                .build();
    }

    @Override
    public List<AssetRecordDTO> listByConditions(List<Long> assetNameIdList,
                                                 List<Long> assetLocationIdList,
                                                 List<Long> assetTypeIdList,
                                                 String remark,
                                                 String startDate,
                                                 String endDate,
                                                 String createUser) {
        QueryWrapper<AssetRecord> wrapper = new QueryWrapper<>();
        if (assetNameIdList != null && !assetNameIdList.isEmpty()) {
            wrapper.in("asset_name_id", assetNameIdList);
        }
        if (assetLocationIdList != null && !assetLocationIdList.isEmpty()) {
            wrapper.in("asset_location_id", assetLocationIdList);
        }
        if (assetTypeIdList != null && !assetTypeIdList.isEmpty()) {
            wrapper.in("asset_type_id", assetTypeIdList);
        }
        if (remark != null && !remark.isBlank()) {
            wrapper.like("remark", remark);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge("acquire_time", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le("acquire_time", endDate);
        }
        wrapper.eq("create_user", createUser);
        wrapper.orderByDesc("acquire_time");

        List<AssetRecord> entityList = assetRecordMapper.selectList(wrapper);
        return assetRecordConverter.toDTOList(entityList);
    }

    @Override
    public List<AssetRecordDTO> recognizeAssetImage(MultipartFile image) throws Exception {
        List<AssetRecordDTO> results = new ArrayList<>();

        // 创建临时文件
        File tempFile = null;
        try {
            // 确保临时目录存在
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存上传的图片到临时文件
            String fileName = "asset_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
            tempFile = new File(dir, fileName);
            image.transferTo(tempFile);

            log.info("图片保存成功: {}", tempFile.getAbsolutePath());

            // 验证文件状态
            System.out.println("文件存在: " + tempFile.exists());
            System.out.println("文件路径: " + tempFile.getAbsolutePath());
            System.out.println("文件大小: " + tempFile.length());
            System.out.println("可读权限: " + tempFile.canRead());

            // 验证图片可以读取
            try {
                BufferedImage testImage = ImageIO.read(tempFile);
                System.out.println("图片宽度: " + testImage.getWidth());
                System.out.println("图片高度: " + testImage.getHeight());
                System.out.println("图片类型: " + testImage.getType());
            } catch (Exception e) {
                System.err.println("图片读取失败: " + e.getMessage());
                throw new RuntimeException("图片文件无效或损坏");
            }

            // 检查Tesseract配置
            System.out.println("=== Tesseract配置检查 ===");
            try {
                System.out.println("Tesseract类: " + tesseract.getClass().getName());
                if (tesseract instanceof net.sourceforge.tess4j.Tesseract) {
                    net.sourceforge.tess4j.Tesseract tess = (net.sourceforge.tess4j.Tesseract) tesseract;
                }
            } catch (Exception e) {
                System.err.println("获取Tesseract配置失败: " + e.getMessage());
            }

            // 使用CompletableFuture进行OCR识别，添加超时控制
            String ocrText = performOCRWithTimeout(tempFile);

            if (ocrText != null && !ocrText.trim().isEmpty()) {
                System.out.println("OCR识别结果: " + ocrText);
                // 解析OCR文本，提取资产信息
                results = parseOCRText(ocrText);
                log.info("解析出 {} 条资产记录", results.size());
            } else {
                log.warn("OCR识别结果为空");
            }

        } catch (Exception e) {
            log.error("图片识别失败", e);
            throw new RuntimeException("图片识别失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.debug("临时文件清理: {}", deleted ? "成功" : "失败");
            }
        }

        return results;
    }

    /**
     * 执行OCR识别，带超时控制
     */
    private String performOCRWithTimeout(File tempFile) throws Exception {
        BufferedImage bufferedImage = null;
        try {
            // 图片预读取，规避 Tesseract PNG header 读取错误
            bufferedImage = ImageIO.read(tempFile);
            if (bufferedImage == null) {
                throw new RuntimeException("图片读取失败：可能格式不正确或文件已损坏");
            }
        } catch (IOException e) {
            System.err.println("图片预读取失败: " + e.getMessage());
            throw new RuntimeException("图片格式不支持或已损坏", e);
        }

        try {
            System.out.println("开始OCR识别...");
            System.out.println("当前线程: " + Thread.currentThread().getName());
            System.out.println("可用内存: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "MB");

            BufferedImage finalImage = bufferedImage; // for lambda capture
            CompletableFuture<String> ocrTask = CompletableFuture.supplyAsync(() -> {
                System.out.println("=== 进入异步OCR任务 ===");
                System.out.println("异步线程: " + Thread.currentThread().getName());
                try {
                    System.out.println("开始执行doOCR...");
                    long startTime = System.currentTimeMillis();
                    String result = tesseract.doOCR(finalImage);
                    long endTime = System.currentTimeMillis();
                    System.out.println("doOCR执行完成，耗时: " + (endTime - startTime) + "ms");
                    System.out.println("识别结果长度: " + (result != null ? result.length() : 0));
                    return result;
                } catch (TesseractException e) {
                    System.err.println("Tesseract异常: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
                } catch (Exception e) {
                    System.err.println("异步任务中的其他异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("OCR执行失败", e);
                }
            });

            System.out.println("等待OCR结果...");
            String text = ocrTask.get(60, TimeUnit.SECONDS); // 60秒超时
            System.out.println("OCR识别成功");
            return text;

        } catch (TimeoutException e) {
            System.err.println("OCR识别超时（60秒）");
            throw new RuntimeException("OCR识别超时，请尝试使用更小或更清晰的图片", e);
        } catch (ExecutionException e) {
            System.err.println("OCR执行异常: " + e.getCause());
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException("OCR执行失败: " + cause.getMessage(), cause);
            }
        } catch (InterruptedException e) {
            System.err.println("OCR被中断");
            Thread.currentThread().interrupt();
            throw new RuntimeException("OCR被中断", e);
        } finally {
            System.out.println("OCR识别流程结束");
        }
    }


    private List<AssetRecordDTO> parseOCRText(String text) {
        List<AssetRecordDTO> results = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return results;
        }

        log.info("=== 开始解析OCR文本 ===");
        String[] lines = text.split("\\n");

        // 尝试方案1：左右列结构
        log.info("尝试左右列结构解析...");
        results = parseLeftRightColumns(lines);

        if (!results.isEmpty()) {
            // 检查是否有以"郑"开头的名称
            boolean hasPersonalInfo = results.stream()
                    .anyMatch(item -> item.getAssetName().startsWith("郑"));

            if (hasPersonalInfo) {
                log.info("左右列结构解析出现个人信息（郑开头），切换到上下块结构解析");
                results.clear(); // 清空第一种解析的结果
            } else {
                log.info("左右列结构解析成功，共{}条记录", results.size());
                return results;
            }
        }

        // 尝试方案2：上下块结构
        log.info("尝试上下块结构解析...");
        results = parseUpDownBlocks(lines);

        if (!results.isEmpty()) {
            log.info("上下块结构解析成功，共{}条记录", results.size());
            return results;
        }

        // 通用方案
        log.info("尝试通用解析...");
        results = parseGeneric(lines);

        log.info("解析完成，共{}条记录", results.size());
        return results;
    }

    // 方案1：解析左右列结构（名称开头+金额在一行，名称剩余部分在后续行）
    private List<AssetRecordDTO> parseLeftRightColumns(String[] lines) {
        List<AssetRecordDTO> results = new ArrayList<>();

        // 找到表头位置
        int startIndex = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("名称") && lines[i].contains("金额")) {
                startIndex = i + 1;
                break;
            }
        }

        log.info("从第{}行开始解析", startIndex);

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // 跳过无关行
            if (isIrrelevantLineForLeftRight(line)) {
                log.debug("跳过无关行: {}", line);
                continue;
            }

            log.debug("处理行: {}", line);

            // 检查这一行是否包含金额（右侧有大数字）
            BigDecimal amountInLine = extractAmountFromEndOfLine(line);

            if (amountInLine != null && amountInLine.compareTo(new BigDecimal("1000")) > 0) {
                // 这一行包含金额，说明是一个产品记录的开始
                log.info("找到金额行: {} - {}", line, amountInLine);

                // 提取金额前面的文本作为名称的开始部分
                String nameStart = extractNameBeforeAmount(line);

                // 向下查找名称的剩余部分
                List<String> nameRemainingParts = new ArrayList<>();
                if (!nameStart.isEmpty()) {
                    nameRemainingParts.add(nameStart);
                }

                // 从下一行开始查找名称的剩余部分
                int nextIndex = i + 1;
                while (nextIndex < lines.length) {
                    String nextLine = lines[nextIndex].trim();

                    // 如果是空行，跳过
                    if (nextLine.isEmpty()) {
                        nextIndex++;
                        continue;
                    }

                    // 如果是无关行，跳过
                    if (isIrrelevantLineForLeftRight(nextLine)) {
                        nextIndex++;
                        continue;
                    }

                    // 如果下一行也包含金额，说明这是新的产品了，停止
                    if (extractAmountFromEndOfLine(nextLine) != null) {
                        break;
                    }

                    // 如果是名称的剩余部分，加入
                    if (isNameContinuationLine(nextLine)) {
                        nameRemainingParts.add(nextLine);
                        log.debug("添加名称剩余部分: {}", nextLine);
                        nextIndex++;
                    } else {
                        // 不是名称部分，停止
                        break;
                    }
                }

                // 更新主循环索引
                i = nextIndex - 1;

                // 组合完整的产品名称
                String fullProductName = String.join("", nameRemainingParts);

                if (!fullProductName.isEmpty()) {
                    // 检查是否以"郑"开头（个人信息）
                    if (fullProductName.startsWith("郑")) {
                        log.debug("发现个人信息，返回标记");
                        AssetRecordDTO marker = new AssetRecordDTO();
                        marker.setAssetName("郑");
                        results.add(marker);
                        return results;
                    } else {
                        AssetRecordDTO item = new AssetRecordDTO();
                        item.setAssetName(cleanProductName(fullProductName));
                        item.setAmount(amountInLine);
                        item.setAcquireTime(LocalDateTime.now());
                        item.setRemark("图片识别导入");
                        results.add(item);
                        log.info("左右列-添加记录: {} - {}", item.getAssetName(), amountInLine);
                    }
                }
            }
        }

        return results;
    }

    // 判断是否是名称的继续行
    private boolean isNameContinuationLine(String line) {
        if (line == null || line.isEmpty()) return false;

        // 排除明显的非名称行
        if (line.equals("马") || line.equals("吕D") || line.equals("DD") ||
                line.matches("^[A-Z]{1,3}$") || line.matches("^\\d+$")) {
            return false;
        }

        // 必须包含中文字符
        if (!line.matches(".*[\\u4e00-\\u9fa5]+.*")) {
            return false;
        }

        // 包含理财产品相关词汇
        if (line.contains("理财") ||
                line.contains("产品") ||
                line.contains("债券") ||
                line.contains("基金") ||
                line.contains("固收") ||
                line.contains("开放式") ||
                line.contains("净值型") ||
                line.contains("收益") ||
                line.contains("持盈") ||
                line.contains("天持") ||
                line.contains("个人") ||
                line.contains("类")) {
            return true;
        }

        // 或者是合理长度的中文文本
        if (line.length() >= 2 && line.replaceAll("[^\\u4e00-\\u9fa5]", "").length() >= 2) {
            return true;
        }

        return false;
    }

    // 从行尾提取金额（更精确的版本）
    private BigDecimal extractAmountFromEndOfLine(String line) {
        if (line == null || line.isEmpty()) return null;

        // 匹配行尾的金额格式：数字+逗号+小数点，考虑可能的空格
        Pattern pattern = Pattern.compile("([\\d,]+\\.\\d{2})\\s*$");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String amountStr = matcher.group(1);
            BigDecimal amount = parseAmount(amountStr);
            if (amount != null && amount.compareTo(new BigDecimal("1000")) > 0) {
                log.debug("从行尾提取到小数金额: {} -> {}", amountStr, amount);
                return amount;
            }
        }

        // 匹配整数金额（至少4位数，避免误识别）
        Pattern intPattern = Pattern.compile("([\\d,]{4,})\\s*$");
        Matcher intMatcher = intPattern.matcher(line);

        if (intMatcher.find()) {
            String amountStr = intMatcher.group(1);
            // 确保不是日期或其他数字
            if (!amountStr.matches("\\d{8}") && !amountStr.matches("20\\d{6}")) {
                BigDecimal amount = parseAmount(amountStr);
                if (amount != null && amount.compareTo(new BigDecimal("1000")) > 0) {
                    log.debug("从行尾提取到整数金额: {} -> {}", amountStr, amount);
                    return amount;
                }
            }
        }

        return null;
    }

    // 提取金额前面的名称部分（改进版）
    private String extractNameBeforeAmount(String line) {
        if (line == null || line.isEmpty()) return "";

        // 先尝试匹配带小数点的金额
        Pattern decimalPattern = Pattern.compile("^(.+?)\\s+([\\d,]+\\.\\d{2})\\s*$");
        Matcher decimalMatcher = decimalPattern.matcher(line);

        if (decimalMatcher.find()) {
            String namePart = decimalMatcher.group(1).trim();
            log.debug("提取金额前名称（小数）: {} -> {}", line, namePart);
            return namePart;
        }

        // 再尝试匹配整数金额
        Pattern intPattern = Pattern.compile("^(.+?)\\s+([\\d,]{4,})\\s*$");
        Matcher intMatcher = intPattern.matcher(line);

        if (intMatcher.find()) {
            String amountStr = intMatcher.group(2);
            // 确保是有效金额
            if (!amountStr.matches("\\d{8}") && !amountStr.matches("20\\d{6}")) {
                String namePart = intMatcher.group(1).trim();
                log.debug("提取金额前名称（整数）: {} -> {}", line, namePart);
                return namePart;
            }
        }

        return "";
    }

    // 判断是否是无关行（更新版）
    private boolean isIrrelevantLineForLeftRight(String line) {
        return line.contains("总金额") ||
                line.contains("收起") ||
                line.contains("温馨提示") ||
                line.equals("名称") ||
                line.equals("金额") ||
                line.equals("理财") ||
                line.equals("全") ||
                line.equals("区") ||
                line.matches("^\\d{2}:\\d{2}$") || // 时间
                line.matches("^\\d{4}/\\d{2}/\\d{2}$") || // 日期
                line.matches("^\\d{3}\\.\\d{3}\\.\\d{2}$") || // 格式化的总金额
                line.equals("马") || // OCR误识别
                line.equals("吕D") || // OCR误识别
                line.equals("DD") || // OCR误识别
                line.matches("^[A-Z]{1,3}$") || // 其他OCR误识别
                line.equals("《"); // OCR误识别的符号
    }

    // 清理产品名称（增强版）
    private String cleanProductName(String name) {
        return name.trim()
                .replaceAll("\\s+", "") // 去除所有空格
                .replaceAll("[':]", "·") // 统一分隔符
                .replaceAll("讲添益", "鑫添益") // 修正OCR识别错误
                .replaceAll("蠢尊利", "鑫尊利") // 修正OCR识别错误
                .replaceAll("春添益", "鑫添益") // 修正OCR识别错误
                .replaceAll("寺盈", "持盈") // 修正OCR识别错误
                .replaceAll("恒窒", "恒睿") // 修正OCR识别错误
                .replaceAll("产马", "产品") // 修正OCR识别错误
                .replaceAll("产吕D", "产品") // 修正OCR识别错误
                .trim();
    }

    // 解析金额
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return null;
        }

        try {
            // 去除逗号
            String cleaned = amountStr.replaceAll(",", "");
            BigDecimal amount = new BigDecimal(cleaned);
            return amount;
        } catch (Exception e) {
            log.warn("解析金额失败: {}", amountStr);
            return null;
        }
    }

    // 方案2：解析上下块结构（名称在上，金额在下）
    private List<AssetRecordDTO> parseUpDownBlocks(String[] lines) {
        List<AssetRecordDTO> results = new ArrayList<>();

        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();

            // 跳过空行
            if (line.isEmpty()) {
                i++;
                continue;
            }

            // 排除以'郑'、'持仓市值'开头的行以及其他无关行
            if (line.startsWith("郑") ||
                    line.startsWith("持仓市值") ||
                    line.startsWith("持仓收益") ||
                    line.startsWith("可赎回") ||
                    line.startsWith("最短持有期") ||
                    line.contains("温馨提示") ||
                    line.matches("^\\*+.*")) {
                log.debug("跳过排除行: {}", line);
                i++;
                continue;
            }

            // 判断是否是产品名称行（包含|分隔符且包含理财相关内容）
            if (isProductNameLineForUpDown(line)) {
                String productName = line;
                log.info("找到产品名称: {}", productName);

                // 向下查找金额行（最多查找5行）
                BigDecimal amount = null;
                int searchEnd = Math.min(i + 6, lines.length);

                for (int j = i + 1; j < searchEnd; j++) {
                    String nextLine = lines[j].trim();

                    // 跳过空行和排除行
                    if (nextLine.isEmpty() ||
                            nextLine.startsWith("郑") ||
                            nextLine.startsWith("持仓市值") ||
                            nextLine.startsWith("持仓收益") ||
                            nextLine.startsWith("可赎回") ||
                            nextLine.startsWith("最短持有期")) {
                        continue;
                    }

                    // 尝试从这一行提取金额（第一个数字）
                    amount = extractFirstAmount(nextLine);
                    if (amount != null && amount.compareTo(new BigDecimal("100")) > 0) {
                        log.info("在第{}行找到金额: {} (原文: {})", j, amount, nextLine);
                        i = j; // 跳到这一行，下次循环从这里继续
                        break;
                    }
                }

                if (amount != null) {
                    AssetRecordDTO item = new AssetRecordDTO();
                    item.setAssetName(cleanProductName(productName));
                    item.setAmount(amount);
                    item.setAcquireTime(LocalDateTime.now());
                    item.setRemark("图片识别导入");
                    results.add(item);
                    log.info("上下块-添加记录: {} - {}", item.getAssetName(), amount);
                } else {
                    log.warn("未找到产品 {} 对应的金额", productName);
                }
            }

            i++;
        }

        return results;
    }

    // 判断是否是产品名称行（针对上下块结构）
    private boolean isProductNameLineForUpDown(String line) {
        // 必须包含|分隔符
        if (!line.contains("|")) {
            return false;
        }

        // 必须包含中文
        if (!line.matches(".*[\\u4e00-\\u9fa5]+.*")) {
            return false;
        }

        // 包含理财产品关键词
        if (line.contains("理财") ||
                line.contains("基金") ||
                line.contains("存款") ||
                line.contains("债券") ||
                line.contains("精选") ||
                line.contains("成长") ||
                line.contains("固收") ||
                line.contains("创利") ||
                line.contains("灵活")) {
            log.debug("识别为产品名称行: {}", line);
            return true;
        }

        return false;
    }

    // 从行中提取第一个金额
    private BigDecimal extractFirstAmount(String line) {
        if (line == null || line.isEmpty()) return null;

        log.debug("尝试从行中提取金额: {}", line);

        // 按空格或制表符分割，找第一个看起来像金额的部分
        String[] parts = line.split("\\s+");

        for (String part : parts) {
            // 匹配金额格式：数字+逗号+小数点
            if (part.matches("[\\d,]+(?:\\.\\d{1,2})?")) {
                BigDecimal amount = parseAmount(part);
                if (amount != null) {
                    log.debug("成功提取金额: {} -> {}", part, amount);
                    return amount;
                }
            }
        }

        // 如果按空格分割没找到，尝试用正则直接匹配第一个金额
        Pattern pattern = Pattern.compile("([\\d,]+(?:\\.\\d{1,2})?)");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String amountStr = matcher.group(1);
            BigDecimal amount = parseAmount(amountStr);
            if (amount != null) {
                log.debug("正则匹配到金额: {} -> {}", amountStr, amount);
                return amount;
            }
        }

        return null;
    }

    // 判断是否是无关行（更新版本）
    private boolean isIrrelevantLine(String line) {
        return line.contains("总金额") ||
                line.contains("收起") ||
                line.contains("温馨提示") ||
                line.contains("可赎回开始日") ||
                line.contains("每日可赎") ||
                line.contains("最短持有期") ||
                line.equals("名称") ||
                line.equals("金额") ||
                line.equals("理财") ||
                line.equals("理财产品") ||
                line.equals("我的理财") ||
                line.matches("^\\*+.*\\d+$") || // 匹配账号行
                line.matches("^\\d{4}/\\d{2}/\\d{2}$") || // 纯日期行
                line.matches("^\\d{2}:\\d{2}$"); // 时间行
    }

    // 通用解析方法（兜底）
    private List<AssetRecordDTO> parseGeneric(String[] lines) {
        List<AssetRecordDTO> results = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || isIrrelevantLine(line)) continue;

            // 尝试各种模式匹配
            // 模式1: 名称 金额
            Pattern pattern1 = Pattern.compile("^(.+?)\\s+([\\d,]+\\.?\\d*)$");
            Matcher matcher1 = pattern1.matcher(line);
            if (matcher1.find()) {
                String name = matcher1.group(1).trim();
                BigDecimal amount = parseAmount(matcher1.group(2));
                if (isValidAssetName(name) && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    AssetRecordDTO item = new AssetRecordDTO();
                    item.setAssetName(name);
                    item.setAmount(amount);
                    item.setAcquireTime(LocalDateTime.now());
                    item.setRemark("图片识别导入");
                    results.add(item);
                    log.info("通用-添加记录: {} - {}", name, amount);
                }
            }
        }

        return results;
    }

    // 验证资产名称
    private boolean isValidAssetName(String name) {
        if (name == null || name.length() < 2) return false;
        return name.matches(".*[\\u4e00-\\u9fa5a-zA-Z]+.*");
    }

    @Override
    @Transactional
    public int batchAddRecords(List<AssetRecordDTO> records, String createUser) {
        if (records == null || records.isEmpty()) {
            throw new RuntimeException("记录列表不能为空");
        }

        int successCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (AssetRecordDTO dto : records) {
            try {
                // 验证必填字段
                validateAssetRecord(dto);

                // 处理资产名称（如果是新名称则创建）
                Long assetNameId = processAssetName(dto, createUser);

                // 创建资产记录实体
                AssetRecord record = new AssetRecord();
                record.setAssetNameId(assetNameId);
                record.setAssetTypeId(dto.getAssetTypeId());
                record.setAmount(dto.getAmount());
                record.setUnitId(dto.getUnitId() != null ? dto.getUnitId() : getDefaultUnitId());
                record.setAssetLocationId(dto.getAssetLocationId());
                record.setAcquireTime(dto.getAcquireTime() != null ? dto.getAcquireTime() : now);
                record.setRemark(dto.getRemark() != null ? dto.getRemark() : "批量导入");
                record.setCreateUser(createUser);

                // 插入记录
                assetRecordMapper.insert(record);
                successCount++;

            } catch (Exception e) {
                log.error("添加记录失败: {}", dto, e);
                // 继续处理下一条，不中断整个批量操作
            }
        }

        if (successCount == 0) {
            throw new RuntimeException("批量添加失败，没有成功添加任何记录");
        }

        log.info("批量添加完成，成功: {}/{}", successCount, records.size());
        return successCount;
    }

    /**
     * 验证资产记录必填字段
     */
    private void validateAssetRecord(AssetRecordDTO dto) {
        if (dto.getAssetName() == null || dto.getAssetName().trim().isEmpty()) {
            if (dto.getAssetNameId() == null) {
                throw new RuntimeException("资产名称不能为空");
            }
        }
        if (dto.getAssetTypeId() == null) {
            throw new RuntimeException("资产类型不能为空");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("金额必须大于0");
        }
        if (dto.getAssetLocationId() == null) {
            throw new RuntimeException("资产位置不能为空");
        }
    }

    /**
     * 处理资产名称，如果不存在则创建
     */
    private Long processAssetName(AssetRecordDTO dto, String createUser) {
        // 如果已经有ID，直接返回
        if (dto.getAssetNameId() != null) {
            return dto.getAssetNameId();
        }

        // 如果有名称，查找或创建
        if (dto.getAssetName() != null && !dto.getAssetName().trim().isEmpty()) {
            String name = dto.getAssetName().trim();

            // 查找是否已存在
            QueryWrapper<AssetName> wrapper = new QueryWrapper<>();
            wrapper.eq("name", name)
                    .eq("create_user", createUser)
                    .eq("deleted", 0);
            AssetName existing = assetNameService.getOne(wrapper);

            if (existing != null) {
                return existing.getId();
            }

            // 创建新的资产名称
            AssetName assetName = new AssetName();
            assetName.setName(name);
            assetName.setDescription("");
            assetName.setCreateUser(createUser);
            assetNameService.save(assetName);

            log.info("创建新的资产名称: {}", name);
            return assetName.getId();
        }

        throw new RuntimeException("资产名称不能为空");
    }

    /**
     * 获取默认货币单位ID（人民币）
     */
    private Long getDefaultUnitId() {
        QueryWrapper<CommonMeta> wrapper = new QueryWrapper<>();
        wrapper.eq("type_code", "UNIT")
                .eq("key1", "CNY")
                .eq("deleted", 0);
        CommonMeta unit = commonMetaService.getOne(wrapper);
        if (unit == null) {
            throw new RuntimeException("系统未配置默认货币单位");
        }
        return unit.getId();
    }
}