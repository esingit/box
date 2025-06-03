package com.box.login.service;

import com.box.login.entity.FitnessRecord;
import java.util.List;

public interface FitnessRecordService {
    List<FitnessRecord> listAll();
    void addRecord(FitnessRecord record);
    void updateRecord(FitnessRecord record);
    void deleteRecord(Long id);
    List<FitnessRecord> listByConditions(Long userId, String type, String remark, String startDate, String endDate);
}
