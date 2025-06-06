package com.box.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.config.UserContextHolder;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.dto.AssetStatsDTO;
import com.box.login.entity.AssetRecord;
import com.box.login.entity.CommonMeta;
import com.box.login.mapper.AssetRecordMapper;
import com.box.login.service.AssetRecordService;
import com.box.login.service.CommonMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AssetRecordServiceImpl implements AssetRecordService {

    @Autowired
    private AssetRecordMapper assetRecordMapper;

    @Autowired
    private CommonMetaService commonMetaService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssetRecordServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    public List<AssetRecord> listAll() {
        QueryWrapper<AssetRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("create_user", UserContextHolder.getCurrentUsername());
        return assetRecordMapper.selectList(wrapper);
    }

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
    public IPage<AssetRecordDTO> pageByConditions(Page<AssetRecord> page, Long typeId, String remark,
                                                  String startDate, String endDate, String createUser) {
        log.debug("Executing pageByConditions - typeId: {}, startDate: {}, endDate: {}, createUser: {}",
                typeId, startDate, endDate, createUser);
        return assetRecordMapper.selectPageWithMeta(page, typeId, remark, startDate, endDate, createUser);
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

        // 2. 查询该日期的资产和负债总额
        QueryWrapper<AssetRecord> statsWrapper = new QueryWrapper<>();
        statsWrapper.eq("create_user", createUser)
                .eq("deleted", 0)
                .apply("DATE(acquire_time) = STR_TO_DATE({0}, '%Y-%m-%d')", latestDate);

        List<AssetRecord> records = assetRecordMapper.selectList(statsWrapper);

        // 3. 计算资产和负债
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (AssetRecord record : records) {
            // 根据资产类型的 value1 字段判断是否为负债
            try {
                CommonMeta type = commonMetaService.getById(record.getAssetTypeId());
                BigDecimal amount = record.getAmount();

                log.info("处理记录 - ID: {}, 类型ID: {}, 金额: {}, 类型名称: {}",
                        record.getId(), record.getAssetTypeId(), amount,
                        type != null ? type.getValue1() : "未知");

                String typeValue = type != null ? type.getValue1() : "未知";
                // 严格匹配"负债"类型
                if (type != null && "负债".equals(type.getValue1())) {
                    totalLiabilities = totalLiabilities.add(amount);
                    log.info("计入负债 - 类型: {}, 金额: {}, 累计负债: {}",
                            typeValue, amount, totalLiabilities);
                } else {
                    totalAssets = totalAssets.add(amount);
                    log.info("计入资产 - 类型: {}, 金额: {}, 累计资产: {}",
                            typeValue, amount, totalAssets);
                }
            } catch (Exception e) {
                log.error("处理资产记录时发生错误 - 记录ID: " + record.getId(), e);
                // 如果无法确定类型，保守起见计入资产
                totalAssets = totalAssets.add(record.getAmount());
            }
        }

        log.info("计算完成 - 总资产: {}, 总负债: {}", totalAssets, totalLiabilities);

        // 格式化日期显示
        String formattedDate;
        try {
            LocalDate date = LocalDate.parse(latestDate);
            formattedDate = date.format(DateTimeFormatter.ofPattern("MM月dd日"));
        } catch (Exception e) {
            formattedDate = latestDate;
        }

        return AssetStatsDTO.builder()
                .totalAssets(totalAssets.doubleValue())
                .totalLiabilities(totalLiabilities.doubleValue())
                .latestDate(latestDate)
                .formattedDate(formattedDate)
                .build();
    }

    // 判断资产类型是否为负债类型
    private boolean isLiabilityType(Long assetTypeId) {
        CommonMeta meta = commonMetaService.getById(assetTypeId);
        if (meta != null && meta.getValue2() != null) {
            return meta.getValue2().contains("负债");
        }
        return false;
    }
}
