package com.box.login.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.config.UserContextHolder;
import com.box.login.dto.ApiResponse;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.dto.AssetStatsDTO;
import com.box.login.entity.AssetRecord;
import com.box.login.service.AssetRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产记录控制器
 */
@Tag(name = "资产记录管理", description = "资产记录相关接口")
@RestController
@RequestMapping("/api/asset-record")
@RequiredArgsConstructor
public class AssetRecordController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssetRecordController.class);
    
    private final AssetRecordService assetRecordService;

    @Operation(summary = "分页查询资产记录")
    @GetMapping("/list")
    public ApiResponse<IPage<AssetRecordDTO>> listRecords(
            @Parameter(description = "资产名称ID") @RequestParam(required = false) List<Long> assetNameIdList,
            @Parameter(description = "资产位置ID") @RequestParam(required = false) List<Long> assetLocationIdList,
            @Parameter(description = "资产类型ID") @RequestParam(required = false) List<Long> assetTypeIdList,
            @Parameter(description = "备注关键词") @RequestParam(required = false) String remark,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "7") Integer pageSize) {
        try {
            Page<AssetRecord> pageObj = new Page<>(page, pageSize);
            String currentUser = UserContextHolder.getCurrentUsername();
            IPage<AssetRecordDTO> records = assetRecordService.pageByConditions(pageObj, assetNameIdList, assetLocationIdList, assetTypeIdList, remark, startDate, endDate, currentUser);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to list records:", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "创建资产记录")
    @PostMapping("/add")
    public ApiResponse<AssetRecord> addRecord(@Validated @RequestBody AssetRecord record) {
        log.debug("Received add record request with data: {}", record);
        try {
            assetRecordService.addRecord(record);
            return ApiResponse.success(record);
        } catch (Exception e) {
            log.error("Failed to add record", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "更新资产记录")
    @PutMapping("/update")
    public ApiResponse<AssetRecord> updateRecord(@Validated @RequestBody AssetRecord record) {
        try {
            assetRecordService.updateRecord(record);
            return ApiResponse.success(record);
        } catch (Exception e) {
            log.error("Failed to update record", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "删除资产记录")
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        try {
            assetRecordService.deleteRecord(id);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to delete record", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "复制上次资产记录")
    @PostMapping("/copy-last")
    public ApiResponse<Void> copyLastRecords(@RequestParam(required = false) Boolean force) {
        try {
            assetRecordService.copyLastRecords(force != null && force);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to copy last records", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "获取最近日期的资产统计")
    @GetMapping("/latest-stats")
    public ApiResponse<AssetStatsDTO> getLatestStats(
            @Parameter(description = "日期偏移量，0表示最新，1表示昨天，以此类推") 
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        try {
            String currentUser = UserContextHolder.getCurrentUsername();
            AssetStatsDTO stats = assetRecordService.getLatestStats(currentUser, offset);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("Failed to get latest asset stats", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
