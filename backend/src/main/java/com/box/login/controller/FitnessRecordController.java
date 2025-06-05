package com.box.login.controller;

import com.box.login.dto.FitnessRecordDTO;
import com.box.login.entity.FitnessRecord;
import com.box.login.service.FitnessRecordService;
import com.box.login.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@RestController
@RequestMapping("/api/fitness-record")
public class FitnessRecordController {

    @Autowired
    private FitnessRecordService fitnessRecordService;

    @GetMapping("/list")
    public ApiResponse<IPage<FitnessRecordDTO>> listRecords(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<FitnessRecord> pageObj = new Page<>(page, pageSize);
        IPage<FitnessRecordDTO> records = fitnessRecordService.pageByConditions(pageObj, typeId, remark, startDate, endDate);
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
}
