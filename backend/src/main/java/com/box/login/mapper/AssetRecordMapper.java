package com.box.login.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.box.login.dto.AssetRecordDTO;
import com.box.login.entity.AssetRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 资产记录Mapper接口
 */
@Mapper
public interface AssetRecordMapper extends BaseMapper<AssetRecord> {
    AssetRecordDTO selectDetailById(@Param("id") Long id, @Param("createUser") String createUser);
    
    Page<AssetRecordDTO> selectPageWithDetails(Page<?> page, 
        @Param("assetName") String assetName, 
        @Param("createUser") String createUser);
}
