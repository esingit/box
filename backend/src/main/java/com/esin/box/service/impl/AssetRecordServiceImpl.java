package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esin.box.config.UserContextHolder;
import com.esin.box.converter.AssetRecordConverter;
import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.dto.AssetStatsDTO;
import com.esin.box.dto.BatchAddResult;
import com.esin.box.entity.AssetRecord;
import com.esin.box.entity.CommonMeta;
import com.esin.box.mapper.AssetRecordMapper;
import com.esin.box.service.AssetNameService;
import com.esin.box.service.AssetRecordService;
import com.esin.box.service.CommonMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        // 调用通用复制方法，保持原有逻辑
        int copiedCount = copyLastRecordsCommon(currentUser, force, true);

        log.info("复制操作完成，用户: {}, 复制记录数: {}", currentUser, copiedCount);
    }

    @Override
    @Transactional
    public BatchAddResult smartBatchAddRecords(List<AssetRecordDTO> records, boolean forceOverwrite, boolean copyLast, String createUser) {
        if (records == null || records.isEmpty()) {
            throw new RuntimeException("记录列表不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean hasTodayRecords = hasTodayRecords(createUser);

        int successCount = 0;
        int updateCount = 0;
        int addCount = 0;
        boolean overwrote = false;
        boolean copied = false;

        try {
            log.info("开始智能批量添加，用户: {}, 记录数: {}, 强制覆盖: {}, 复制历史: {}",
                    createUser, records.size(), forceOverwrite, copyLast);

            // 🔥 场景1：今日无记录，且需要复制上回记录
            if (!hasTodayRecords && copyLast) {
                // 调用通用复制方法，不强制覆盖，不抛异常
                int copiedCount = copyLastRecordsCommon(createUser, false, false);
                if (copiedCount > 0) {
                    copied = true;
                    log.info("已复制 {} 条历史记录到今日", copiedCount);
                } else {
                    log.warn("没有找到可复制的历史记录，继续执行后续操作");
                }
            }

            // 🔥 场景2：今日有记录，且需要强制覆盖
            if ((hasTodayRecords || copied) && forceOverwrite) {
                int deletedCount = deleteTodayRecords(createUser);
                overwrote = true;
                log.info("已清空今日 {} 条记录，准备重新添加", deletedCount);
            }

            // ... 后续处理逻辑保持不变
            Map<Long, AssetRecord> existingRecordsMap = new HashMap<>();

            if (!forceOverwrite || copied) {
                List<AssetRecord> todayRecords = getTodayRecords(createUser);
                log.info("获取到今日现有记录: {} 条", todayRecords.size());

                existingRecordsMap = todayRecords.stream()
                        .collect(Collectors.toMap(
                                AssetRecord::getAssetNameId,
                                record -> record,
                                (existing, replacement) -> existing
                        ));
                log.info("建立资产名称映射: {} 个", existingRecordsMap.size());
            }

            List<AssetRecord> recordsToInsert = new ArrayList<>();
            List<AssetRecord> recordsToUpdate = new ArrayList<>();

            for (AssetRecordDTO dto : records) {
                AssetRecord existingRecord = existingRecordsMap.get(dto.getAssetNameId());

                if (existingRecord != null && !forceOverwrite) {
                    log.info("更新现有记录，资产名称ID: {}, 原金额: {}, 新金额: {}",
                            dto.getAssetNameId(), existingRecord.getAmount(), dto.getAmount());

                    existingRecord.setAmount(dto.getAmount());
                    existingRecord.setRemark(dto.getRemark() != null ? dto.getRemark() : existingRecord.getRemark());
                    existingRecord.setUpdateTime(now);
                    existingRecord.setUpdateUser(createUser);
                    recordsToUpdate.add(existingRecord);
                    updateCount++;
                } else {
                    log.info("新增记录，资产名称ID: {}, 金额: {}", dto.getAssetNameId(), dto.getAmount());
                    AssetRecord newRecord = convertToEntity(dto, createUser, now);
                    recordsToInsert.add(newRecord);
                    addCount++;
                }
            }

            if (!recordsToUpdate.isEmpty()) {
                for (AssetRecord record : recordsToUpdate) {
                    assetRecordMapper.updateById(record);
                }
                log.info("批量更新完成: {} 条记录", recordsToUpdate.size());
            }

            if (!recordsToInsert.isEmpty()) {
                batchInsert(recordsToInsert);
                log.info("批量插入完成: {} 条记录", recordsToInsert.size());
            }

            successCount = updateCount + addCount;
            String message = buildResultMessage(overwrote, copied, updateCount, addCount, records.size());

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

        } catch (Exception e) {
            log.error("批量添加失败，用户: {}", createUser, e);
            throw new RuntimeException("批量添加失败：" + e.getMessage());
        }
    }

    /**
     * 🔥 通用的复制上回记录方法
     *
     * @param username 用户名
     * @param force 是否强制覆盖今日记录
     * @param throwIfNoHistory 如果没有历史记录是否抛出异常
     * @return 复制的记录数量
     */
    private int copyLastRecordsCommon(String username, boolean force, boolean throwIfNoHistory) {
        try {
            log.info("开始复制上回记录，用户: {}, 强制模式: {}, 抛异常模式: {}", username, force, throwIfNoHistory);

            // 1. 检查今日是否已有记录（仅在需要时检查）
            if (force) {
                boolean hasTodayRecords = hasTodayRecords(username);
                if (hasTodayRecords) {
                    // 强制模式下，先删除今日所有记录
                    int deletedCount = deleteTodayRecords(username);
                    log.info("强制模式：已删除今日 {} 条记录", deletedCount);
                }
            } else {
                // 非强制模式，检查今日是否已有记录
                boolean hasTodayRecords = hasTodayRecords(username);
                if (hasTodayRecords && throwIfNoHistory) {
                    throw new RuntimeException("今日已有记录，如需重复复制请使用强制模式");
                }
            }

            // 2. 查找最近的记录日期（不包括今天）
            LocalDate today = LocalDate.now();
            QueryWrapper<AssetRecord> dateWrapper = new QueryWrapper<>();
            dateWrapper.select("DATE(acquire_time) as record_date")
                    .eq("create_user", username)
                    .eq("deleted", 0)
                    .apply("DATE(acquire_time) < CURDATE()") // 排除今天
                    .groupBy("DATE(acquire_time)")
                    .orderByDesc("DATE(acquire_time)")
                    .last("LIMIT 1");

            List<Map<String, Object>> dateResults = assetRecordMapper.selectMaps(dateWrapper);
            if (dateResults.isEmpty()) {
                String message = String.format("用户 %s 没有找到历史记录可复制", username);
                log.warn(message);
                if (throwIfNoHistory) {
                    throw new RuntimeException("没有找到可以复制的历史记录");
                }
                return 0;
            }

            String lastDate = dateResults.get(0).get("record_date").toString();
            log.info("找到用户 {} 最近记录日期: {}", username, lastDate);

            // 3. 获取该日期的所有记录
            QueryWrapper<AssetRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("create_user", username)
                    .eq("deleted", 0)
                    .apply("DATE(acquire_time) = {0}", lastDate)
                    .orderByAsc("create_time");

            List<AssetRecord> recordsToCopy = assetRecordMapper.selectList(recordWrapper);

            if (recordsToCopy.isEmpty()) {
                String message = String.format("在日期 %s 没有找到用户 %s 的记录", lastDate, username);
                log.warn(message);
                if (throwIfNoHistory) {
                    throw new RuntimeException("在最近的记录日期中没有找到可复制的记录");
                }
                return 0;
            }

            log.info("找到 {} 条记录需要复制，从日期: {}", recordsToCopy.size(), lastDate);

            // 4. 复制记录到今天
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

                // 如果有版本字段，设置为0
                if (hasVersionField()) {
                    newRecord.setVersion(0);
                }

                newRecords.add(newRecord);

                log.debug("准备复制记录: 资产名称ID={}, 类型ID={}, 金额={}",
                        record.getAssetNameId(), record.getAssetTypeId(), record.getAmount());
            }

            // 5. 批量插入新记录
            batchInsert(newRecords);

            log.info("成功复制 {} 条历史记录到今日，用户: {}", newRecords.size(), username);
            return newRecords.size();

        } catch (Exception e) {
            log.error("复制历史记录失败，用户: {}, 错误: {}", username, e.getMessage(), e);

            // 根据配置决定是否抛出异常
            if (throwIfNoHistory) {
                throw new RuntimeException("复制历史记录失败：" + e.getMessage());
            } else {
                // 在批量添加场景中，复制失败不应该影响后续的添加操作
                log.warn("复制历史记录失败，但不影响后续操作：{}", e.getMessage());
                return 0;
            }
        }
    }

    /**
     * 检查实体是否有版本字段
     * 这个方法可以根据实际的实体类结构来实现
     */
    private boolean hasVersionField() {
        try {
            AssetRecord.class.getDeclaredField("version");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    // 🔥 移除重复的 copyLastRecordsInternal 方法，因为已经整合到 copyLastRecordsCommon 中

    // 其他方法保持不变...
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

    private String buildResultMessage(boolean overwrote, boolean copied, int updateCount, int addCount, int totalInput) {
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
}