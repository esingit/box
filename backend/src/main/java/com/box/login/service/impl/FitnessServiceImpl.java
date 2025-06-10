package com.box.login.service.impl;

import com.box.login.mapper.FitnessRecordMapper;
import com.box.login.service.FitnessService;
import com.box.login.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FitnessServiceImpl implements FitnessService {

    @Autowired
    private FitnessRecordMapper fitnessRecordMapper;

    @Override
    public List<Map<String, Object>> getStatistics() {
        String userId = SecurityUtils.getCurrentUserId();
        return fitnessRecordMapper.getStatistics(userId);
    }
}
