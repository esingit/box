package com.box.login.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.box.login.dto.AssetDetailDTO;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资产记录Mapper接口
 */
@Mapper
public interface AssetRecordMapper extends BaseMapper<AssetRecord> {
    IPage<AssetRecordDTO> selectPageWithMeta(IPage<AssetRecord> page,
                                             @Param("assetNameId") Long assetNameId,
                                             @Param("locationId") Long locationId,
                                             @Param("typeId") Long typeId,
                                             @Param("remark") String remark,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("createUser") String createUser);

    List<AssetDetailDTO> getDetailedStatistics(@Param("userId") String userId, @Param("assetTypeId") Long assetTypeId, @Param("assetNameId") Long assetNameId);
}
