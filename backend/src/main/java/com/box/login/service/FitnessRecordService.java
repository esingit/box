package com.box.login.service;

import com.box.login.entity.FitnessRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface FitnessRecordService {
    List<FitnessRecord> listAll();
    void addRecord(FitnessRecord record);
    void updateRecord(FitnessRecord record);
    void deleteRecord(Long id);
    List<FitnessRecord> listByConditions(String type, String remark, String startDate, String endDate);
    IPage<FitnessRecord> pageByConditions(Page<FitnessRecord> page, String type, String remark, String startDate, String endDate);
}
