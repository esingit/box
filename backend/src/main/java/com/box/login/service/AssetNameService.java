package com.box.login.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.box.login.entity.AssetName;

public interface AssetNameService extends IService<AssetName> {
    /**
     * 创建资产名称，校验名称唯一性
     *
     * @param assetName 资产名称对象
     * @return 创建后的资产名称对象
     * @throws RuntimeException 如果名称已存在
     */
    AssetName addAssetName(AssetName assetName);

    /**
     * 更新资产名称，校验名称唯一性
     *
     * @param assetName 资产名称对象
     * @return 更新后的资产名称对象
     * @throws RuntimeException 如果名称已存在
     */
    AssetName updateAssetName(AssetName assetName);

    /**
     * 高级分页查询
     *
     * @param current 当前页
     * @param size 每页大小
     * @param name 资产名称（模糊查询）
     * @param startTime 创建开始时间
     * @param endTime 创建结束时间
     * @return 分页结果
     */
    Page<AssetName> listRecords(Integer current, Integer size, String name, String description,
                                 String startTime, String endTime);
}
