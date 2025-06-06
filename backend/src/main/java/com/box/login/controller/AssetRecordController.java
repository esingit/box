package com.box.login.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.config.UserContextHolder;
import com.box.login.dto.ApiResponse;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;
import com.box.login.service.AssetRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @Parameter(description = "资产类型ID") @RequestParam(required = false) Long typeId,
            @Parameter(description = "备注关键词") @RequestParam(required = false) String remark,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "7") Integer pageSize) {
        try {
            log.debug("Received list request - typeId: {}, remark: {}, startDate: {}, endDate: {}, page: {}, pageSize: {}", 
                     typeId, remark, startDate, endDate, page, pageSize);
            Page<AssetRecord> pageObj = new Page<>(page, pageSize);
            String currentUser = UserContextHolder.getCurrentUsername();
            log.debug("Querying records for user: {}", currentUser);
            IPage<AssetRecordDTO> records = assetRecordService.pageByConditions(pageObj, typeId, remark, startDate, endDate, currentUser);
            log.debug("Query completed. Total records: {}, Current page: {}", records.getTotal(), records.getCurrent());
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
    public ApiResponse<Void> copyLastRecords() {
        try {
            assetRecordService.copyLastRecords();
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to copy last records", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
