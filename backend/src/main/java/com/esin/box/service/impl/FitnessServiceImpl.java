package com.esin.box.service.impl;

import com.esin.box.mapper.FitnessRecordMapper;
import com.esin.box.service.FitnessService;
import com.esin.box.utils.SecurityUtils;
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
