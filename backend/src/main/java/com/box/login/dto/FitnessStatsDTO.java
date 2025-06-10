package com.box.login.dto;

import lombok.Data;

@Data
public class FitnessStatsDTO {
    private Integer monthlyCount;    // 本月运动次数
    private Integer lastWorkoutDays; // 上次运动天数
    private Integer totalCount;      // 累计运动次数
    private Integer weeklyCount;     // 本周运动天数
    private String nextWorkoutDay;   // 下次运动日期
    private Integer carbsIntake;     // 今日碳水摄入量
    private Integer proteinIntake;   // 今日蛋白质摄入量
}
