package com.box.login.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.box.login.dto.FitnessRecordDTO;
import com.box.login.entity.FitnessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FitnessRecordMapper extends BaseMapper<FitnessRecord> {
    IPage<FitnessRecordDTO> selectPageWithMeta(IPage<FitnessRecord> page, @Param("typeId") Long typeId,
                                              @Param("remark") String remark, @Param("startDate") String startDate,
                                              @Param("endDate") String endDate);
}
