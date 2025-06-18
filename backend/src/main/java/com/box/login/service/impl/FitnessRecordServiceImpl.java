package com.box.login.service.impl;

import com.box.login.dto.FitnessRecordDTO;
import com.box.login.dto.FitnessStatsDTO;
import com.box.login.entity.FitnessRecord;
import com.box.login.mapper.FitnessRecordMapper;
import com.box.login.mapper.FitnessStatsMapper;
import com.box.login.service.FitnessRecordService;
import com.box.login.config.UserContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FitnessRecordServiceImpl implements FitnessRecordService {

    @Autowired
    private FitnessRecordMapper fitnessRecordMapper;
    
    @Autowired
    private FitnessStatsMapper fitnessStatsMapper;

    @Override
    public void addRecord(FitnessRecord record) {
        // 设置创建人
        record.setCreateUser(UserContextHolder.getCurrentUsername());
        fitnessRecordMapper.insert(record);
    }

    @Override
    public void updateRecord(FitnessRecord record) {
        // 验证是否是记录创建人
        FitnessRecord existing = fitnessRecordMapper.selectById(record.getId());
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限修改此记录");
        }
        fitnessRecordMapper.updateById(record);
    }

    @Override
    public void deleteRecord(Long id) {
        // 验证是否是记录创建人
        FitnessRecord existing = fitnessRecordMapper.selectById(id);
        String currentUser = UserContextHolder.getCurrentUsername();
        if (existing != null && !currentUser.equals(existing.getCreateUser())) {
            throw new RuntimeException("您没有权限删除此记录");
        }
        fitnessRecordMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<FitnessRecordDTO> pageByConditions(Page<FitnessRecord> page, List<Long> typeIdList, String remark,
                                                   String startDate, String endDate, String createUser) {
        return fitnessRecordMapper.selectPageWithMeta(page, typeIdList, remark, startDate, endDate, createUser);
    }

    @Override
    @Transactional(readOnly = true)
    public FitnessStatsDTO getStats(String createUser) {
        FitnessStatsDTO stats = new FitnessStatsDTO();
        stats.setMonthlyCount(fitnessStatsMapper.getMonthlyCount(createUser));
        stats.setLastWorkoutDays(fitnessStatsMapper.getLastWorkoutDays(createUser));
        stats.setTotalCount(fitnessStatsMapper.getTotalCount(createUser));
        stats.setWeeklyCount(fitnessStatsMapper.getWeeklyCount(createUser));
        stats.setNextWorkoutDay(fitnessStatsMapper.getNextWorkoutDay(createUser));
        stats.setCarbsIntake(fitnessStatsMapper.getCarbsIntake(createUser));
        stats.setProteinIntake(fitnessStatsMapper.getProteinIntake(createUser));
        return stats;
    }
}
