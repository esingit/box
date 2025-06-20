package com.box.login.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.config.UserContextHolder;
import com.box.login.entity.AssetName;
import com.box.login.service.AssetNameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-name")
@Tag(name = "资产名称管理", description = "资产名称的增删改查接口")
public class AssetNameController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssetNameController.class);
    
    @Autowired
    private AssetNameService assetNameService;

    @GetMapping("/all")
    @Operation(
        summary = "获取所有资产名称",
        description = "获取当前用户的所有未删除的资产名称",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功")
        }
    )
    public Result<List<AssetName>> listAll() {
        try {
            LambdaQueryWrapper<AssetName> wrapper = Wrappers.lambdaQuery(AssetName.class)
                .eq(AssetName::getCreateUser, UserContextHolder.getCurrentUsername())
                .eq(AssetName::getDeleted, 0)
                .orderByDesc(AssetName::getCreateTime)
                .select(AssetName::getId, AssetName::getName, AssetName::getDescription); // 只选择需要的字段
            return Result.success(assetNameService.list(wrapper));
        } catch (Exception e) {
            log.error("获取资产名称列表失败:", e);
            return Result.error("获取资产名称列表失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(
        summary = "创建资产名称",
        description = "创建一个新的资产名称",
        responses = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
        }
    )
    public Result<AssetName> add(@Validated @RequestBody AssetName assetName) {
        try {
            return Result.success(assetNameService.addAssetName(assetName));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新资产名称",
        description = "根据ID更新资产名称信息",
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败")
        }
    )
    public Result<AssetName> update(
            @Parameter(description = "资产名称ID") @PathVariable Long id,
            @Validated @RequestBody AssetName assetName) {
        try {
            assetName.setId(id);
            return Result.success(assetNameService.updateAssetName(assetName));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除资产名称",
        description = "根据ID删除资产名称",
        responses = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "删除失败")
        }
    )
    public Result<Void> delete(
            @Parameter(description = "资产名称ID") @PathVariable Long id) {
        assetNameService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取资产名称详情",
        description = "根据ID获取资产名称详情",
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "资产名称不存在")
        }
    )
    public Result<AssetName> getById(
            @Parameter(description = "资产名称ID") @PathVariable Long id) {
        AssetName assetName = assetNameService.getById(id);
        return assetName != null ? Result.success(assetName) : Result.error("资产名称不存在");
    }

    @GetMapping("/list")
    @Operation(
        summary = "高级查询资产名称",
        description = "支持分页、按名称和描述模糊搜索、时间范围筛选等",
        responses = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "查询失败")
        }
    )
    public Result<Page<AssetName>> listRecords(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "资产名称") @RequestParam(required = false) String name,
            @Parameter(description = "资产描述") @RequestParam(required = false) String description,
            @Parameter(description = "开始时间 (yyyy-MM-dd HH:mm:ss)") 
                @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间 (yyyy-MM-dd HH:mm:ss)") 
                @RequestParam(required = false) String endTime) {
        try {
            Page<AssetName> result = assetNameService.listRecords(
                current, size, name, description, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/exists")
    @Operation(
        summary = "检查资产名称是否存在",
        description = "检查指定的资产名称是否已经存在，可以在更新时排除当前记录",
        responses = {
            @ApiResponse(responseCode = "200", description = "检查完成")
        }
    )
    public Result<Boolean> checkNameExists(
            @Parameter(description = "资产名称") @RequestParam String name,
            @Parameter(description = "排除的ID（可选）") @RequestParam(required = false) Long excludeId) {
        try {
            AssetName param = new AssetName();
            param.setName(name);
            param.setId(excludeId);
            assetNameService.updateAssetName(param);
            return Result.success(false);
        } catch (RuntimeException e) {
            return Result.success(true);
        }
    }
}
