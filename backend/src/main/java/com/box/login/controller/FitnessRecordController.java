package com.box.login.controller;

import com.box.login.entity.FitnessRecord;
import com.box.login.service.FitnessRecordService;
import com.box.login.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.box.login.service.UserService;
import com.box.login.entity.User;

@RestController
@RequestMapping("/api/fitness-record")
public class FitnessRecordController {

    @Autowired
    private FitnessRecordService fitnessRecordService;

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ApiResponse<List<FitnessRecord>> listRecords(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Long userId = user != null ? user.getId() : null;
        List<FitnessRecord> records = fitnessRecordService.listByConditions(userId, type, remark, startDate, endDate);
        return ApiResponse.success(records);
    }

    @PostMapping("/add")
    public ApiResponse<FitnessRecord> addRecord(@RequestBody FitnessRecord record) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        if (user != null) {
            record.setUserId(user.getId());
        }
        fitnessRecordService.addRecord(record);
        return ApiResponse.success(record);
    }

    @PutMapping("/update")
    public ApiResponse<FitnessRecord> updateRecord(@RequestBody FitnessRecord record) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        if (user != null) {
            record.setUserId(user.getId());
        }
        fitnessRecordService.updateRecord(record);
        return ApiResponse.success(record);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        fitnessRecordService.deleteRecord(id);
        return ApiResponse.success();
    }
}
