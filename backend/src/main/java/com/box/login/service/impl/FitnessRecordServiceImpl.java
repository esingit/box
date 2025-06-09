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
    @Transactional(readOnly = true)
    public List<FitnessRecord> listAll() {
        QueryWrapper<FitnessRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("create_user", UserContextHolder.getCurrentUsername());
        return fitnessRecordMapper.selectList(wrapper);
    }

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
    public List<FitnessRecord> listByConditions(Long typeId, String remark, String startDate, String endDate) {
        QueryWrapper<FitnessRecord> wrapper = new QueryWrapper<>();
        // 添加用户过滤
        wrapper.eq("create_user", UserContextHolder.getCurrentUsername());
        
        if (typeId != null) wrapper.eq("type_id", typeId);
        if (remark != null && !remark.isEmpty()) wrapper.like("remark", remark);
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("finish_time", LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("finish_time", LocalDate.parse(endDate).atTime(23,59,59));
        }
        return fitnessRecordMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<FitnessRecordDTO> pageByConditions(Page<FitnessRecord> page, Long typeId, String remark, 
                                                   String startDate, String endDate, String createUser) {
        // 添加日志记录
        org.slf4j.LoggerFactory.getLogger(this.getClass()).debug(
            "Executing pageByConditions with params: typeId={}, remark={}, startDate={}, endDate={}, createUser={}, page={}, pageSize={}", 
            typeId, remark, startDate, endDate, createUser, page.getCurrent(), page.getSize()
        );
        return fitnessRecordMapper.selectPageWithMeta(page, typeId, remark, startDate, endDate, createUser);
    }

    @Override
    @Transactional(readOnly = true)
    public FitnessStatsDTO getStats(String createUser) {
        FitnessStatsDTO stats = new FitnessStatsDTO();
        stats.setMonthlyCount(fitnessStatsMapper.getMonthlyCount(createUser));
        stats.setStreakDays(fitnessStatsMapper.getStreakDays(createUser));
        stats.setTotalCount(fitnessStatsMapper.getTotalCount(createUser));
        return stats;
    }
}
