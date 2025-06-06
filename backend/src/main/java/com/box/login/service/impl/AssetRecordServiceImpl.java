package com.box.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;
import com.box.login.mapper.AssetRecordMapper;
import com.box.login.service.AssetRecordService;
import com.box.login.config.UserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AssetRecordServiceImpl implements AssetRecordService {

    @Autowired
    private AssetRecordMapper assetRecordMapper;

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
    public void copyLastRecords() {
        String currentUser = UserContextHolder.getCurrentUsername();
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        // DEBUG: 打印所有历史记录的日期
        log.info("=== 开始调试：打印所有记录 ===");
        List<AssetRecord> allRecords = assetRecordMapper.selectList(
            new QueryWrapper<AssetRecord>()
                .eq("create_user", currentUser)
                .eq("deleted", 0)
                .orderByDesc("acquire_time")
        );
        for (AssetRecord r : allRecords) {
            log.info("记录: id={}, acquire_time={}, amount={}", r.getId(), r.getAcquireTime(), r.getAmount());
        }
        log.info("=== 今天的日期是: {} ===", today);

        // 查询最近的一天的日期（不包括今天）
        QueryWrapper<AssetRecord> dateWrapper = new QueryWrapper<>();
        dateWrapper.select("DATE(acquire_time) as date_only")
                  .eq("create_user", currentUser)
                  .eq("deleted", 0)
                  .apply("DATE(acquire_time) < DATE(NOW())")
                  .groupBy("DATE(acquire_time)")
                  .orderByDesc("date_only")
                  .last("LIMIT 1");
        
        log.info("查询最近日期的SQL条件: {}", dateWrapper.getSqlSegment());
        List<Map<String, Object>> dateList = assetRecordMapper.selectMaps(dateWrapper);
        
        if (dateList.isEmpty()) {
            log.info("没有找到历史记录");
            return;
        }
        
        String dateStr = dateList.get(0).get("date_only").toString();
        log.info("找到最近记录日期: {}", dateStr);
        log.info("开始复制 {} 的记录", dateStr);
        
        // 查询指定日期的所有记录
        QueryWrapper<AssetRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("create_user", currentUser)
              .eq("deleted", 0)
              .apply("DATE(acquire_time) = STR_TO_DATE({0}, '%Y-%m-%d')", dateStr)
              .orderByAsc("create_time"); // 按创建时间顺序复制
              
        log.info("查询指定日期记录的SQL条件: {}", wrapper.getSqlSegment());

        List<AssetRecord> recordsToCopy = assetRecordMapper.selectList(wrapper);
        
        if (recordsToCopy.isEmpty()) {
            log.info("在日期 {} 没有找到用户 {} 的记录", dateStr, currentUser);
            return;
        }

        log.info("找到 {} 条记录需要复制", recordsToCopy.size());
        
        for (AssetRecord record : recordsToCopy) {
            AssetRecord newRecord = new AssetRecord();
            // 复制基本字段
            newRecord.setAssetNameId(record.getAssetNameId());
            newRecord.setAssetTypeId(record.getAssetTypeId());
            newRecord.setUnitId(record.getUnitId());
            newRecord.setAssetLocationId(record.getAssetLocationId());
            // 设置今天的日期
            newRecord.setAcquireTime(today);
            // 设置创建人
            newRecord.setCreateUser(currentUser);
            // 复制金额
            newRecord.setAmount(record.getAmount());
            
            // 插入新记录
            assetRecordMapper.insert(newRecord);
            log.info("Copied record: type={}, amount={}", record.getAssetTypeId(), record.getAmount());
        }
    }
}
