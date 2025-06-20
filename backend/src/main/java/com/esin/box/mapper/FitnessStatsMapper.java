package com.esin.box.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.esin.box.entity.FitnessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FitnessStatsMapper extends BaseMapper<FitnessRecord> {

    Integer getMonthlyCount(@Param("createUser") String createUser);

    Integer getTotalCount(@Param("createUser") String createUser);

    Integer getWeeklyCount(@Param("createUser") String createUser);

    Integer getCarbsIntake(@Param("createUser") String createUser);

    Integer getProteinIntake(@Param("createUser") String createUser);

    Integer getLastWorkoutDays(@Param("createUser") String createUser);

    String getNextWorkoutDay(@Param("createUser") String createUser);

    Integer getStreakDays(@Param("createUser") String createUser);
}
