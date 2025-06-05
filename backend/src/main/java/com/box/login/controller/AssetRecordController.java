package com.box.login.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;
import com.box.login.service.AssetRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 资产记录控制器
 */
@Tag(name = "资产记录管理", description = "资产记录相关接口")
@RestController
@RequestMapping("/api/asset-records")
@RequiredArgsConstructor
public class AssetRecordController {

    private final AssetRecordService assetRecordService;

    @Operation(summary = "创建资产记录")
    @PostMapping
    public Result<AssetRecord> create(@Validated @RequestBody AssetRecord assetRecord) {
        assetRecordService.save(assetRecord);
        return Result.success(assetRecord);
    }

    @Operation(summary = "更新资产记录")
    @PutMapping("/{id}")
    public Result<AssetRecord> update(@PathVariable Long id, @Validated @RequestBody AssetRecord assetRecord) {
        assetRecord.setId(id);
        assetRecordService.updateById(assetRecord);
        return Result.success(assetRecord);
    }

    @Operation(summary = "删除资产记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        assetRecordService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "获取资产记录详情")
    @GetMapping("/{id}")
    public Result<AssetRecordDTO> getDetail(@PathVariable Long id) {
        return Result.success(assetRecordService.getDetailById(id));
    }

    @Operation(summary = "分页查询资产记录")
    @GetMapping
    public Result<Page<AssetRecordDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String assetName) {
        
        Page<AssetRecord> page = new Page<>(current, size);
        return Result.success(assetRecordService.pageWithDetails(page, assetName));
    }
}
