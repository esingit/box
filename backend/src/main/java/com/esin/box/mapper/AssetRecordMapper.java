package com.esin.box.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.esin.box.dto.AssetRecordDTO;
import com.esin.box.entity.AssetRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资产记录Mapper接口
 */
@Mapper
public interface AssetRecordMapper extends BaseMapper<AssetRecord> {
    IPage<AssetRecordDTO> selectPageWithMeta(IPage<AssetRecord> page,
                                             @Param("assetNameIdList") List<Long> assetNameIdList,
                                             @Param("assetLocationIdList") List<Long> assetLocationIdList,
                                             @Param("assetTypeIdList") List<Long> assetTypeIdList,
                                             @Param("remark") String remark,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("createUser") String createUser);

    /**
     * 批量插入记录
     */
    void insertBatch(@Param("records") List<AssetRecord> records);
}
