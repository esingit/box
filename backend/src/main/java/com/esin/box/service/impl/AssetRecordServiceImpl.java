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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

            // 使用Tesseract进行OCR识别
            // ITesseract tesseract = new Tesseract();
            // tesseract.setDatapath("/usr/local/share/tessdata");
            // tesseract.setLanguage("chi_sim+eng");

            // 直接使用注入的 tesseract
            String text = tesseract.doOCR(tempFile);
            log.info("OCR识别结果: {}", text);

            // 解析识别的文本
            results = parseOCRText(text);

        } catch (Exception e) {
            log.error("图片识别失败", e);
            throw new RuntimeException("图片识别失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return results;
    }

    /**
     * 解析OCR识别的文本，提取资产名称和金额
     */
    private List<AssetRecordDTO> parseOCRText(String text) {
        List<AssetRecordDTO> results = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return results;
        }

        // 按行分割文本
        String[] lines = text.split("\\n");

        // 定义匹配模式
        // 模式1: 名称 金额 (如：工资收入 5000)
        Pattern pattern1 = Pattern.compile("([\\u4e00-\\u9fa5a-zA-Z\\s]+)\\s+([\\d,]+\\.?\\d*)");
        // 模式2: 名称：金额 (如：工资收入：5000)
        Pattern pattern2 = Pattern.compile("([\\u4e00-\\u9fa5a-zA-Z\\s]+)[：:]\\s*([\\d,]+\\.?\\d*)");
        // 模式3: 金额 名称 (如：5000 工资收入)
        Pattern pattern3 = Pattern.compile("([\\d,]+\\.?\\d*)\\s+([\\u4e00-\\u9fa5a-zA-Z\\s]+)");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            AssetRecordDTO item = null;

            // 尝试匹配模式2（优先级最高，因为有明确的分隔符）
            Matcher matcher2 = pattern2.matcher(line);
            if (matcher2.find()) {
                item = new AssetRecordDTO();
                item.setAssetName(matcher2.group(1).trim());
                item.setAmount(parseAmount(matcher2.group(2)));
            }

            // 尝试匹配模式1
            if (item == null) {
                Matcher matcher1 = pattern1.matcher(line);
                if (matcher1.find()) {
                    String name = matcher1.group(1).trim();
                    // 排除纯数字被识别为名称的情况
                    if (!name.matches("\\d+")) {
                        item = new AssetRecordDTO();
                        item.setAssetName(name);
                        item.setAmount(parseAmount(matcher1.group(2)));
                    }
                }
            }

            // 尝试匹配模式3
            if (item == null) {
                Matcher matcher3 = pattern3.matcher(line);
                if (matcher3.find()) {
                    String name = matcher3.group(2).trim();
                    if (!name.isEmpty()) {
                        item = new AssetRecordDTO();
                        item.setAssetName(name);
                        item.setAmount(parseAmount(matcher3.group(1)));
                    }
                }
            }

            // 如果成功解析，添加到结果列表
            if (item != null && item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                // 设置默认值
                item.setAcquireTime(LocalDateTime.now());
                item.setRemark("批量导入");
                results.add(item);
                log.debug("解析成功: {} - {}", item.getAssetName(), item.getAmount());
            }
        }

        return results;
    }

    /**
     * 解析金额字符串
     */
    private BigDecimal parseAmount(String amountStr) {
        try {
            // 移除逗号和空格
            amountStr = amountStr.replaceAll("[,\\s]", "");
            return new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            log.warn("无法解析金额: {}", amountStr);
            return null;
        }
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
