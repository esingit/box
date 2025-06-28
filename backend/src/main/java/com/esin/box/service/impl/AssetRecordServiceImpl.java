package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esin.box.config.UserContextHolder;
import com.esin.box.converter.AssetRecordConverter;
import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.dto.AssetStatsDTO;
import com.esin.box.dto.BatchAddResult;
import com.esin.box.entity.AssetName;
import com.esin.box.entity.AssetRecord;
import com.esin.box.entity.CommonMeta;
import com.esin.box.mapper.AssetNameMapper;
import com.esin.box.mapper.AssetRecordMapper;
import com.esin.box.service.AssetRecordService;
import com.esin.box.service.CommonMetaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 资产记录服务实现类
 */
@Slf4j
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
    private AssetNameMapper assetNameMapper;

    // 线程局部缓存，避免重复查询
    private final ThreadLocal<Map<Long, String>> assetNameCacheHolder = new ThreadLocal<>();

    @Override
    public void addRecord(AssetRecord record) {
        log.debug("Adding new record: {}", record);

        // 参数验证
        validateRecord(record);

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
    public IPage<AssetRecordDTO> pageByConditions(Page<AssetRecord> page, List<Long> assetNameIdList,
                                                  List<Long> assetLocationIdList, List<Long> assetTypeIdList,
                                                  String remark, String startDate, String endDate, String createUser) {
        return assetRecordMapper.selectPageWithMeta(page, assetNameIdList, assetLocationIdList,
                assetTypeIdList, remark, startDate, endDate, createUser);
    }

    @Override
    public void copyLastRecords(boolean force) {
        String currentUser = UserContextHolder.getCurrentUsername();
        // 调用通用复制方法，保持原有逻辑
        int copiedCount = copyLastRecordsCommon(currentUser, force, true);
        log.info("复制操作完成，用户: {}, 复制记录数: {}", currentUser, copiedCount);
    }

    @Override
    @Transactional
    public BatchAddResult smartBatchAddRecords(List<AssetRecordDTO> records, boolean forceOverwrite,
                                               boolean copyLast, String createUser) {
        if (records == null || records.isEmpty()) {
            throw new RuntimeException("记录列表不能为空");
        }

        try {
            // 预加载资产名称缓存
            Map<Long, String> cache = loadAssetNameCache(records);
            assetNameCacheHolder.set(cache);

            LocalDateTime now = LocalDateTime.now();
            boolean hasTodayRecords = hasTodayRecords(createUser);

            int successCount;
            int updateCount = 0;
            int addCount;
            boolean overwrote = false;
            boolean copied = false;

            log.info("开始智能批量添加，用户: {}, 记录数: {}, 强制覆盖: {}, 复制历史: {}",
                    createUser, records.size(), forceOverwrite, copyLast);

            // 场景1：强制覆盖模式
            BatchAddResult result;
            if (forceOverwrite) {
                result = handleForceOverwriteMode(records, createUser, now, hasTodayRecords);
                overwrote = result.isOverwrote();
            } else {
                // 场景2：非强制覆盖模式
                result = handleNormalMode(records, createUser, now, hasTodayRecords, copyLast);
                copied = result.isCopied();
                updateCount = result.getUpdateCount();
            }
            addCount = result.getAddCount();

            // 计算最终结果
            successCount = updateCount + addCount;
            String message = buildResultMessage(overwrote, copied, updateCount, addCount);

            log.info("智能批量添加完成，用户: {}, 成功: {}, 更新: {}, 新增: {}, 覆盖: {}, 复制: {}",
                    createUser, successCount, updateCount, addCount, overwrote, copied);

            return BatchAddResult.builder()
                    .successCount(successCount)
                    .totalCount(records.size())
                    .overwrote(overwrote)
                    .copied(copied)
                    .updateCount(updateCount)
                    .addCount(addCount)
                    .message(message)
                    .build();

        } catch (RuntimeException e) {
            // 直接抛出业务异常，不再包装
            log.error("批量添加失败，用户: {}, 错误: {}", createUser, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("批量添加失败，用户: {}", createUser, e);
            throw new RuntimeException("批量添加失败：" + e.getMessage());
        } finally {
            // 清理缓存
            assetNameCacheHolder.remove();
        }
    }

    /**
     * 处理强制覆盖模式
     */
    private BatchAddResult handleForceOverwriteMode(List<AssetRecordDTO> records, String createUser,
                                                    LocalDateTime now, boolean hasTodayRecords) {
        boolean overwrote = false;
        int addCount = 0;

        if (hasTodayRecords) {
            int deletedCount = deleteTodayRecords(createUser);
            overwrote = true;
            log.info("强制覆盖模式：已清空今日 {} 条记录", deletedCount);
        }

        // 检查输入数据中的重复
        validateNoDuplicates(records);

        List<AssetRecord> recordsToInsert = new ArrayList<>();
        for (AssetRecordDTO dto : records) {
            AssetRecord newRecord = convertToEntity(dto, createUser, now);
            recordsToInsert.add(newRecord);
            addCount++;
        }

        if (!recordsToInsert.isEmpty()) {
            batchInsert(recordsToInsert);
            log.info("强制覆盖模式：批量插入完成 {} 条记录", recordsToInsert.size());
        }

        return BatchAddResult.builder()
                .overwrote(overwrote)
                .addCount(addCount)
                .build();
    }

    /**
     * 处理非强制覆盖模式
     */
    private BatchAddResult handleNormalMode(List<AssetRecordDTO> records, String createUser,
                                            LocalDateTime now, boolean hasTodayRecords, boolean copyLast) {
        boolean copied = false;
        int updateCount = 0;
        int addCount = 0;

        // 如果今日无记录且需要复制历史记录
        if (!hasTodayRecords && copyLast) {
            int copiedCount = copyLastRecordsCommon(createUser, false, false);
            if (copiedCount > 0) {
                copied = true;
                log.info("已复制 {} 条历史记录到今日", copiedCount);
            } else {
                log.warn("没有找到可复制的历史记录，继续执行后续操作");
            }
        }

        // 获取当前所有今日记录（包括刚复制的）
        List<AssetRecord> todayRecords = getTodayRecords(createUser);
        log.info("获取到今日现有记录: {} 条", todayRecords.size());

        // 构建多级索引
        Map<Long, Map<String, List<AssetRecord>>> existingRecordsIndex = buildMultiLevelIndex(todayRecords);

        // 处理每条输入记录
        List<AssetRecord> recordsToInsert = new ArrayList<>();

        for (AssetRecordDTO dto : records) {
            AssetRecord matchedRecord = findExactMatch(dto, existingRecordsIndex);

            if (matchedRecord != null) {
                // 更新现有记录
                int result = updateExistingRecord(matchedRecord, dto, createUser, now);
                if (result > 0) {
                    updateCount++;
                }
            } else {
                // 新增记录
                log.info("未找到匹配记录，新增：资产名称ID: {}, 金额: {}, 备注: {}",
                        dto.getAssetNameId(), dto.getAmount(), dto.getRemark());
                AssetRecord newRecord = convertToEntity(dto, createUser, now);
                recordsToInsert.add(newRecord);
                addCount++;
            }
        }

        // 执行批量插入新记录
        if (!recordsToInsert.isEmpty()) {
            batchInsert(recordsToInsert);
            log.info("批量插入完成: {} 条记录", recordsToInsert.size());
        }

        return BatchAddResult.builder()
                .copied(copied)
                .updateCount(updateCount)
                .addCount(addCount)
                .build();
    }

    /**
     * 更新现有记录
     */
    private int updateExistingRecord(AssetRecord matchedRecord, AssetRecordDTO dto,
                                     String createUser, LocalDateTime now) {
        log.info("找到匹配记录，ID: {}, 资产名称: {}, 备注: {}, 原金额: {}, 新金额: {}",
                matchedRecord.getId(), getAssetNameById(dto.getAssetNameId()),
                dto.getRemark(), matchedRecord.getAmount(), dto.getAmount());

        UpdateWrapper<AssetRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", matchedRecord.getId())
                .set("amount", dto.getAmount())
                .set("update_time", now)
                .set("update_user", createUser);

        if (dto.getAcquireTime() != null) {
            updateWrapper.set("acquire_time", dto.getAcquireTime());
        }

        if (dto.getRemark() != null) {
            updateWrapper.set("remark", dto.getRemark());
        }

        int updateResult = assetRecordMapper.update(null, updateWrapper);
        if (updateResult > 0) {
            log.info("更新记录成功");
        } else {
            throw new RuntimeException("更新记录失败，记录ID: " + matchedRecord.getId());
        }

        return updateResult;
    }

    /**
     * 构建多级索引 - 使用方括号编号作为索引键
     */
    private Map<Long, Map<String, List<AssetRecord>>> buildMultiLevelIndex(List<AssetRecord> records) {
        Map<Long, Map<String, List<AssetRecord>>> index = new HashMap<>();

        for (AssetRecord record : records) {
            Long assetNameId = record.getAssetNameId();
            String bracketCode = extractBracketCode(record.getRemark());

            index.computeIfAbsent(assetNameId, k -> new HashMap<>())
                    .computeIfAbsent(bracketCode, k -> new ArrayList<>())
                    .add(record);
        }

        return index;
    }

    /**
     * 精确匹配记录 - 基于方括号编号匹配
     */
    private AssetRecord findExactMatch(AssetRecordDTO dto, Map<Long, Map<String, List<AssetRecord>>> index) {
        Map<String, List<AssetRecord>> codeMap = index.get(dto.getAssetNameId());
        if (codeMap == null) {
            return null;
        }

        List<AssetRecord> allMatches = codeMap.values().stream()
                .flatMap(List::stream)
                .toList();

        if (allMatches.size() == 1) {
            return allMatches.get(0);
        }

        // 获取资产名称
        String assetName = getAssetNameById(dto.getAssetNameId());

        // 检查是否有[首次导入]标记
        if (isFirstTimeAdd(dto.getRemark())) {
            log.info("资产[{}]存在{}条记录，但输入记录标记为[首次导入]，将作为新记录添加",
                    assetName, allMatches.size());
            return null;
        }

        // 有多条记录，必须通过编号匹配
        String dtoBracketCode = extractBracketCode(dto.getRemark());

        if (dtoBracketCode.isEmpty()) {
            Set<String> existingCodes = codeMap.keySet().stream()
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.toSet());

            throw new RuntimeException(String.format(
                    """
                            资产[%s]存在%d条记录，必须在备注中添加编号（如[001]）来指定更新哪条记录。
                            现有编号：%s
                            如果要添加新记录，请在备注中加入[首次导入]标记。""",
                    assetName,
                    allMatches.size(),
                    existingCodes.isEmpty() ? "无" : String.join(", ", existingCodes)
            ));
        }

        // 有编号，进行严格匹配
        List<AssetRecord> codeMatches = codeMap.get(dtoBracketCode);

        if (codeMatches == null || codeMatches.isEmpty()) {
            Set<String> existingCodes = codeMap.keySet().stream()
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.toSet());

            throw new RuntimeException(String.format(
                    """
                            资产[%s]存在%d条记录，但没有编号为[%s]的记录。
                            现有编号：%s
                            请检查编号是否正确，或使用[首次导入]标记来添加新记录。""",
                    assetName,
                    allMatches.size(),
                    dtoBracketCode,
                    existingCodes.isEmpty() ? "无" : String.join(", ", existingCodes)
            ));
        }

        if (codeMatches.size() > 1) {
            throw new RuntimeException(String.format(
                    "资产[%s]编号[%s]匹配到%d条记录，无法确定更新哪一条。\n" +
                            "请确保资产名称和编号的组合是唯一的。",
                    assetName,
                    dtoBracketCode,
                    codeMatches.size()
            ));
        }

        return codeMatches.get(0);
    }

    /**
     * 获取资产名称
     */
    private String getAssetNameById(Long assetNameId) {
        if (assetNameId == null) {
            return "未知资产";
        }

        // 先从缓存获取
        Map<Long, String> cache = assetNameCacheHolder.get();
        if (cache != null && cache.containsKey(assetNameId)) {
            return cache.get(assetNameId);
        }

        // 从数据库查询
        AssetName assetName = assetNameMapper.selectById(assetNameId);
        if (assetName != null && StringUtils.isNotEmpty(assetName.getName())) {
            return assetName.getName();
        }

        return "资产ID:" + assetNameId;
    }

    /**
     * 预加载资产名称缓存
     */
    private Map<Long, String> loadAssetNameCache(List<AssetRecordDTO> records) {
        Set<Long> assetNameIds = records.stream()
                .map(AssetRecordDTO::getAssetNameId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (assetNameIds.isEmpty()) {
            return new HashMap<>();
        }

        List<AssetName> assetNames = assetNameMapper.selectBatchIds(assetNameIds);
        return assetNames.stream()
                .collect(Collectors.toMap(
                        AssetName::getId,
                        AssetName::getName,
                        (v1, v2) -> v1
                ));
    }

    /**
     * 验证记录字段
     */
    private void validateRecord(AssetRecord record) {
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
    }

    /**
     * 检查备注中是否包含[首次导入]标记
     */
    private boolean isFirstTimeAdd(String remark) {
        if (StringUtils.isEmpty(remark)) {
            return false;
        }
        return remark.contains("[首次导入]") || remark.contains("【首次导入】");
    }

    /**
     * 验证输入数据中没有重复
     */
    private void validateNoDuplicates(List<AssetRecordDTO> records) {
        Map<String, List<AssetRecordDTO>> normalRecords = new HashMap<>();
        Map<Long, Integer> firstTimeAddCount = new HashMap<>();

        for (AssetRecordDTO dto : records) {
            if (isFirstTimeAdd(dto.getRemark())) {
                firstTimeAddCount.merge(dto.getAssetNameId(), 1, Integer::sum);
            } else {
                String bracketCode = extractBracketCode(dto.getRemark());
                String key = dto.getAssetNameId() + "_" + bracketCode;
                normalRecords.computeIfAbsent(key, k -> new ArrayList<>()).add(dto);
            }
        }

        List<String> duplicates = new ArrayList<>();

        for (Map.Entry<String, List<AssetRecordDTO>> entry : normalRecords.entrySet()) {
            List<AssetRecordDTO> dtoList = entry.getValue();
            if (dtoList.size() > 1) {
                AssetRecordDTO first = dtoList.get(0);
                String bracketCode = extractBracketCode(first.getRemark());
                String assetName = getAssetNameById(first.getAssetNameId());
                duplicates.add(String.format("资产[%s]编号[%s]出现%d次",
                        assetName,
                        bracketCode.isEmpty() ? "空" : bracketCode,
                        dtoList.size()));
            }
        }

        for (Map.Entry<Long, Integer> entry : firstTimeAddCount.entrySet()) {
            if (entry.getValue() > 1) {
                String assetName = getAssetNameById(entry.getKey());
                log.info("资产[{}]有{}条[首次导入]记录，都将作为新记录添加",
                        assetName, entry.getValue());
            }
        }

        if (!duplicates.isEmpty()) {
            throw new RuntimeException("输入数据中存在重复的编号记录：" + String.join("; ", duplicates));
        }
    }

    /**
     * 从备注中提取方括号内的编号（排除[首次导入]）
     */
    private String extractBracketCode(String remark) {
        if (StringUtils.isEmpty(remark)) {
            return "";
        }

        // 先移除[首次导入]标记
        String cleanedRemark = remark
                .replace("[首次导入]", "")
                .replace("【首次导入】", "");

        // 正则表达式匹配方括号内的内容
        Pattern pattern = Pattern.compile("\\[([^]]+)]|【([^】]+)】");
        Matcher matcher = pattern.matcher(cleanedRemark);

        if (matcher.find()) {
            String code = matcher.group(1);
            if (code == null) {
                code = matcher.group(2);
            }
            return code.trim();
        }

        return "";
    }

    /**
     * 通用的复制上回记录方法
     */
    private int copyLastRecordsCommon(String username, boolean force, boolean throwIfNoHistory) {
        try {
            log.info("开始复制上回记录，用户: {}, 强制模式: {}, 抛异常模式: {}",
                    username, force, throwIfNoHistory);

            // 检查今日是否已有记录
            boolean hasTodayRecords = hasTodayRecords(username);
            if (force) {
                if (hasTodayRecords) {
                    int deletedCount = deleteTodayRecords(username);
                    log.info("强制模式：已删除今日 {} 条记录", deletedCount);
                }
            } else {
                if (hasTodayRecords && throwIfNoHistory) {
                    throw new RuntimeException("今日已有记录，如需重复复制请使用强制模式");
                }
            }

            // 查找最近的记录日期
            String lastDate = findLastRecordDate(username);
            if (lastDate == null) {
                String message = String.format("用户 %s 没有找到历史记录可复制", username);
                log.warn(message);
                if (throwIfNoHistory) {
                    throw new RuntimeException("没有找到可以复制的历史记录");
                }
                return 0;
            }

            // 获取该日期的所有记录
            List<AssetRecord> recordsToCopy = getRecordsByDate(username, lastDate);
            if (recordsToCopy.isEmpty()) {
                if (throwIfNoHistory) {
                    throw new RuntimeException("在最近的记录日期中没有找到可复制的记录");
                }
                return 0;
            }

            log.info("找到 {} 条记录需要复制，从日期: {}", recordsToCopy.size(), lastDate);

            // 复制记录到今天
            List<AssetRecord> newRecords = createCopiedRecords(recordsToCopy, username, lastDate);

            // 批量插入新记录
            batchInsert(newRecords);

            log.info("成功复制 {} 条历史记录到今日，用户: {}", newRecords.size(), username);
            return newRecords.size();

        } catch (Exception e) {
            log.error("复制历史记录失败，用户: {}, 错误: {}", username, e.getMessage(), e);
            if (throwIfNoHistory) {
                throw new RuntimeException("复制历史记录失败：" + e.getMessage());
            } else {
                log.warn("复制历史记录失败，但不影响后续操作：{}", e.getMessage());
                return 0;
            }
        }
    }

    /**
     * 查找最近的记录日期
     */
    private String findLastRecordDate(String username) {
        QueryWrapper<AssetRecord> dateWrapper = new QueryWrapper<>();
        dateWrapper.select("DATE(acquire_time) as record_date")
                .eq("create_user", username)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) < CURDATE()")
                .groupBy("DATE(acquire_time)")
                .orderByDesc("DATE(acquire_time)")
                .last("LIMIT 1");

        List<Map<String, Object>> dateResults = assetRecordMapper.selectMaps(dateWrapper);
        if (dateResults.isEmpty()) {
            return null;
        }

        return dateResults.get(0).get("record_date").toString();
    }

    /**
     * 获取指定日期的记录
     */
    private List<AssetRecord> getRecordsByDate(String username, String date) {
        QueryWrapper<AssetRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("create_user", username)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) = {0}", date)
                .orderByAsc("create_time");

        return assetRecordMapper.selectList(recordWrapper);
    }

    /**
     * 创建复制的记录
     */
    private List<AssetRecord> createCopiedRecords(List<AssetRecord> recordsToCopy,
                                                  String username, String lastDate) {
        LocalDateTime now = LocalDateTime.now();
        List<AssetRecord> newRecords = new ArrayList<>();

        for (AssetRecord record : recordsToCopy) {
            AssetRecord newRecord = new AssetRecord();

            // 复制所有必要字段
            newRecord.setAssetNameId(record.getAssetNameId());
            newRecord.setAssetTypeId(record.getAssetTypeId());
            newRecord.setUnitId(record.getUnitId());
            newRecord.setAssetLocationId(record.getAssetLocationId());
            newRecord.setAmount(record.getAmount());
            newRecord.setRemark(record.getRemark() != null ? record.getRemark() : "复制自" + lastDate);

            // 设置时间和用户信息
            newRecord.setAcquireTime(now);
            newRecord.setCreateTime(now);
            newRecord.setUpdateTime(now);
            newRecord.setCreateUser(username);
            newRecord.setUpdateUser(username);
            newRecord.setDeleted(0);
            newRecord.setVersion(0);

            newRecords.add(newRecord);

            log.debug("准备复制记录: 资产名称ID={}, 类型ID={}, 金额={}",
                    record.getAssetNameId(), record.getAssetTypeId(), record.getAmount());
        }

        return newRecords;
    }

    private List<AssetRecord> getTodayRecords(String username) {
        QueryWrapper<AssetRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("create_user", username)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) = CURDATE()")
                .orderByAsc("create_time");

        List<AssetRecord> records = assetRecordMapper.selectList(wrapper);
        log.info("获取用户 {} 今日记录: {} 条", username, records.size());
        return records;
    }

    private int deleteTodayRecords(String username) {
        UpdateWrapper<AssetRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("create_user", username)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) = CURDATE()")
                .set("deleted", 1)
                .set("update_time", LocalDateTime.now())
                .set("update_user", username);

        int deletedCount = assetRecordMapper.update(null, updateWrapper);
        log.info("软删除用户 {} 的今日记录: {} 条", username, deletedCount);
        return deletedCount;
    }

    @Override
    public boolean hasTodayRecords(String username) {
        QueryWrapper<AssetRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("create_user", username)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) = CURDATE()");

        Long count = assetRecordMapper.selectCount(wrapper);
        boolean hasRecords = count > 0;
        log.info("用户 {} 今日记录检查: {} 条", username, count);
        return hasRecords;
    }

    private AssetRecord convertToEntity(AssetRecordDTO dto, String createUser, LocalDateTime now) {
        AssetRecord record = new AssetRecord();
        record.setAssetNameId(dto.getAssetNameId());
        record.setAssetTypeId(dto.getAssetTypeId());
        record.setAmount(dto.getAmount());
        record.setUnitId(dto.getUnitId());
        record.setAssetLocationId(dto.getAssetLocationId());
        record.setAcquireTime(dto.getAcquireTime() != null ? dto.getAcquireTime() : now);
        record.setRemark(dto.getRemark() != null ? dto.getRemark() : "批量导入");
        record.setCreateUser(createUser);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setUpdateUser(createUser);
        record.setDeleted(0);
        return record;
    }

    private void batchInsert(List<AssetRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        log.info("开始批量插入 {} 条记录", records.size());
        int batchSize = 100;
        for (int i = 0; i < records.size(); i += batchSize) {
            int end = Math.min(i + batchSize, records.size());
            List<AssetRecord> batch = records.subList(i, end);

            for (AssetRecord record : batch) {
                assetRecordMapper.insert(record);
            }
            log.debug("批次插入完成: {}-{}", i + 1, end);
        }
        log.info("批量插入完成，总计: {} 条记录", records.size());
    }

    private String buildResultMessage(boolean overwrote, boolean copied, int updateCount,
                                      int addCount) {
        StringBuilder message = new StringBuilder("批量操作完成：");

        if (copied) {
            message.append("已复制历史记录，");
        }

        if (overwrote) {
            message.append("已覆盖今日记录，");
        }

        if (updateCount > 0) {
            message.append(String.format("更新%d条", updateCount));
        }

        if (addCount > 0) {
            if (updateCount > 0) {
                message.append("，");
            }
            message.append(String.format("新增%d条", addCount));
        }

        return message.toString();
    }

    /**
     * 查找统计日期
     */
    private String findStatisticsDate(String createUser, Integer offset) {
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

        Map<String, Object> dateResult = assetRecordMapper.selectMaps(dateWrapper).stream()
                .findFirst().orElse(null);
        return dateResult != null ? dateResult.get("date_only").toString() : null;
    }

    /**
     * 查找前一个日期
     */
    private String findPreviousDate(String createUser, String currentDate) {
        QueryWrapper<AssetRecord> previousWrapper = new QueryWrapper<>();
        previousWrapper.select("DATE(acquire_time) as date_only")
                .eq("create_user", createUser)
                .eq("deleted", 0)
                .lt("DATE(acquire_time)", currentDate)
                .orderByDesc("acquire_time")
                .last("LIMIT 1");

        return assetRecordMapper.selectMaps(previousWrapper).stream()
                .findFirst()
                .map(m -> m.get("date_only").toString())
                .orElse(null);
    }

    /**
     * 计算资产统计
     */
    private AssetStats calculateStats(List<AssetRecord> records) {
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal investmentAssets = BigDecimal.ZERO;  // 新增：理财资产
        Map<Long, BigDecimal> typeAmounts = new HashMap<>();

        log.info("🔍 开始计算资产统计，记录数: {}", records.size());

        // 按类型累加金额
        for (AssetRecord record : records) {
            typeAmounts.merge(record.getAssetTypeId(), record.getAmount(), BigDecimal::add);
        }

        log.info("🔍 按类型汇总完成，类型数: {}", typeAmounts.size());

        // 按类型统计资产和负债
        for (Map.Entry<Long, BigDecimal> entry : typeAmounts.entrySet()) {
            CommonMeta type = commonMetaService.getById(entry.getKey());

            if (type != null && "ASSET_TYPE".equals(type.getTypeCode())) {
                String key1 = type.getKey1();
                BigDecimal amount = entry.getValue();

                log.debug("🔍 处理类型 ID: {}, key1: '{}', value1: '{}', 金额: {}",
                        entry.getKey(), key1, type.getValue1(), amount);

                if ("DEBT".equals(key1)) {
                    // 负债类型
                    totalLiabilities = totalLiabilities.add(amount);
                    log.debug("  ✅ 归类为负债");
                } else {
                    // 非负债类型都计入总资产
                    totalAssets = totalAssets.add(amount);
                    log.debug("  ✅ 归类为资产");

                    // 检查是否为理财资产
                    if ("FUND".equals(key1) || "FINANCE".equals(key1) || "STOCK".equals(key1)) {
                        investmentAssets = investmentAssets.add(amount);
                        log.debug("  💰 同时归类为理财资产");
                    }
                }
            } else {
                log.warn("🔍 未找到类型 ID: {} 或类型不是 ASSET_TYPE，默认归为资产", entry.getKey());
                totalAssets = totalAssets.add(entry.getValue());
            }
        }

        log.info("🔍 统计计算完成 - 总资产: {}, 总负债: {}, 理财资产: {}",
                totalAssets, totalLiabilities, investmentAssets);

        return new AssetStats(totalAssets, totalLiabilities, investmentAssets);
    }

    /**
     * 资产统计内部类 - 新增理财资产字段
     *
     * @param investmentAssets 新增
     */
        private record AssetStats(BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal investmentAssets) {
    }

    @Override
    @Transactional(readOnly = true)
    public AssetStatsDTO getLatestStats(String createUser, Integer offset) {
        log.debug("获取用户 {} 的资产统计, 偏移天数: {}", createUser, offset);

        // 找到指定偏移日期的记录日期
        String latestDate = findStatisticsDate(createUser, offset);
        if (latestDate == null) {
            log.info("未找到任何记录，返回零值统计");
            return AssetStatsDTO.builder()
                    .netAssets(0.0)
                    .totalLiabilities(0.0)
                    .investmentAssets(0.0)
                    .latestDate(LocalDateTime.now().toString())
                    .build();
        }

        // 获取最新日期的资产记录
        List<AssetRecord> latestRecords = getRecordsByDate(createUser, latestDate);

        // 获取上一个日期的资产记录
        String previousDate = findPreviousDate(createUser, latestDate);
        List<AssetRecord> previousRecords = previousDate != null ?
                getRecordsByDate(createUser, previousDate) : new ArrayList<>();

        // 计算统计数据
        AssetStats latestStats = calculateStats(latestRecords);
        AssetStats previousStats = calculateStats(previousRecords);

        // 🔥 修复：净资产 = totalAssets（因为 totalAssets 已经排除了负债）
        BigDecimal latestNetAssets = latestStats.totalAssets;
        BigDecimal previousNetAssets = previousStats.totalAssets;

        // 计算变化额
        BigDecimal netAssetsChange = latestNetAssets.subtract(previousNetAssets);
        BigDecimal liabilitiesChange = latestStats.totalLiabilities.subtract(previousStats.totalLiabilities);
        BigDecimal investmentAssetsChange = latestStats.investmentAssets.subtract(previousStats.investmentAssets);

        // 格式化日期显示
        String formattedDate = formatDate(latestDate);

        log.info("计算完成 - 净资产: {}, 总负债: {}, 理财资产: {}, 净资产变化: {}, 负债变化: {}, 理财资产变化: {}",
                latestNetAssets, latestStats.totalLiabilities, latestStats.investmentAssets,
                netAssetsChange, liabilitiesChange, investmentAssetsChange);

        return AssetStatsDTO.builder()
                .netAssets(latestNetAssets.doubleValue())
                .totalLiabilities(latestStats.totalLiabilities.doubleValue())
                .investmentAssets(latestStats.investmentAssets.doubleValue())
                .latestDate(latestDate)
                .formattedDate(formattedDate)
                .netAssetsChange(netAssetsChange.doubleValue())
                .liabilitiesChange(liabilitiesChange.doubleValue())
                .investmentAssetsChange(investmentAssetsChange.doubleValue())
                .build();
    }

    /**
     * 格式化日期
     */
    private String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("MM月dd日"));
        } catch (Exception e) {
            return dateStr;
        }
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
}