package com.box.login.service;

import com.box.login.entity.FitnessRecord;
import com.box.login.dto.FitnessRecordDTO;
import com.box.login.dto.FitnessStatsDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface FitnessRecordService {
    void addRecord(FitnessRecord record);
    void updateRecord(FitnessRecord record);
    void deleteRecord(Long id);
    IPage<FitnessRecordDTO> pageByConditions(Page<FitnessRecord> page, List<Long> typeIdList, String remark, String startDate, String endDate, String createUser);
    FitnessStatsDTO getStats(String createUser);
}
