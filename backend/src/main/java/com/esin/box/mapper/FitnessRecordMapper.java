package com.esin.box.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.esin.box.dto.FitnessRecordDTO;
import com.esin.box.entity.FitnessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FitnessRecordMapper extends BaseMapper<FitnessRecord> {
    IPage<FitnessRecordDTO> selectPageWithMeta(IPage<FitnessRecord> page, @Param("typeIdList") List<Long> typeIdList,
                                               @Param("remark") String remark, @Param("startDate") String startDate,
                                               @Param("endDate") String endDate, @Param("createUser") String createUser);

    Integer getMonthlyCount(@Param("createUser") String createUser);

    Integer getTotalCount(@Param("createUser") String createUser);

    Integer getWeeklyCount(@Param("createUser") String createUser);

    Integer getCarbsIntake(@Param("createUser") String createUser);

    Integer getProteinIntake(@Param("createUser") String createUser);

    Integer getLastWorkoutDays(@Param("createUser") String createUser);

    String getNextWorkoutDay(@Param("createUser") String createUser);

    Integer getStreakDays(@Param("createUser") String createUser);
}
