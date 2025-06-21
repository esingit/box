package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esin.box.config.UserContextHolder;
import com.esin.box.converter.FitnessRecordConverter;
import com.esin.box.dto.FitnessRecordDTO;
import com.esin.box.dto.FitnessStatsDTO;
import com.esin.box.entity.FitnessRecord;
import com.esin.box.mapper.FitnessRecordMapper;
import com.esin.box.service.FitnessRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FitnessRecordServiceImpl implements FitnessRecordService {

    @Autowired
    private FitnessRecordMapper fitnessRecordMapper;

    @Autowired
    private FitnessRecordConverter fitnessRecordConverter;

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
    public IPage<FitnessRecordDTO> pageByConditions(Page<FitnessRecord> page, List<Long> typeIdList, String remark, String startDate, String endDate, String createUser) {
        return fitnessRecordMapper.selectPageWithMeta(page, typeIdList, remark, startDate, endDate, createUser);
    }

    @Override
    @Transactional(readOnly = true)
    public FitnessStatsDTO getStats(String createUser) {
        FitnessStatsDTO stats = new FitnessStatsDTO();
        stats.setMonthlyCount(fitnessRecordMapper.getMonthlyCount(createUser));
        stats.setLastWorkoutDays(fitnessRecordMapper.getLastWorkoutDays(createUser));
        stats.setTotalCount(fitnessRecordMapper.getTotalCount(createUser));
        stats.setWeeklyCount(fitnessRecordMapper.getWeeklyCount(createUser));
        stats.setNextWorkoutDay(fitnessRecordMapper.getNextWorkoutDay(createUser));
        stats.setCarbsIntake(fitnessRecordMapper.getCarbsIntake(createUser));
        stats.setProteinIntake(fitnessRecordMapper.getProteinIntake(createUser));
        return stats;
    }


    @Override
    public List<FitnessRecordDTO> listByConditions(List<Long> typeIdList, String remark, String startDate, String endDate, String username) {
        QueryWrapper<FitnessRecord> wrapper = buildQueryWrapper(typeIdList, remark, startDate, endDate, username);
        List<FitnessRecord> entities = fitnessRecordMapper.selectList(wrapper);
        return fitnessRecordConverter.toDTOList(entities);
    }

    private QueryWrapper<FitnessRecord> buildQueryWrapper(List<Long> typeIdList, String remark, String startDate, String endDate, String username) {
        QueryWrapper<FitnessRecord> wrapper = new QueryWrapper<>();
        if (typeIdList != null && !typeIdList.isEmpty()) {
            wrapper.in("type_id", typeIdList);
        }
        if (remark != null && !remark.isBlank()) {
            wrapper.like("remark", remark);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge("finish_time", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le("finish_time", endDate);
        }
        wrapper.eq("create_user", username);
        wrapper.orderByDesc("finish_time");
        return wrapper;
    }
}
