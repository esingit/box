package com.esin.box.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.esin.box.entity.AssetName;

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
     * @param pageObj 分页数据
     * @param name 资产名称（模糊查询）
     * @param startTime 创建开始时间
     * @param endTime 创建结束时间
     * @return 分页结果
     */
    IPage<AssetName> listRecords(Page<AssetName> pageObj, String name, String description,
                                 String startTime, String endTime);
}
