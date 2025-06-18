package com.box.login.controller;

import com.box.login.dto.FitnessRecordDTO;
import com.box.login.dto.FitnessStatsDTO;
import com.box.login.entity.FitnessRecord;
import com.box.login.service.FitnessRecordService;
import com.box.login.dto.ApiResponse;
import com.box.login.config.UserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

@RestController
@RequestMapping("/api/fitness-record")
public class FitnessRecordController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FitnessRecordController.class);

    @Autowired
    private FitnessRecordService fitnessRecordService;

    @GetMapping("/list")
    public ApiResponse<IPage<FitnessRecordDTO>> listRecords(
            @RequestParam(required = false) List<Long> typeIdList,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "7") Integer pageSize) {
        Page<FitnessRecord> pageObj = new Page<>(page, pageSize);
        String currentUser = UserContextHolder.getCurrentUsername();
        IPage<FitnessRecordDTO> records = fitnessRecordService.pageByConditions(pageObj, typeIdList, remark, startDate, endDate, currentUser);
        return ApiResponse.success(records);
    }

    @PostMapping("/add")
    public ApiResponse<FitnessRecord> addRecord(@RequestBody FitnessRecord record) {
        fitnessRecordService.addRecord(record);
        return ApiResponse.success(record);
    }

    @PutMapping("/update")
    public ApiResponse<FitnessRecord> updateRecord(@RequestBody FitnessRecord record) {
        fitnessRecordService.updateRecord(record);
        return ApiResponse.success(record);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        fitnessRecordService.deleteRecord(id);
        return ApiResponse.success();
    }

    @GetMapping("/stats")
    public ApiResponse<FitnessStatsDTO> getStats() {
        String currentUser = UserContextHolder.getCurrentUsername();
        FitnessStatsDTO stats = fitnessRecordService.getStats(currentUser);
        return ApiResponse.success(stats);
    }
}
