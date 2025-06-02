package com.box.login.service.impl;

import com.box.login.entity.FitnessRecord;
import com.box.login.mapper.FitnessRecordMapper;
import com.box.login.service.FitnessRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
