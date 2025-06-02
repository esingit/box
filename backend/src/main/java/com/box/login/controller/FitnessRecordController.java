package com.box.login.controller;

import com.box.login.entity.FitnessRecord;
import com.box.login.service.FitnessRecordService;
import com.box.login.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fitness-record")
public class FitnessRecordController {

    @Autowired
    private FitnessRecordService fitnessRecordService;

    @GetMapping("/list")
    public ApiResponse<List<FitnessRecord>> listRecords() {
        List<FitnessRecord> records = fitnessRecordService.listAll();
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
