package com.box.login.service.impl;

import com.box.login.entity.FitnessRecord;
import com.box.login.mapper.FitnessRecordMapper;
import com.box.login.service.FitnessRecordService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FitnessRecordServiceImpl implements FitnessRecordService {

    @Autowired
    private FitnessRecordMapper fitnessRecordMapper;

    @Override
    public List<FitnessRecord> listAll() {
        return fitnessRecordMapper.selectList(null);
    }

    @Override
    public void addRecord(FitnessRecord record) {
        fitnessRecordMapper.insert(record);
    }

    @Override
    public void updateRecord(FitnessRecord record) {
        fitnessRecordMapper.updateById(record);
    }

    @Override
    public void deleteRecord(Long id) {
        fitnessRecordMapper.deleteById(id);
    }

    @Override
    public List<FitnessRecord> listByConditions(String type, String remark, String startDate, String endDate) {
        QueryWrapper<FitnessRecord> wrapper = new QueryWrapper<>();
        if (type != null && !type.isEmpty()) wrapper.eq("type", type);
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
    public IPage<FitnessRecord> pageByConditions(Page<FitnessRecord> page, String type, String remark, String startDate, String endDate) {
        QueryWrapper<FitnessRecord> wrapper = new QueryWrapper<>();
        if (type != null && !type.isEmpty()) wrapper.eq("type", type);
        if (remark != null && !remark.isEmpty()) wrapper.like("remark", remark);
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("finish_time", java.time.LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("finish_time", java.time.LocalDate.parse(endDate).atTime(23,59,59));
        }
        return fitnessRecordMapper.selectPage(page, wrapper);
    }
}
